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
package org.onehippo.cm.model;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.commons.lang3.tuple.Pair;

public class SnsUtils {

    private static Pattern pattern = Pattern.compile("^([^\\[\\]]+)(\\[([1-9][0-9]*)])?$");

    public static Pair<String, Integer> splitIndexedName(final String name) {
        final Matcher matcher = pattern.matcher(name);
        if (!matcher.find()) {
            throw new IllegalArgumentException("Name '" + name + "' is not a valid indexed name");
        }
        try {
            if (matcher.group(2) == null) {
                return Pair.of(matcher.group(1), 0);
            }
            final int index = Integer.parseInt(matcher.group(3));
            return Pair.of(matcher.group(1), index);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Name '" + name + "' is not a valid indexed name");
        }
    }

    public static String createIndexedName(final String unindexedName, final int index) {
        return unindexedName + '[' + index + ']';
    }

    public static String createIndexedName(final Node node) throws RepositoryException {
        return createIndexedName(node.getName(), node.getIndex());
    }

    public static String createIndexedName(final String name) {
        // TODO improve parsing
        if (name.endsWith("]")) {
            return name;
        }
        return name + "[1]";
    }

    public static String getUnindexedName(final String name) {
        return splitIndexedName(name).getLeft();
    }

    public static boolean hasSns(final String name, final Set<String> siblings) {
        final Pair<String, Integer> parsedName = splitIndexedName(name);
        if (parsedName.getRight() > 1) {
            return true;
        }
        if (parsedName.getRight() == 1 && siblings.contains(createIndexedName(parsedName.getLeft(), 2))) {
            return true;
        }
        return false;
    }

}
