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

/**
 * Options to the {@link Restore} command.
 */
public class RestoreOptions extends CommonOptions {

    @Option(name = "-f", aliases = { "--file" }, usage = "Input file to restore from (default: stdin)",
            required = false, metaVar = "<filename>")
    String inputFile = "-";

}
