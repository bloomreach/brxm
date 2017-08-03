/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cm.engine.autoexport;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.onehippo.cm.model.tree.ConfigurationItemCategory;

/**
 * Class representing the patterns for the .meta:residual-child-node-category override configuration.
 *
 * A pattern should be formatted as "path: category". The path pattern supports wildcards: * matches any number of
 * characters except a /, ** matches any number of characters.
 */
class OverrideResidualMatchers {

    private final List<Matcher> matchers;

    OverrideResidualMatchers(final String... patterns) throws IllegalArgumentException {
        this.matchers = new ArrayList<>(patterns.length);
        for (String pattern : patterns) {
            matchers.add(new Matcher(pattern));
        }
    }

    void add(final String string) throws IllegalArgumentException {
        matchers.add(new Matcher(string));
    }

    /**
     * @return the category of the first matching pattern, if there is a pattern match on the path of the given node,
     *         or null, if there is no match
     */
    ConfigurationItemCategory getMatch(final String path) {
        for (final Matcher matcher : matchers) {
            if (matcher.matches(path)) {
                return matcher.category;
            }
        }
        return null;
    }

    private static class Matcher {
        final Pattern pattern;
        final ConfigurationItemCategory category;

        Matcher(final String string) {
            final String illegalFormatMessage = "Illegal pattern: '" + string
                    + "', pattern must be formatted as 'path: category'";
            final int colon = string.lastIndexOf(": ");

            if (colon == -1) {
                throw new IllegalArgumentException(illegalFormatMessage);
            }
            try {
                category = ConfigurationItemCategory.valueOf(string.substring(colon + 2).toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(illegalFormatMessage);
            }
            final String path = string.substring(0, colon);
            if (path.trim().equals("")) {
                throw new IllegalArgumentException(illegalFormatMessage);
            }
            pattern = Pattern.compile(PatternSet.compile(path));
        }

        boolean matches(final String path) {
            return pattern.matcher(path).matches();
        }
    }

}
