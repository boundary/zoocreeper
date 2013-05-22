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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utilities for logging.
 */
public final class LoggingUtils {

    private LoggingUtils() {}

    /**
     * Enables debug logging on the specified logger name.
     */
    public static void enableDebugLogging(String packageName) {
        Logger logger = (Logger) LoggerFactory.getLogger(packageName);
        logger.setLevel(Level.DEBUG);
    }
}
