/*
 * Copyright 2011-2018 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.util;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Input utilities for user searches.
 */
public final class SearchInputParsingUtils {

    private static final Logger log = LoggerFactory.getLogger(SearchInputParsingUtils.class);

    private static final String WHITESPACE_PATTERN = "\\s+";

    /**
     * Constructor preventing instantiation.
     */
    private SearchInputParsingUtils() {

    }

    /**
     * Returns a parsed version of the input.
     *
     * @param input                                the user input
     * @param allowSingleNonLeadingWildCardPerTerm if there is allowed one wildcard (* or ?) per term (however, still
     *                                             not allowed as leading for a term)
     * @return the parsed version of the <code>input</code>. When <code>input</code> is <code>null</code>,
     * <code>null</code> is returned
     *
     * Calls <code>#parse(input, allowSingleNonLeadingWildCardPerTerm, null, true)</code>
     */
    public static String parse(final String input, final boolean allowSingleNonLeadingWildCardPerTerm) {
        return parse(input, allowSingleNonLeadingWildCardPerTerm, null/*ignore*/, true);
    }

    /**
     * Returns a parsed version of the input.
     *
     * Calls <code>#parse(input, allowSingleNonLeadingWildCardPerTerm, null, retainWordBoundaries)</code>
     *
     * @param input                                the user input
     * @param allowSingleNonLeadingWildCardPerTerm if there is allowed one wildcard (* or ?) per term (however, still
     *                                             not allowed as leading for a term)
     * @param retainWordBoundaries                 whether to retain characters such as ~ & ! when they appear in a
     *                                             token as word boundaries or remove them, see also
     *                                             {@link #isSpecialChar(char)}
     * @return the parsed version of the <code>input</code>. When <code>input</code> is <code>null</code>,
     * <code>null</code> is returned
     */
    public static String parse(final String input, final boolean allowSingleNonLeadingWildCardPerTerm, final boolean retainWordBoundaries) {
        return parse(input, allowSingleNonLeadingWildCardPerTerm, null/*ignore*/, retainWordBoundaries);
    }

    /**
     * Returns a parsed version of the input.
     *
     * Calls <code>#parse(input, allowSingleNonLeadingWildCardPerTerm, ignore, true)</code>
     *
     * @param input                                the user input
     * @param allowSingleNonLeadingWildCardPerTerm if there is allowed one wildcard (* or ?) per term (however, still
     *                                             not allowed as leading for a term)
     * @param ignore                               the chars that should not be parsed
     * @return the parsed version of the <code>input</code>. When <code>input</code> is <code>null</code>,
     * <code>null</code> is returned
     */
    public static String parse(final String input, final boolean allowSingleNonLeadingWildCardPerTerm, final char[] ignore) {
        return parse(input, allowSingleNonLeadingWildCardPerTerm, ignore, true);
    }

    /**
     * Returns a parsed version of the input.
     *
     * @param input                                the user input
     * @param allowSingleNonLeadingWildCardPerTerm if there is allowed one wildcard (* or ?) per term (however, still
     *                                             not allowed as leading for a term)
     * @param ignore                               the chars that should not be parsed
     * @param retainWordBoundaries                 whether to retain characters such as ~ & ! when they appear in a
     *                                             token as word boundaries or remove them, see also
     *                                             {@link #isSpecialChar(char)}
     * @return the parsed version of the <code>input</code>. When <code>input</code> is <code>null</code>,
     * <code>null</code> is returned
     */
    public static String parse(final String input, final boolean allowSingleNonLeadingWildCardPerTerm, final char[] ignore, final boolean retainWordBoundaries) {
        if (input == null) {
            return null;
        }
        String parsed = EncodingUtils.foldToASCIIReplacer(input);
        parsed = compressWhitespace(parsed);
        parsed = removeInvalidAndEscapeChars(parsed, allowSingleNonLeadingWildCardPerTerm, ignore, retainWordBoundaries);
        parsed = removeLeadingOrTrailingOrOperator(parsed);
        parsed = rewriteNotOperatorsToMinus(parsed);
        parsed = removeLeadingAndTrailingAndReplaceWithSpaceAndOperators(parsed);
        return parsed;
    }

    /**
     * Returns a parsed version of the input.
     *
     * Calls <code>#parse(input, allowSingleNonLeadingWildCardPerTerm, maxLength, null, true)</code>
     *
     * @param input                                the user input
     * @param allowSingleNonLeadingWildCardPerTerm if there is allowed one wildcard (* or ?) per term (however, still
     *                                             not allowed as leading for a term)
     * @param maxLength                            the maxLength of the returned parsed input
     * @return the parsed version of the <code>input</code>. When <code>input</code> is <code>null</code>,
     * <code>null</code> is returned
     */
    public static String parse(final String input, final boolean allowSingleNonLeadingWildCardPerTerm, int maxLength) {
        return parse(input, allowSingleNonLeadingWildCardPerTerm, maxLength, null/*ignore*/, true);
    }

    /**
     * Returns a parsed version of the input.
     *
     * Calls <code>#parse(input, allowSingleNonLeadingWildCardPerTerm, maxLength, ignore, true)</code>
     *
     * @param input                                the user input
     * @param allowSingleNonLeadingWildCardPerTerm if there is allowed one wildcard (* or ?) per term (however, still
     *                                             not allowed as leading for a term)
     * @param maxLength                            the maxLength of the returned parsed input
     * @param ignore                               the chars that should not be parsed
     * @return the parsed version of the <code>input</code>. When <code>input</code> is <code>null</code>,
     * <code>null</code> is returned
     */
    public static String parse(final String input, final boolean allowSingleNonLeadingWildCardPerTerm, final int maxLength, final char[] ignore) {
        return parse(input, allowSingleNonLeadingWildCardPerTerm, maxLength, ignore, true);
    }

    /**
     * Returns a parsed version of the input.
     *
     * @param input                                the user input
     * @param allowSingleNonLeadingWildCardPerTerm if there is allowed one wildcard (* or ?) per term (however, still
     *                                             not allowed as leading for a term)
     * @param maxLength                            the maxLength of the returned parsed input
     * @param ignore                               the chars that should not be parsed
     * @param retainWordBoundaries                 whether to retain characters such as ~ & ! when they appear in a
     *                                             token as word boundaries or remove them, see also
     *                                             {@link #isSpecialChar(char)}
     * @return the parsed version of the <code>input</code>. When <code>input</code> is <code>null</code>,
     * <code>null</code> is returned
     */
    public static String parse(final String input, final boolean allowSingleNonLeadingWildCardPerTerm, final int maxLength, final char[] ignore, final boolean retainWordBoundaries) {
        if (input == null) {
            return null;
        }
        String parsed = parse(input, allowSingleNonLeadingWildCardPerTerm, ignore, retainWordBoundaries);
        if (parsed.length() > maxLength) {
            parsed = parsed.substring(0, maxLength);
        }
        return parsed;
    }

    public static String removeLeadingWildCardsFromWords(final String input) {
        if (input == null) {
            throw new IllegalArgumentException("Input is not allowed to be null");
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            // Some of these characters break the jcr query and others like * and ? have a very negative impact
            // on performance.
            if (c == '*' || c == '?') {
                if (sb.length() > 0) {
                    char prevChar = sb.charAt(sb.length() - 1);
                    if (!(prevChar == '\"' || prevChar == '\'' || prevChar == ' ')) {
                        sb.append(c);
                    }
                }
            } else {
                sb.append(c);
            }
        }
        String output = sb.toString();
        return output;
    }

    /**
     * <p>
     * Removes invalid chars, escapes some chars. If <code>allowSingleNonLeadingWildCard</code> is <code>true</code>,
     * there is one single non leading <code>*</code> or <code>?</code> allowed. Note, that this wildcard is not allowed
     * to be leading of a new word.
     * </p>
     * <p>
     * Recommended is to remove all wildcards
     * </p>
     *
     * @param input                                the user input
     * @param allowSingleNonLeadingWildCardPerTerm if there is allowed one wildcard (* or ?) per term (however, still
     *                                             not allowed as leading for a term)
     * @return formatted version of <code>input</code>
     */
    public static String removeInvalidAndEscapeChars(final String input, final boolean allowSingleNonLeadingWildCardPerTerm) {
        return removeInvalidAndEscapeChars(input, allowSingleNonLeadingWildCardPerTerm, null/*ignore*/, false);
    }

    /**
     * <p>
     * Removes invalid chars, escapes some chars. If <code>allowSingleNonLeadingWildCard</code> is <code>true</code>,
     * there is one single non leading <code>*</code> or <code>?</code> allowed. Note, that this wildcard is not allowed
     * to be leading of a new word.
     * </p>
     * <p>
     * Recommended is to remove all wildcards
     * </p>
     *
     * @param input                                the user input
     * @param allowSingleNonLeadingWildCardPerTerm if there is allowed one wildcard (* or ?) per term (however, still
     *                                             not allowed as leading for a term)
     * @param ignore                               the chars that should not be parsed
     * @return formatted version of <code>input</code>
     */
    public static String removeInvalidAndEscapeChars(final String input, final boolean allowSingleNonLeadingWildCardPerTerm, final char[] ignore) {
        return removeInvalidAndEscapeChars(input, allowSingleNonLeadingWildCardPerTerm, ignore, false);
    }

    /**
     * <p>
     * Removes invalid chars, escapes some chars. If <code>allowSingleNonLeadingWildCard</code> is <code>true</code>,
     * there is one single non leading <code>*</code> or <code>?</code> allowed. Note, that this wildcard is not allowed
     * to be leading of a new word.
     * </p>
     * <p>
     * Recommended is to remove all wildcards
     * </p>
     *
     * @param input                                the user input
     * @param allowSingleNonLeadingWildCardPerTerm if there is allowed one wildcard (* or ?) per term (however, still
     *                                             not allowed as leading for a term)
     * @param ignore                               the chars that should not be parsed
     * @param retainWordBoundaries                 whether to retain special characters as word boundaries or remove
     *                                             them
     * @return formatted version of <code>input</code>
     */
    public static String removeInvalidAndEscapeChars(final String input, final boolean allowSingleNonLeadingWildCardPerTerm, final char[] ignore, final boolean retainWordBoundaries) {
        if (input == null) {
            throw new IllegalArgumentException("Input is not allowed to be null");
        }
        final StringBuffer sb = new StringBuffer();
        boolean allowWildCardInCurrentTerm = allowSingleNonLeadingWildCardPerTerm;

        boolean prevCharIsSpecialOrRemoved = false;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            // Some of these characters break the jcr query and others like * and ? have a very negative impact
            // on performance.
            if (!ignoreChar(c, ignore) && isSpecialChar(c)) {
                if (c == '\"') {
                    sb.append('\\');
                    sb.append(c);
                } else if (c == '\'') {
                    sb.append("''");
                } else if (c == ' ') {
                    // next term. set allowWildCardInCurrentTerm again to allowSingleNonLeadingWildCardPerTerm
                    allowWildCardInCurrentTerm = allowSingleNonLeadingWildCardPerTerm;
                    sb.append(c);
                } else {
                    // '~' is used for synonyms search: This is jackrabbit specific and different than lucene fuzzy
                    // see http://wiki.apache.org/jackrabbit/SynonymSearch. It is only allowed to be the first char
                    // of a word
                    // '!' or '-' are used to NOT a term. However, we only allow them at the beginning of a term as they
                    // do not make sense any where else
                    if (c == '~' || c == '!' || c == '-') {
                        if (sb.length() == 0) {
                            if (containsNextCharAndIsNotSpecial(input, i)) {
                                sb.append(c);
                            }
                        } else {
                            char prevChar = sb.charAt(sb.length() - 1);
                            if (prevChar == ' ' && containsNextCharAndIsNotSpecial(input, i)) {
                                sb.append(c);
                            } else if (c == '-') {
                                // check next char : only if next char is again a non-special char we include the '-'
                                if (containsNextCharAndIsNotSpecial(input, i)) {
                                    sb.append(c);
                                }
                            } else {
                                if (retainWordBoundaries) {
                                    sb.append(' ');
                                } // else skip it to provide old behavior
                            }
                        }
                    } else if (sb.length() > 0) {
                        // if one wildcard is allowed, it will be added but never as leading
                        // also if the wildcard is found after a special char (like '!', '-', '&', ' ' etc, it will be skipped as well)
                        if (c == '*' || c == '?') {
                            if (allowWildCardInCurrentTerm && !prevCharIsSpecialOrRemoved) {
                                // check if prev char is not a space or " or  '
                                // i must be > 0 here
                                char prevChar = sb.charAt(sb.length() - 1);
                                if (!(prevChar == '\"' || prevChar == '\'' || prevChar == ' ')) {
                                    sb.append(c);
                                    allowWildCardInCurrentTerm = false;
                                }
                            }
                        } else if (retainWordBoundaries && i != 0 && input.charAt(i-1) != ' ' && containsNextCharAndIsNotSpecial(input, i)) {
                            sb.append(' ');
                        }
                    }
                }
                prevCharIsSpecialOrRemoved = true;
            } else {
                sb.append(c);
                prevCharIsSpecialOrRemoved = false;
            }
        }
        String output = sb.toString();
        return compressWhitespace(output);
    }

    private static boolean ignoreChar(final char c, final char[] ignore) {
        if (ignore == null) {
            return false;
        }
        for (char ignoreChar : ignore) {
            if (c == ignoreChar) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsNextCharAndIsNotSpecial(final String input, final int cursor) {
        if ((input.length() > cursor + 1) && !isSpecialChar(input.charAt(cursor + 1))) {
            return true;
        }
        return false;
    }

    public static boolean isSpecialChar(final char c) {
        return c == '(' || c == ')' || c == '^' || c == '[' || c == ']' || c == '{'
                || c == '}' || c == '~' || c == '*' || c == '?' || c == '|' || c == '&'
                || c == '!' || c == '-' || c == '\"' || c == '\'' || c == ' ' || c == '\\';
    }

    /**
     * Rewrites any "NOT" operators in the keywords to a minus symbol (-). This is necessary because Jackrabbit doesn't
     * fully support the Lucene query language. Jackrabbit <em>does</em> support the minus symbol to exclude keywords
     * from search results but <em>does not</em> support the "NOT" keyword.
     *
     * @param input keywords to rewrite
     * @return rewritten input
     */
    private static String rewriteNotOperatorsToMinus(String input) {
        if (input == null) {
            throw new IllegalArgumentException("Input is not allowed to be null");
        }
        return input.replace("NOT ", "-");
    }

    /**
     * Removes any "AND" operators in the keywords. This is necessary because Jackrabbit doesn't
     * fully support the Lucene query language. Lucene by default applies "and" to all keywords so to support the "AND"
     * operator it can be simply removed from the keywords.
     *
     * @param input keywords to rewrite
     * @return rewritten input
     */
    private static String removeLeadingAndTrailingAndReplaceWithSpaceAndOperators(String input) {
        if (input == null) {
            throw new IllegalArgumentException("Input is not allowed to be null");
        }
        String output = input;
        output = StringUtils.removeStart(output, "AND ");
        output = StringUtils.removeEnd(output, " AND");
        return output.replace(" AND ", " ");
    }

    /**
     * Removes the logical operator "OR" at the end of the query. Otherwise this will result in a Lucene parse
     * exception.
     *
     * @param input the original (possibly invalid) query string
     * @return a valid query string
     */
    public static String removeLeadingOrTrailingOrOperator(String input) {
        if (input == null) {
            throw new IllegalArgumentException("Input is not allowed to be null");
        }
        String output = input;
        output = StringUtils.removeStart(output, "OR ");
        output = StringUtils.removeEnd(output, " OR");
        return output;
    }

    /**
     * Compress whitespace (tab, newline, multiple spaces) by removing leading and trailing whitespace, and reducing
     * in-between whitespace to one space.
     *
     * @param text the text to compress (may be null)
     * @return the compressed text, or null if the text to compress was null
     */
    public static String compressWhitespace(String text) {
        if (text == null) {
            return null;
        }
        String trimmedText = StringUtils.trim(text);
        return trimmedText.replaceAll(WHITESPACE_PATTERN, " ");
    }
}
