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

import org.apache.zookeeper.common.PathUtils;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.OptionDef;
import org.kohsuke.args4j.spi.OptionHandler;
import org.kohsuke.args4j.spi.Parameters;
import org.kohsuke.args4j.spi.Setter;

/**
 * Option handler for a ZooKeeper path.
 */
public class ZooKeeperPathOptionHandler extends OptionHandler<String> {

    public ZooKeeperPathOptionHandler(CmdLineParser parser, OptionDef option, Setter<? super String> setter) {
        super(parser, option, setter);
    }

    @Override
    public int parseArguments(Parameters params) throws CmdLineException {
        String param = params.getParameter(0);
        try {
            PathUtils.validatePath(param);
            setter.addValue(param);
            return 1;
        } catch (IllegalArgumentException e) {
            throw new CmdLineException(owner,
                    String.format("\"%s\" is not a valid value for \"%s\"", param, params.getParameter(-1)));
        }
    }

    @Override
    public String getDefaultMetaVariable() {
        return "zk_path";
    }
}
