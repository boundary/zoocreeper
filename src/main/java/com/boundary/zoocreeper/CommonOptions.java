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

import org.kohsuke.args4j.Option;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Common options to both the {@link Backup} and {@link Restore} commands.
 */
public class CommonOptions {

    @Option(name = "-z", aliases = { "--zk-connect" }, usage = "ZooKeeper connection string (e.g. localhost:2181)",
            required = true, metaVar = "<zookeeper_connect>")
    String zkConnect;

    @Option(name = "--session-timeout", usage = "ZooKeeper session timeout (in milliseconds)", required = false,
            metaVar = "<timeout_in_ms>")
    long zkSessionTimeoutMs = TimeUnit.SECONDS.toMillis(30);

    @Option(name = "--compress", usage = "Compress output / Decompress input (using zlib)", required = false)
    boolean compress = false;

    @Option(name = "--exclude", usage = "Regular expression of paths to exclude", required = false,
            metaVar = "<exclude_regex>")
    List<String> excludePaths = new ArrayList<String>();

    @Option(name = "-v", aliases = { "--verbose" }, usage = "Verbose logging output", required = false)
    boolean verbose;

    @Option(name = "--root-path", usage = "ZooKeeper root path for backup/restore (default: '/')",
            required = false, metaVar = "<root_path>", handler = ZooKeeperPathOptionHandler.class)
    String rootPath = "/";

}
