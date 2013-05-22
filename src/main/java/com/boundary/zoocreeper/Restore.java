/**
 * Copyright 2013 Boundary, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.boundary.zoocreeper;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.io.Closeables;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.KeeperException.NodeExistsException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Id;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;
import java.util.zip.GZIPInputStream;

/**
 * Command-line utility used to restore a ZK backup.
 */
public class Restore implements Watcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(Restore.class);
    private static final JsonFactory JSON_FACTORY = new JsonFactory();

    private final ZooKeeperFactory zooKeeperFactory;
    private final RestoreOptions options;

    public Restore(ZooKeeperFactory zooKeeperFactory, RestoreOptions options) {
        this.zooKeeperFactory = zooKeeperFactory;
        this.options = options;
    }

    /**
     * Restores ZooKeeper state from the specified backup stream.
     *
     * @param inputStream Input stream containing a JSON encoded ZooKeeper backup.
     * @throws InterruptedException If this method is interrupted.
     * @throws IOException If an error occurs reading from the backup stream.
     */
    public void restore(InputStream inputStream) throws InterruptedException, IOException, KeeperException {
        ZooKeeper zk = null;
        JsonParser jp = null;
        try {
            jp = JSON_FACTORY.createParser(inputStream);
            zk = zooKeeperFactory.createZooKeeper(options, this);
            doRestore(jp, zk);
        } finally {
            if (zk != null) {
                zk.close();
            }
            if (jp != null) {
                jp.close();
            }
        }
    }

    private static void expectNextToken(JsonParser jp, JsonToken expected) throws IOException {
        if (jp.nextToken() != expected) {
            throw new IOException(String.format("Expected: %s, Found: %s", expected, jp.getCurrentToken()));
        }
    }

    private static void expectCurrentToken(JsonParser jp, JsonToken expected) throws IOException {
        final JsonToken currentToken = jp.getCurrentToken();
        if (currentToken != expected) {
            throw new IOException(String.format("Expected: %s, Found: %s", expected, currentToken));
        }
    }

    private static String getParentPath(String path) {
        final int lastSlash = path.lastIndexOf('/');
        return (lastSlash > 0) ? path.substring(0, lastSlash) : "/";
    }

    private static void createPath(ZooKeeper zk, String path) throws KeeperException, InterruptedException {
        if ("/".equals(path)) {
            return;
        }
        if (zk.exists(path, false) == null) {
            createPath(zk, getParentPath(path));
            LOGGER.info("Creating path: {}", path);
            try {
                zk.create(path, null, Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            } catch (NodeExistsException e) {
                // Race condition
            }
        }
    }

    private void doRestore(JsonParser jp, ZooKeeper zk) throws IOException, KeeperException, InterruptedException {
        expectNextToken(jp, JsonToken.START_OBJECT);
        while (jp.nextToken() != JsonToken.END_OBJECT) {
            final BackupZNode zNode = readZNode(jp, jp.getCurrentName());
            if (zNode.ephemeralOwner != 0) {
                LOGGER.info("Skipping ephemeral ZNode: {}", zNode.path);
                continue;
            }
            if (!zNode.path.startsWith(options.rootPath)) {
                LOGGER.info("Skipping ZNode (not under root path '{}'): {}", options.rootPath, zNode.path);
                continue;
            }
            createPath(zk, getParentPath(zNode.path));
            try {
                zk.create(zNode.path, zNode.data, zNode.acls, CreateMode.PERSISTENT);
                LOGGER.info("Created node: {}", zNode.path);
            } catch (NodeExistsException e) {
                if (options.overwriteExisting) {
                    // TODO: Compare with current data / acls
                    zk.setACL(zNode.path, zNode.acls, -1);
                    zk.setData(zNode.path, zNode.data, -1);
                }
                else {
                    LOGGER.warn("Node already exists: {}", zNode.path);
                }
            }
        }
    }

    private static class BackupZNode {
        private final String path;
        private final long ephemeralOwner;
        private final byte[] data;
        private final List<ACL> acls;

        public BackupZNode(String path, long ephemeralOwner, byte[] data, List<ACL> acls) {
            this.path = path;
            this.ephemeralOwner = ephemeralOwner;
            this.data = data;
            this.acls = acls;
        }
    }

    private static final ImmutableList<String> REQUIRED_ZNODE_FIELDS = ImmutableList.of(Backup.FIELD_EPHEMERAL_OWNER,
            Backup.FIELD_DATA, Backup.FIELD_ACLS);

    private static BackupZNode readZNode(JsonParser jp, String path) throws IOException {
        expectNextToken(jp, JsonToken.START_OBJECT);
        long ephemeralOwner = 0;
        byte[] data = null;
        final List<ACL> acls = Lists.newArrayList();
        final Set<String> seenFields = Sets.newHashSet();
        while (jp.nextToken() != JsonToken.END_OBJECT) {
            jp.nextValue();
            final String fieldName = jp.getCurrentName();
            seenFields.add(fieldName);
            if (Backup.FIELD_EPHEMERAL_OWNER.equals(fieldName)) {
                ephemeralOwner = jp.getLongValue();
            }
            else if (Backup.FIELD_DATA.equals(fieldName)) {
                if (jp.getCurrentToken() == JsonToken.VALUE_NULL) {
                    data = null;
                }
                else {
                    data = jp.getBinaryValue();
                }
            }
            else if (Backup.FIELD_ACLS.equals(fieldName)) {
                readACLs(jp, acls);
            }
            else {
                LOGGER.debug("Ignored field: {}", fieldName);
            }
        }
        if (!seenFields.containsAll(REQUIRED_ZNODE_FIELDS)) {
            throw new IOException("Missing required fields: " + REQUIRED_ZNODE_FIELDS);
        }
        return new BackupZNode(path, ephemeralOwner, data, acls);
    }

    private static void readACLs(JsonParser jp, List<ACL> acls) throws IOException {
        expectCurrentToken(jp, JsonToken.START_ARRAY);
        while (jp.nextToken() != JsonToken.END_ARRAY) {
            acls.add(readACL(jp));
        }
    }

    private static final ImmutableList<String> REQUIRED_ACL_FIELDS = ImmutableList.of(Backup.FIELD_ACL_SCHEME,
            Backup.FIELD_ACL_ID, Backup.FIELD_ACL_PERMS);

    private static ACL readACL(JsonParser jp) throws IOException {
        expectCurrentToken(jp, JsonToken.START_OBJECT);
        String scheme = null;
        String id = null;
        int perms = -1;
        Set<String> seenFields = Sets.newHashSet();
        while (jp.nextToken() != JsonToken.END_OBJECT) {
            jp.nextValue();
            final String fieldName = jp.getCurrentName();
            seenFields.add(fieldName);
            if (Backup.FIELD_ACL_SCHEME.equals(fieldName)) {
                scheme = jp.getValueAsString();
            }
            else if (Backup.FIELD_ACL_ID.equals(fieldName)) {
                id = jp.getValueAsString();
            }
            else if (Backup.FIELD_ACL_PERMS.equals(fieldName)) {
                perms = jp.getIntValue();
            }
            else {
                throw new IOException("Unexpected field: " + fieldName);
            }
        }
        if (!seenFields.containsAll(REQUIRED_ACL_FIELDS)) {
            throw new IOException("Missing required ACL fields: " + REQUIRED_ACL_FIELDS);
        }
        final Id zkId;
        if (Ids.ANYONE_ID_UNSAFE.getScheme().equals(scheme) && Ids.ANYONE_ID_UNSAFE.getId().equals(id)) {
            zkId = Ids.ANYONE_ID_UNSAFE;
        }
        else {
            zkId = new Id(scheme, id);
        }
        return new ACL(perms, zkId);
    }

    @Override
    public void process(WatchedEvent event) {
        LOGGER.debug("Received watch event: {}", event);
    }

    public static void main(String[] args) throws IOException, InterruptedException, KeeperException {
        RestoreOptions options = new RestoreOptions();
        CmdLineParser parser = new CmdLineParser(options);
        try {
            parser.parseArgument(args);
        } catch (CmdLineException e) {
            System.err.println(e.getLocalizedMessage());
            System.err.println(Restore.class.getName() + " [options...] arguments...");
            parser.printUsage(System.err);
            System.exit(1);
        }
        if (options.verbose) {
            LoggingUtils.enableDebugLogging(Restore.class.getPackage().getName());
        }
        InputStream is = null;
        try {
            if ("-".equals(options.inputFile)) {
                is = new BufferedInputStream(System.in);
            }
            else {
                is = new BufferedInputStream(new FileInputStream(options.inputFile));
            }
            if (options.compress) {
                is = new GZIPInputStream(is);
            }
            Restore restore = new Restore(new DefaultZooKeeperFactory(), options);
            restore.restore(is);
        } finally {
            Closeables.close(is, true);
        }
    }
}
