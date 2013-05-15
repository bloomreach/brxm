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
package org.onehippo.cms7.services.search.jcr.query;

import java.util.StringTokenizer;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Input utilities for user searches.
 *
 */
public final class FullTextSearchParser {

    static final Logger log = LoggerFactory.getLogger(FullTextSearchParser.class);

    private static final String DEFAULT_IGNORED_CHARS = "&|!(){}[]^\"~*?:\\";

    private static final char MINUS_SIGN = '-';

    private final static String ignoredChars = DEFAULT_IGNORED_CHARS;

    private final static int minimalLength = 3;

    private static final String WHITESPACE_PATTERN = "\\s+";

    private FullTextSearchParser() {
    }

    /**
     * Returns a parsed version of the input
     * @param input the user input
     * @param allowSingleNonLeadingWildCardPerTerm if there is allowed one wildcard (* or ?) per term (however, still not allowed as leading for a term)
     * @return the parsed version of the <code>input</code>. When <code>input</code> is <code>null</code>, <code>null</code> is returned
     */
    public static String fullTextParseHstMode(final String input, final boolean allowSingleNonLeadingWildCardPerTerm) {
        if(input == null) {
            return null;
        }        
        String parsed = compressWhitespace(input);
        parsed = removeInvalidAndEscapeChars(parsed, allowSingleNonLeadingWildCardPerTerm);
        parsed = removeLeadingOrTrailingOrOperator(parsed);
        parsed = rewriteNotOperatorsToMinus(parsed);
        parsed = removeLeadingAndTrailingAndReplaceWithSpaceAndOperators(parsed);
        parsed = isoLatin1AccentReplacer(parsed);
        log.debug("Rewrote input '{}' to '{}'", input, parsed);
        return parsed;
    }

    public static String fullTextParseCmsSimpleSearchMode(String value, final boolean wildcardPostfix) {
        value = isoLatin1AccentReplacer(value.trim());
        StringBuilder whereClauseBuilder = new StringBuilder();
        boolean isOperatorToken;
        String peekedToken = null;
        for (StringTokenizer st = new StringTokenizer(value, " "); st.hasMoreTokens() || peekedToken != null ;) {
            String token;
            if (peekedToken != null) {
                token = peekedToken;
                peekedToken = null;
            } else {
                token = st.nextToken();
            }
            StringBuilder tb = new StringBuilder();
            for (int i = 0; i < token.length(); i++) {
                char c = token.charAt(i);
                if (ignoredChars.indexOf(c) == -1) {
                    if (c == '\'') {
                        tb.append('\\');
                    }
                    if (c == MINUS_SIGN) {
                        // we do not allowe minus sign followed by a space or ignored char
                        if (token.length() > i + 1) {
                            char nextChar = token.charAt(i + 1);
                            if (nextChar == ' ' || ignoredChars.indexOf(nextChar) > -1) {
                                // not allowed position for -
                            } else {
                                tb.append(c);
                            }
                        }
                    } else {
                        tb.append(c);
                    }
                }
            }
            if (tb.length() == 0) {
                continue;
            }


            if (token.equals("OR") || token.equals("AND")) {
                isOperatorToken = true;
            } else {
                isOperatorToken = false;
            }

            if (wildcardPostfix && tb.length() < getMinimalLength() && !isOperatorToken) {
                // for wildcard postfixing we demand the term to be at least as long as #getMinimalLength()
                continue;
            }

            // add a space (this defaults to AND)
            if (isOperatorToken && !st.hasMoreTokens()) {
                // we do not allow an operator AND or OR as last token
                continue;
            }

            // now we could still have that the problem that the query ends with AND AND : thus we need to peek the next token
            // if there are more to double check it is not a operator
            if (isOperatorToken && st.hasMoreTokens()) {
                peekedToken = st.nextToken();
                if (peekedToken.equals("OR") || peekedToken.equals("AND")) {
                    // the next token is an operator. Skip current one
                    continue;
                }
            }

            if (isOperatorToken && whereClauseBuilder.length() == 0) {
                // first term is not allowed to be an operator hence skip
                continue;
            }

            if (whereClauseBuilder.length() > 0) {
                whereClauseBuilder.append(" ");
            }

            whereClauseBuilder.append(tb);

            // we only append a wildcard IF and only IF
            // 1: WildcardSearch is set to true
            // 2: The term length is at least equal to minimal length: This is to avoid expensive kind of a* searches
            // 3: The term is not an operator token like AND or OR
            if (wildcardPostfix && tb.length() >= getMinimalLength() && !isOperatorToken) {
                whereClauseBuilder.append('*');
            }
        }
        return whereClauseBuilder.toString();
    }

    public static int getMinimalLength() {
        return minimalLength;
    }


    /**
     * <p>
     * Removes invalid chars, escapes some chars. If <code>allowSingleNonLeadingWildCard</code> is <code>true</code>, there
     * is one single non leading <code>*</code> or <code>?</code> allowed. Note, that this wildcard is not allowed to be
     * leading of a new word.
     * </p>
     * <p>
     * Recommended is to remove all wildcards
     * </p>
     * @param input
     * @param allowSingleNonLeadingWildCardPerTerm
     * @return formatted version of <code>input</code>
     */
    public static String removeInvalidAndEscapeChars(final String input, final boolean allowSingleNonLeadingWildCardPerTerm) {
        if(input == null) {
            throw new IllegalArgumentException("Input is not allowed to be null");
        }
        StringBuilder sb = new StringBuilder();
        boolean allowWildCardInCurrentTerm = allowSingleNonLeadingWildCardPerTerm;

        boolean prevCharIsSpecialOrRemoved = false;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            // Some of these characters break the jcr query and others like * and ? have a very negative impact
            // on performance.
            if (isSpecialChar(c)) {
                if (c == '\"') {
                    sb.append('\\');
                    sb.append(c);
                } else if (c == '\'') {
                    // we strip ' because jackrabbit xpath builder breaks on \' (however it should be possible according spec)
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
                            if (prevChar == ' ') {
                                sb.append(c);
                            } else if (c == '-') {
                                // check next char : only if next char is again a non-special char we include the '-'
                                if (containsNextCharAndIsNotSpecial(input, i)) {
                                    sb.append(c);
                                }
                            }
                            // else we remove the ~ , !, -
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
        if(!input.equals(output)) {
           log.debug("Rewrote input '{}' to '{}'", input, output);
        }
        return output;
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
                || c =='!' || c == '-' || c == '\"' || c == '\'' || c == ' ';
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
        if(input == null) {
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
        if(input == null) {
            throw new IllegalArgumentException("Input is not allowed to be null");
        }
        String output = input;
        output = StringUtils.removeStart(output, "AND ");
        output = StringUtils.removeEnd(output, " AND");
        return output.replace(" AND ", " ");
    }
    
    /**
     * Removes the logical operator "OR" at the end of the query. Otherwise this will result in a Lucene parse exception.
     *
     * @param input the original (possibly invalid) query string
     * @return a valid query string
     */
    public static String removeLeadingOrTrailingOrOperator(String input) {
        if(input == null) {
            throw new IllegalArgumentException("Input is not allowed to be null");
        }
        String output = input;
        output = StringUtils.removeStart(output, "OR ");
        output = StringUtils.removeEnd(output, " OR");
        return output;
    }
    
    /**
     * Compress whitespace (tab, newline, multiple spaces) by removing leading and trailing whitespace, and reducing
     * inbetween whitespace to one space.
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

    public static String isoLatin1AccentReplacer(String input) {
        if(input == null) {
            return null;
        }

        char[] inputChars = input.toCharArray();
        // Worst-case length required:
        char[] output = new char[inputChars.length * 2];

        int outputPos = 0;

        int pos = 0;
        int length = inputChars.length;

        for (int i = 0; i < length; i++, pos++) {
            final char c = inputChars[pos];

            // Quick test: if it's not in range then just keep
            // current character
            if (c < '\u00c0')
                output[outputPos++] = c;
            else {
                switch (c) {
                    case '\u00C0': // À
                    case '\u00C1': // Á
                    case '\u00C2': // Â
                    case '\u00C3': // Ã
                    case '\u00C4': // Ä
                    case '\u00C5': // Å
                        output[outputPos++] = 'A';
                        break;
                    case '\u00C6': // Æ
                        output[outputPos++] = 'A';
                        output[outputPos++] = 'E';
                        break;
                    case '\u00C7': // Ç
                        output[outputPos++] = 'C';
                        break;
                    case '\u00C8': // È
                    case '\u00C9': // É
                    case '\u00CA': // Ê
                    case '\u00CB': // Ë
                        output[outputPos++] = 'E';
                        break;
                    case '\u00CC': // Ì
                    case '\u00CD': // Í
                    case '\u00CE': // Î
                    case '\u00CF': // Ï
                        output[outputPos++] = 'I';
                        break;
                    case '\u00D0': // Ð
                        output[outputPos++] = 'D';
                        break;
                    case '\u00D1': // Ñ
                        output[outputPos++] = 'N';
                        break;
                    case '\u00D2': // Ò
                    case '\u00D3': // Ó
                    case '\u00D4': // Ô
                    case '\u00D5': // Õ
                    case '\u00D6': // Ö
                    case '\u00D8': // Ø
                        output[outputPos++] = 'O';
                        break;
                    case '\u0152': // Œ
                        output[outputPos++] = 'O';
                        output[outputPos++] = 'E';
                        break;
                    case '\u00DE': // Þ
                        output[outputPos++] = 'T';
                        output[outputPos++] = 'H';
                        break;
                    case '\u00D9': // Ù
                    case '\u00DA': // Ú
                    case '\u00DB': // Û
                    case '\u00DC': // Ü
                        output[outputPos++] = 'U';
                        break;
                    case '\u00DD': // Ý
                    case '\u0178': // Ÿ
                        output[outputPos++] = 'Y';
                        break;
                    case '\u00E0': // à
                    case '\u00E1': // á
                    case '\u00E2': // â
                    case '\u00E3': // ã
                    case '\u00E4': // ä
                    case '\u00E5': // å
                        output[outputPos++] = 'a';
                        break;
                    case '\u00E6': // æ
                        output[outputPos++] = 'a';
                        output[outputPos++] = 'e';
                        break;
                    case '\u00E7': // ç
                        output[outputPos++] = 'c';
                        break;
                    case '\u00E8': // è
                    case '\u00E9': // é
                    case '\u00EA': // ê
                    case '\u00EB': // ë
                        output[outputPos++] = 'e';
                        break;
                    case '\u00EC': // ì
                    case '\u00ED': // í
                    case '\u00EE': // î
                    case '\u00EF': // ï
                        output[outputPos++] = 'i';
                        break;
                    case '\u00F0': // ð
                        output[outputPos++] = 'd';
                        break;
                    case '\u00F1': // ñ
                        output[outputPos++] = 'n';
                        break;
                    case '\u00F2': // ò
                    case '\u00F3': // ó
                    case '\u00F4': // ô
                    case '\u00F5': // õ
                    case '\u00F6': // ö

                    case '\u00F8': // ø
                        output[outputPos++] = 'o';
                        break;
                    case '\u0153': // œ
                        output[outputPos++] = 'o';
                        output[outputPos++] = 'e';
                        break;
                    case '\u00DF': // ß
                        output[outputPos++] = 's';
                        output[outputPos++] = 's';
                        break;
                    case '\u00FE': // þ
                        output[outputPos++] = 't';
                        output[outputPos++] = 'h';
                        break;
                    case '\u00F9': // ù
                    case '\u00FA': // ú
                    case '\u00FB': // û
                    case '\u00FC': // ü
                        output[outputPos++] = 'u';
                        break;
                    case '\u00FD': // ý
                    case '\u00FF': // ÿ
                        output[outputPos++] = 'y';
                        break;
                    default:
                        output[outputPos++] = c;
                        break;
                }
            }
        }

        // now take only the populated chars from output
        char[] outputChars = new char[outputPos];
        System.arraycopy(output, 0, outputChars, 0, outputPos);
        return new String(outputChars);
    }
}
