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
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.io.Closeables;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.KeeperException.NoNodeException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Stat;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;

/**
 * Backup command.
 */
public class Backup implements Watcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(Backup.class);
    private static final JsonFactory JSON_FACTORY = new JsonFactory();
    public static final String FIELD_AVERSION = "aversion";
    public static final String FIELD_CTIME = "ctime";
    public static final String FIELD_CVERSION = "cversion";
    public static final String FIELD_CZXID = "czxid";
    public static final String FIELD_EPHEMERAL_OWNER = "ephemeralOwner";
    public static final String FIELD_MTIME = "mtime";
    public static final String FIELD_MZXID = "mzxid";
    public static final String FIELD_PZXID = "pzxid";
    public static final String FIELD_VERSION = "version";
    public static final String FIELD_DATA = "data";
    public static final String FIELD_ACLS = "acls";
    public static final String FIELD_ACL_ID = "id";
    public static final String FIELD_ACL_SCHEME = "scheme";
    public static final String FIELD_ACL_PERMS = "perms";
    private final ZooKeeperFactory zooKeeperFactory;
    private final BackupOptions options;
    private final List<Pattern> excludePatterns;

    public Backup(ZooKeeperFactory zooKeeperFactory, BackupOptions options) {
        Preconditions.checkNotNull(zooKeeperFactory);
        Preconditions.checkNotNull(options);
        this.zooKeeperFactory = zooKeeperFactory;
        this.options = options;
        excludePatterns = Lists.newArrayListWithExpectedSize(options.excludePaths.size());
        for (String path : options.excludePaths) {
            excludePatterns.add(Pattern.compile(path));
        }
    }

    public void backup(OutputStream os) throws InterruptedException, IOException, KeeperException {
        JsonGenerator jgen = null;
        ZooKeeper zk = null;
        try {
            zk = zooKeeperFactory.createZooKeeper(options, this);
            jgen = JSON_FACTORY.createGenerator(os);
            if (options.prettyPrint) {
                jgen.setPrettyPrinter(new DefaultPrettyPrinter());
            }
            jgen.writeStartObject();
            doBackup(zk, jgen, "/");
            jgen.writeEndObject();
        } finally {
            if (jgen != null) {
                jgen.close();
            }
            if (zk != null) {
                zk.close();
            }
        }
    }

    private boolean isExcluded(String path) {
        boolean ignored = false;
        for (Pattern pattern : this.excludePatterns) {
            if (pattern.matcher(path).find()) {
                LOGGER.info("Excluding path: {} matching pattern: {}", path, pattern.pattern());
                ignored = true;
                break;
            }
        }
        return ignored;
    }

    private static String createFullPath(String path, String childPath) {
        final String fullChildPath;
        if (path.endsWith("/")) {
            fullChildPath = path + childPath;
        }
        else {
            fullChildPath = path + '/' + childPath;
        }
        return fullChildPath;
    }

    private static <T> List<T> nullToEmpty(List<T> original) {
        return (original != null) ? original : Collections.<T> emptyList();
    }

    private void doBackup(ZooKeeper zk, JsonGenerator jgen, String path)
            throws KeeperException, InterruptedException, IOException {
        try {
            final Stat stat = new Stat();
            List<ACL> acls = nullToEmpty(zk.getACL(path, stat));
            if (stat.getEphemeralOwner() != 0 && !options.backupEphemeral) {
                LOGGER.debug("Skipping ephemeral node: {}", path);
                return;
            }
            byte[] data = null;
            if (stat.getDataLength() > 0) {
                Stat dataStat = new Stat();
                data = zk.getData(path, false, dataStat);
                for (int i = 0; stat.compareTo(dataStat) != 0 && i < options.numRetries; i++) {
                    LOGGER.warn("Retrying getACL / getData to read consistent state");
                    acls = zk.getACL(path, stat);
                    data = zk.getData(path, false, dataStat);
                }
                if (stat.compareTo(dataStat) != 0) {
                    throw new IllegalStateException("Unable to read consistent data for znode: " + path);
                }
            }
            LOGGER.debug("Backing up node: {}", path);
            dumpNode(jgen, path, stat, acls, data);
            final List<String> childPaths = nullToEmpty(zk.getChildren(path, false, null));
            Collections.sort(childPaths);
            for (String childPath : childPaths) {
                final String fullChildPath = createFullPath(path, childPath);
                if (!isExcluded(fullChildPath)) {
                    doBackup(zk, jgen, fullChildPath);
                }
            }
        } catch (NoNodeException e) {
            LOGGER.warn("Node disappeared during backup: {}", path);
        }
    }

    private void dumpNode(JsonGenerator jgen, String path, Stat stat, List<ACL> acls, byte[] data) throws IOException {
        jgen.writeObjectFieldStart(path);

        // The number of changes to the ACL of this znode.
        jgen.writeNumberField(FIELD_AVERSION, stat.getAversion());

        // The time in milliseconds from epoch when this znode was created.
        jgen.writeNumberField(FIELD_CTIME, stat.getCtime());

        // The number of changes to the children of this znode.
        jgen.writeNumberField(FIELD_CVERSION, stat.getCversion());

        // The zxid of the change that caused this znode to be created.
        jgen.writeNumberField(FIELD_CZXID, stat.getCzxid());

        // The length of the data field of this znode.
        // jgen.writeNumberField("dataLength", stat.getDataLength());

        // The session id of the owner of this znode if the znode is an ephemeral node. If it is not an ephemeral node,
        // it will be zero.
        jgen.writeNumberField(FIELD_EPHEMERAL_OWNER, stat.getEphemeralOwner());

        // The time in milliseconds from epoch when this znode was last modified.
        jgen.writeNumberField(FIELD_MTIME, stat.getMtime());

        // The zxid of the change that last modified this znode.
        jgen.writeNumberField(FIELD_MZXID, stat.getMzxid());

        // The number of children of this znode.
        // jgen.writeNumberField("numChildren", stat.getNumChildren());

        // last modified children?
        jgen.writeNumberField(FIELD_PZXID, stat.getPzxid());

        // The number of changes to the data of this znode.
        jgen.writeNumberField(FIELD_VERSION, stat.getVersion());

        if (data != null) {
            jgen.writeBinaryField(FIELD_DATA, data);
        }
        else {
            jgen.writeNullField(FIELD_DATA);
        }

        jgen.writeArrayFieldStart(FIELD_ACLS);
        for (ACL acl : acls) {
            jgen.writeStartObject();
            jgen.writeStringField(FIELD_ACL_ID, acl.getId().getId());
            jgen.writeStringField(FIELD_ACL_SCHEME, acl.getId().getScheme());
            jgen.writeNumberField(FIELD_ACL_PERMS, acl.getPerms());
            jgen.writeEndObject();
        }
        jgen.writeEndArray();

        jgen.writeEndObject();
    }

    @Override
    public void process(WatchedEvent event) {
        LOGGER.debug("Received watch event: {}", event);
    }

    public static void main(String[] args) throws IOException, InterruptedException, KeeperException {
        BackupOptions options = new BackupOptions();
        CmdLineParser parser = new CmdLineParser(options);
        try {
            parser.parseArgument(args);
        } catch (CmdLineException e) {
            System.err.println(e.getLocalizedMessage());
            System.err.println(Backup.class.getName() + " [options...] arguments...");
            parser.printUsage(System.err);
            System.exit(1);
        }
        if (options.verbose) {
            LoggingUtils.enableDebugLogging(Backup.class.getPackage().getName());
        }
        Backup backup = new Backup(new DefaultZooKeeperFactory(), options);
        OutputStream os;
        if ("-".equals(options.outputFile)) {
            os = System.out;
        }
        else {
            os = new BufferedOutputStream(new FileOutputStream(options.outputFile));
        }
        try {
            if (options.compress) {
                os = new GZIPOutputStream(os);
            }
            backup.backup(os);
        } finally {
            os.flush();
            Closeables.close(os, true);
        }
    }
}
