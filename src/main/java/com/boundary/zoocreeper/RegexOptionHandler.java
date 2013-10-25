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

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.OptionDef;
import org.kohsuke.args4j.spi.OptionHandler;
import org.kohsuke.args4j.spi.Parameters;
import org.kohsuke.args4j.spi.Setter;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * args4j handler for parsing a regular expression pattern.
 */
public class RegexOptionHandler extends OptionHandler<Pattern> {
    public RegexOptionHandler(CmdLineParser parser, OptionDef option, Setter<? super Pattern> setter) {
        super(parser, option, setter);
    }

    @Override
    public int parseArguments(Parameters params) throws CmdLineException {
        final String regex = params.getParameter(0);
        try {
            final Pattern pattern = Pattern.compile(regex);
            setter.addValue(pattern);
        } catch (PatternSyntaxException e) {
            throw new CmdLineException(owner, "Invalid regular expression: " + regex, e);
        }
        return 1;
    }

    @Override
    public String getDefaultMetaVariable() {
        return "regex";
    }
}
