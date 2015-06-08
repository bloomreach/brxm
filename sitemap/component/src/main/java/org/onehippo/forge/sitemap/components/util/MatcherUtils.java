/*
 * Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *  http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.forge.sitemap.components.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utilities for resolving matchers like ${1}, ${2}, etc.. in sitemap content paths
 */
public final class MatcherUtils {

    private static final Pattern ANY_OR_DEFAULT = Pattern.compile("(_default_|_any_)");
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("(\\$\\{\\d\\})");

    // RegExp matching (optional-prefix:)property(optional-spaces)=(optional-spaces)value
    private static final Pattern PROPERTY_VALUE_PATTERN = Pattern.compile("([\\w]+:?[\\w]+)[\\s]*=[\\s]*([\\w]+)");

    private static final String COMMA_SEPARATING_REG_EXP = "[\\s]*,[\\s]*";

    private MatcherUtils() {} // Hide constructor of utility class

    /**
     * Resolves the matched node for a matcher at the passed index. For example if the contentPathWithMatcher is
     * "/content/documents/news/${1}" and the pathToMatch is "/content/documents/news/pressrelease-2012" with index 1
     * then this method will return "pressrelease-2012"
     *
     * @param contentPathWithMatcher the content path with the matcher to use as a base
     * @param pathToMatch            the path to match the versus the base
     * @param index                  the index for the matcher
     * @return the matched node in the place of the matcher
     */
    public static String obtainMatchedNodeForMatcher(final String contentPathWithMatcher,
                                                     final String pathToMatch,
                                                     final int index) {
        String matcher = getMatcherForIndex(index);
        if (!contentPathWithMatcher.contains(matcher)) {
            throw new IllegalArgumentException("contentPathWithMatcher does not contain a matcher for index = " + index);
        }
        int matcherLocation = contentPathWithMatcher.indexOf(matcher);
        String prefix = contentPathWithMatcher.substring(0, matcherLocation);
        int suffixStartIdx = matcherLocation + matcher.length();
        String suffix = contentPathWithMatcher.substring(suffixStartIdx);

        if (!pathToMatch.startsWith(prefix) || !pathToMatch.endsWith(suffix)) {
            throw new IllegalArgumentException("pathToMatch does not meet the pattern of the contentPathWithMatcher");
        }

        int beginIdx = prefix.length();
        int endIdx = pathToMatch.lastIndexOf(suffix);

        return pathToMatch.substring(beginIdx, endIdx);
    }

    /**
     * Replaces placeholder with their respective matched nodes. ${1} is replaced with matchedNodes[0] and so forth
     * @param stringWithPlaceholders a string containing placeholders
     * @param matchedNodes the matched nodes for the different placeholders
     * @param replaceWithStars whether or not to replace a placeholder with a "*"
     * @return String with placeholders replaced by their matched nodes
     */
    public static String replacePlaceholdersWithMatchedNodes(final String stringWithPlaceholders,
                                                             final List<String> matchedNodes,
                                                             final boolean replaceWithStars) {
        String result = stringWithPlaceholders;
        int idx = 1;
        for (String matchedNode : matchedNodes) {
            if (!replaceWithStars && matchedNode.equals("*")) {
                // If we should not replace stars, ignore this matched node if it is a star
                continue;
            }
            String matcher = getMatcherForIndex(idx);
            result = result.replace(matcher, matchedNode);
            idx++;
        }
        return result;
    }

    /**
     * Replaces placeholder with their respective matched nodes. ${1} is replaced with matchedNodes[0] and so forth.
     * Internally this calls replacePlaceholdersWithMatchesNodes(stringWithPlaceholders, matchedNodes, true).
     * @param stringWithPlaceholders a string containing placeholders
     * @param matchedNodes the matched nodes for the different placeholders
     * @return String with placeholders replaced by their matched nodes
     */
    public static String replacePlaceholdersWithMatchedNodes(final String stringWithPlaceholders,
                                                             final List<String> matchedNodes) {
        return replacePlaceholdersWithMatchedNodes(stringWithPlaceholders, matchedNodes, true);
    }

    /**
     * Returns the matcher string representation for the passed index as follows: "${idx}"
     * @param idx the index of this matcher
     * @return the String representation in the form ${idx}
     */
    public static String getMatcherForIndex(final int idx) {
        return "${i}".replace("i", Integer.toString(idx));
    }

    /**
     * Takes a string with placeholders and a string to parse and returns the values of the placeholders in a map
     * @param stringWithPlaceholders The "template" string to use to parse the other string
     * @param stringToParse The string to parse for placeholder values
     * @return a {@link Map} with placeholder numbers as keys and values
     */
    public static Map<Integer, String> extractPlaceholderValues(final String stringWithPlaceholders,
                                                                final String stringToParse) {
        Map<Integer, String> result = new HashMap<>();

        Pattern matcherPattern = buildPatternWithCaptureGroupsFromStringWithPlaceholders(stringWithPlaceholders);
        Matcher matcher = matcherPattern.matcher(stringToParse);

        if (matcher.find()) {
            Integer[] placeHolderOrdering = resolveOrderOfPlaceholders(stringWithPlaceholders);
            for (int i = 1 ; i <= matcher.groupCount() ; i++) {
                // Array indexes are 0-based, matcher group indexes are 1-based
                result.put(placeHolderOrdering[i - 1], matcher.group(i));
            }
        } else {
            throw new IllegalArgumentException("Cannot match stringWithPlaceholders: "+ stringWithPlaceholders
                    + " to stringToParse: " + stringToParse);
        }

        return result;
    }

    public static String replaceDefaultAndAnyMatchersWithMatchedNodes(final String path,
                                                                      final List<String> matchedNodes) {
        String newPath = path;
        for (String matchedNode : matchedNodes) {
            Matcher matcher = ANY_OR_DEFAULT.matcher(newPath);
            if (matcher.find()) {
                newPath = matcher.replaceFirst(matchedNode);
            }
        }
        return newPath;
    }

    /**
     * Get the comma separated values from a string.
     */
    public static String[] getCommaSeparatedValues(final String values) {
        if (values == null) {
            return new String[]{};
        }
        return values.trim().split(COMMA_SEPARATING_REG_EXP);
    }

    /**
     * Get a property/value pair for a string of format 'property=value'.
     *
     * @return null if the argument is not a 'property=value' string, else the property and value in a 2-string array
     */
    public static String[] parsePropertyValue(final String propertyAndValue) {
        Matcher matcher = PROPERTY_VALUE_PATTERN.matcher(propertyAndValue);
        if (!matcher.matches()) {
            return null;
        }

        return new String[] {matcher.group(1), matcher.group(2)};
    }

    /**
     * Parses a String with placeholders and returns a list of placeholder numbers in the order that they appear in
     * the String.
     * @param stringWithPlaceholders the String to parse
     * @return {@link Integer[]} with the order in which placeholder appear in the String
     */
    private static Integer[] resolveOrderOfPlaceholders(final String stringWithPlaceholders) {
        List<Integer> placeholderList = new ArrayList<>();

        Matcher placeholderMatcher = Pattern.compile("(\\$\\{(\\d)\\})").matcher(stringWithPlaceholders);
        while (placeholderMatcher.find()) {
            placeholderList.add(Integer.parseInt(placeholderMatcher.group(2)));
        }

        Integer[] result = new Integer[placeholderList.size()];
        return placeholderList.toArray(result);
    }

    /**
     * Returns a Pattern based on a String with placeholders where every placeholder is replaced by a capture group
     * @param stringWithPlaceholders the string containing placeholders
     * @return a {@link Pattern} with capture groups
     */
    private static Pattern buildPatternWithCaptureGroupsFromStringWithPlaceholders(final String stringWithPlaceholders) {
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(stringWithPlaceholders);
        String resultingPattern = matcher.replaceAll("(.*)");
        return Pattern.compile(resultingPattern);
    }
}
