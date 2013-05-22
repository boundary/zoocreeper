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
* Options to the backup command.
*/
class BackupOptions extends CommonOptions {

    @Option(name = "--ephemeral", usage = "Backup ephemeral nodes", required = false)
    boolean backupEphemeral = false;

    @Option(name = "--retries", usage = "Number of retries to read consistent data", required = false,
            metaVar = "<num_retries>")
    int numRetries = 5;

    @Option(name = "-f", aliases = { "--file" }, usage = "Output file for backup data (default: stdout)",
            required = false, metaVar = "<filename>")
    String outputFile = "-";

    @Option(name = "--pretty-print", usage = "Pretty printing of JSON output", required = false)
    boolean prettyPrint = false;

}
