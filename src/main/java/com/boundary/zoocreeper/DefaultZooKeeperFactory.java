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
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

import java.io.IOException;

/**
 * Default implementation of {@link ZooKeeperFactory}.
 */
public class DefaultZooKeeperFactory implements ZooKeeperFactory {
    @Override
    public ZooKeeper createZooKeeper(CommonOptions options, Watcher watcher) throws IOException {
        return new ZooKeeper(options.zkConnect, Ints.checkedCast(options.zkSessionTimeoutMs), watcher);
    }
}
