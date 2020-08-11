/*
 *  Copyright 2013-2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.testutils.log4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.filter.AbstractFilter;

/**
 * This is a very simple filter based on string matching.
 *
 * <p>The filter reads lines from a called <code>log4j-filters.txt</code>
 * that is expected to be on the classpath. Each line that does not start with # represents a string to
 * to match the log message to filter against. If the log message contains one
 * of the configured strings it is filtered out. Lines may also be regular expressions.
 * Lines that start with # are treated as comments and ignored</p>
 * <p>
 * Note: this class originally was provided through the hippo-repository-testutils module,
 * org.onehippo.repository.testutils.log4j.StringMatchFilter which was log4j1 based.
 * </p>
 * <p>
 * For log4j2 the class needed to be rewritten as log4j2 plugin, and now is more conveniently provided
 * through the hippo-cms-testutils module (which hippo-repository-testutils depends upon).
 * </p>
 */
@Plugin(name = "StringMatchFilter", category = Node.CATEGORY, elementType = Filter.ELEMENT_TYPE, printObject = true)
public class StringMatchFilter extends AbstractFilter {

    private final List<String> stringsToMatch = new ArrayList<String>();
    private final List<Pattern> patternsToMatch = new ArrayList<>();

    public StringMatchFilter() {
        super(Result.DENY, Result.NEUTRAL);
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
    public Result filter(final LogEvent event) {
        String msg = event.getMessage().getFormattedMessage();

        if(msg == null || stringsToMatch.isEmpty()) {
            return Result.NEUTRAL;
        }

        if (matches(msg)) {
            return Result.DENY;
        }
        return Result.NEUTRAL;
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

    @PluginFactory
    public static StringMatchFilter createFilter() {
        return new StringMatchFilter();
    }
}
