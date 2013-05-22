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

import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

import java.io.IOException;

/**
 * Interface used to create a ZooKeeper client.
 */
public interface ZooKeeperFactory {
    /**
     * Creates a {@link org.apache.zookeeper.ZooKeeper} client.
     *
     * @param options The options used to create the client.
     * @param watcher The default {@link Watcher} to use for the client.
     * @return A new {@link ZooKeeper} client.
     */
    ZooKeeper createZooKeeper(CommonOptions options, Watcher watcher) throws IOException;
}
