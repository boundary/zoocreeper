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

import com.google.common.primitives.Ints;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Common options to both the {@link Backup} and {@link Restore} commands.
 */
public class CommonOptions {

    @Option(name = "-z", aliases = { "--zk-connect" }, usage = "ZooKeeper connection string (e.g. localhost:2181)",
            required = true, metaVar = "<zookeeper_connect>")
    String zkConnect;

    @Option(name = "--connect-timeout", usage = "ZooKeeper connect timeout (in milliseconds)", required = false,
            metaVar = "<timeout_in_ms>")
    long zkConnectTimeoutMs = TimeUnit.SECONDS.toMillis(10);

    @Option(name = "--session-timeout", usage = "ZooKeeper session timeout (in milliseconds)", required = false,
            metaVar = "<timeout_in_ms>")
    long zkSessionTimeoutMs = TimeUnit.SECONDS.toMillis(30);

    @Option(name = "--compress", usage = "Compress output / Decompress input (using zlib)", required = false)
    boolean compress = false;

    @Option(name = "--exclude", usage = "Regular expression of paths to exclude", required = false,
            metaVar = "<exclude_regex>", handler = RegexOptionHandler.class)
    List<Pattern> excludePatterns = new ArrayList<Pattern>();

    @Option(name = "--include", usage = "Regular expression of paths to include", required = false,
            metaVar = "<include_regex>", handler = RegexOptionHandler.class)
    List<Pattern> includePatterns = new ArrayList<Pattern>();

    @Option(name = "-v", aliases = { "--verbose" }, usage = "Verbose logging output", required = false)
    boolean verbose;

    @Option(name = "--root-path", usage = "ZooKeeper root path for backup/restore (default: '/')",
            required = false, metaVar = "<root_path>", handler = ZooKeeperPathOptionHandler.class)
    String rootPath = "/";

    @Option(name = "-h", aliases = { "--help" }, usage = "Show usage information")
    boolean help;

    public boolean isPathExcluded(Logger logger, String path) {
        boolean ignored = false;
        for (Pattern excludePattern : this.excludePatterns) {
            if (excludePattern.matcher(path).find()) {
                logger.info("Excluding path: {} matching pattern: {}", path, excludePattern.pattern());
                ignored = true;
                break;
            }
        }
        return ignored;
    }

    public boolean isPathIncluded(Logger logger, String path) {
        if (this.includePatterns.isEmpty()) {
            return true;
        }
        boolean included = false;
        for (Pattern includePattern : this.includePatterns) {
            if (includePattern.matcher(path).find()) {
                logger.info("Including path: {} matching pattern: {}", path, includePattern.pattern());
                included = true;
                break;
            }
        }
        return included;
    }

    /**
     * Creates a connection to ZooKeeper (waiting for the connection to be made).
     * @param logger Logger used for informational messages.
     * @return A ZooKeeper client (in SyncConnected state).
     * @throws IOException If the connection couldn't be made.
     * @throws InterruptedException If interrupted while waiting for connection to be made.
     */
    public ZooKeeper createZooKeeper(Logger logger) throws IOException, InterruptedException {
        final CountDownLatch connected = new CountDownLatch(1);
        logger.info("Connecting to ZooKeeper: {}", zkConnect);
        final ZooKeeper zk = new ZooKeeper(zkConnect, Ints.checkedCast(zkSessionTimeoutMs), new Watcher() {
            @Override
            public void process(WatchedEvent event) {
                if (event.getState() == Event.KeeperState.SyncConnected) {
                    connected.countDown();
                }
            }
        });
        try {
            if (!connected.await(zkConnectTimeoutMs, TimeUnit.MILLISECONDS)) {
                throw new IOException("Timeout out connecting to: " + zkConnect);
            }
            logger.info("Connected");
            return zk;
        } catch (InterruptedException e) {
            try {
                zk.close();
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }
            throw e;
        }
    }

}
