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

import com.google.common.io.Closeables;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

/**
 * Command-line utility used to restore a ZK backup.
 */
public class Restore implements Watcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(Restore.class);

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
    public void restore(InputStream inputStream) throws InterruptedException, IOException {
        ZooKeeper zk = null;
        try {
            zk = zooKeeperFactory.createZooKeeper(options, this);
            throw new UnsupportedOperationException("Not yet implemented");
        } finally {
            if (zk != null) {
                zk.close();
            }
        }
    }

    @Override
    public void process(WatchedEvent event) {
        LOGGER.debug("Received watch event: {}", event);
    }

    public static void main(String[] args) throws IOException, InterruptedException {
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
