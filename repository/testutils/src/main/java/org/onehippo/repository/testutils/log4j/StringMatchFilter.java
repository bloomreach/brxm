/*
 *  Copyright 2013-2013 Hippo B.V. (http://www.onehippo.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.onehippo.repository.testutils.log4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.spi.Filter;
import org.apache.log4j.spi.LoggingEvent;

/**
 * This is a very simple filter based on string matching.
 *
 * <p>The filter reads lines from a called <code>log4j-filters.txt</code>
 * that is expected to be on the classpath. Each line that does not start with # represents a string to
 * to match the log message to filter against. If the log message contains one
 * of the configured strings it is filtered out. Lines may also be regular expressions.
 * Lines that start with # are treated as comments and ignored</p>
 */
public class StringMatchFilter extends Filter {

    private final List<String> stringsToMatch = new ArrayList<String>();
    private final List<Pattern> patternsToMatch = new ArrayList<>();

    public StringMatchFilter() {
        final InputStream resourceAsStream = getClass().getClassLoader().getResourceAsStream("log4j-filters.txt");
        if (resourceAsStream != null) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(resourceAsStream))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty()) {
                        continue;
                    }
                    if (line.startsWith("#")) {
                        continue;
                    }
                    stringsToMatch.add(line);
                    patternsToMatch.add(Pattern.compile(line));
                }
            } catch (PatternSyntaxException ignored) {
            } catch (IOException e) {
                System.err.println("Error while initializing log4j StringMatchFilter: " + e);
            }
        } else {
            System.err.println("StringMatchFilter: Could not find log4j-filters.txt");
        }
    }

    @Override
    public int decide(final LoggingEvent event) {
        String msg = event.getRenderedMessage();

        if(msg == null || stringsToMatch.isEmpty()) {
            return Filter.NEUTRAL;
        }

        if (matches(msg)) {
            return Filter.DENY;
        }
        return Filter.NEUTRAL;
    }

    private boolean matches(final String msg) {
        for (String s : stringsToMatch) {
            if (msg.contains(s)) {
                return true;
            }
        }
        for (Pattern pattern : patternsToMatch) {
            if (pattern.matcher(msg).matches()) {
                return true;
            }
        }
        return false;
    }

}
