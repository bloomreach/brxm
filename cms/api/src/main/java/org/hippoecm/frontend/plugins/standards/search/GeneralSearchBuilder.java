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
package org.hippoecm.frontend.plugins.standards.search;

import java.util.Arrays;
import java.util.StringTokenizer;

import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.QueryResult;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.plugins.standards.browse.BrowserSearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.stream.Collectors.joining;

public class GeneralSearchBuilder {

    private static final Logger log = LoggerFactory.getLogger(GeneralSearchBuilder.class);

    private static final String DEFAULT_IGNORED_CHARS = "&|!(){}[]^\"~*?:\\";
    static final char MINUS_SIGN = '-';

    private String text = StringUtils.EMPTY;
    private String ignoredChars = DEFAULT_IGNORED_CHARS;
    private String[] scope;
    private String[] includePrimaryTypes;
    private String[] excludedPrimaryTypes = ArrayUtils.EMPTY_STRING_ARRAY;
    private boolean wildcardSearch;
    private int limit = -1;
    private int minimalLength = 3;
    private final String queryName;

    public static final String TEXT_QUERY_NAME = "text";

    public GeneralSearchBuilder() {
        this(TEXT_QUERY_NAME);
    }

    public GeneralSearchBuilder(final String queryName) {
        this.queryName = queryName;
        setScope(new String[]{"/"});
    }

    public void setScope(final String[] paths) {
        for (String path : paths) {
            if (!path.startsWith("/")) {
                throw new IllegalArgumentException("Search path should be absolute: " + path);
            }
        }
        scope = paths;
    }

    public void setLimit(final int limit) {
        this.limit = limit;
    }

    /**
     * Sets the JCR primary types to search for.
     *
     * @param includePrimaryTypes {@link String}[] of primary types
     */
    public void setIncludePrimaryTypes(final String[] includePrimaryTypes) {
        this.includePrimaryTypes = includePrimaryTypes;
    }

    public void setExcludedPrimaryTypes(final String[] excludedPrimaryTypes) {
        this.excludedPrimaryTypes = excludedPrimaryTypes;
    }

    public void setWildcardSearch(final boolean wildcardSearch) {
        this.wildcardSearch = wildcardSearch;
    }

    public void setIgnoredChars(final String ignoredChars) {
        this.ignoredChars = DEFAULT_IGNORED_CHARS + ignoredChars;
    }

    public void setText(final String value) {
        if (value == null) {
            throw new IllegalArgumentException("Text may not be null.");
        }
        text = value.trim();
    }

    public String getText() {
        return text;
    }

    public void setMinimalLength(final int minimalLength) {
        this.minimalLength = minimalLength;
    }

    protected int getMinimalLength() {
        return minimalLength;
    }

    protected String[] getIncludePrimaryTypes() {
        return includePrimaryTypes;
    }

    protected String[] getScope() {
        return scope;
    }

    protected String getIgnoredChars() {
        return ignoredChars;
    }

    protected boolean isWildcardSearch() {
        return wildcardSearch;
    }

    public TextSearchResultModel getResultModel() {
        final String text = getText();
        if (StringUtils.isBlank(text)) {
            return null;
        }

        final StringBuilder queryStringBuilder = getQueryStringBuilder();
        if (queryStringBuilder == null) {
            return null;
        }

        final String query = queryStringBuilder.toString();
        final IModel<QueryResult> resultModel = new QueryResultModel(query, limit);
        return new TextSearchResultModel(text, new BrowserSearchResult(queryName, resultModel), scope);
    }

    /**
     * Makes the JCR Xpath query string
     *
     * @return StringBuilder that represents the JCR Xpath query
     */
    protected StringBuilder getQueryStringBuilder() {
        final String searchTerm = isoLatin1AccentReplacer(getText().trim());
        final StringBuilder queryStringBuilder = new StringBuilder();

        try {
            appendIncludedPrimaryNodeTypeFilter(queryStringBuilder);

            queryStringBuilder.append('[');

            appendExcludedPrimaryNodeTypeFilter(queryStringBuilder);

            appendScope(queryStringBuilder);

            appendContains(queryStringBuilder, searchTerm);

            queryStringBuilder.append(']');

            appendExtraWhereClauses(queryStringBuilder);

            appendOrderByClause(queryStringBuilder);

        } catch (InvalidQueryException e) {
            log.debug("Cannot create a Xpath query for '{}'. Return null.", searchTerm);
            return null;
        }

        log.debug("Xpath query = {} ", queryStringBuilder.toString());
        return queryStringBuilder;
    }

    protected void appendIncludedPrimaryNodeTypeFilter(final StringBuilder sb) {
        if (includePrimaryTypes == null || includePrimaryTypes.length == 0) {
            sb.append("//element()");
        } else {
            sb.append("//element()[");
            sb.append(buildTypesString(includePrimaryTypes));
            sb.append(']');
        }
    }

    protected void appendExcludedPrimaryNodeTypeFilter(final StringBuilder sb) {
        if (excludedPrimaryTypes.length > 0) {
            sb.append("not(");
            sb.append(buildTypesString(excludedPrimaryTypes));
            sb.append(")");
        }
    }

    protected String buildTypesString(final String[] primaryTypes) {
        return Arrays.stream(primaryTypes)
                .map(type -> String.format("@jcr:primaryType='%s'", type))
                .collect(joining(" or "));
    }

    protected void appendScope(final StringBuilder sb) {
    }

    protected void appendContains(final StringBuilder queryStringBuilder, final String searchTerm) throws InvalidQueryException {
        boolean valid = false;

        if (StringUtils.isNotBlank(searchTerm)) {
            if (!queryStringBuilder.toString().endsWith("[")) {
                queryStringBuilder.append(" and ");
            }
            final String whereClause = getWhereClause(searchTerm, false);
            if (isWildcardSearch()) {
                final String whereClauseWildCards = getWhereClause(searchTerm, true);
                if (!whereClauseWildCards.isEmpty()) {
                    valid = true;
                    if (!whereClause.isEmpty()) {
                        queryStringBuilder.append("(");
                        queryStringBuilder.append(formatJcrContains(whereClause));
                        queryStringBuilder.append(" or ");
                        queryStringBuilder.append(formatJcrContains(whereClauseWildCards));
                        queryStringBuilder.append(")");
                    } else {
                        queryStringBuilder.append(formatJcrContains(whereClauseWildCards));
                    }
                } else if (!whereClause.isEmpty()) {
                    valid = true;
                    queryStringBuilder.append(formatJcrContains(whereClause));
                }
            } else if (!whereClause.isEmpty()) {
                valid = true;
                queryStringBuilder.append(formatJcrContains(whereClause));
            }
        }

        if (!valid) {
            throw new InvalidQueryException();
        }
    }

    protected static String formatJcrContains(final String containsTerm) {
        return String.format("jcr:contains(.,'%s')", containsTerm);
    }

    protected String getWhereClause(final String value, final boolean wildcardPostfix) {
        final StringBuilder whereClauseBuilder = new StringBuilder();
        boolean isOperatorToken;
        String peekedToken = null;
        for (final StringTokenizer st = new StringTokenizer(value, " "); st.hasMoreTokens() || peekedToken != null; ) {
            final String token;
            if (peekedToken != null) {
                token = peekedToken;
                peekedToken = null;
            } else {
                token = st.nextToken();
            }
            final StringBuilder tb = new StringBuilder();
            for (int i = 0; i < token.length(); i++) {
                final char c = token.charAt(i);
                if (getIgnoredChars().indexOf(c) == -1) {
                    if (c == '\'') {
                        // According to JCR 1.0 (JSR-170), section 6.6.4.9, the apostrophe(') and quotation mark (")
                        // are escaped by the two adjacent marks, i.e. ('') and ("").
                        tb.append('\'');
                    }
                    if (c == MINUS_SIGN) {
                        // we do not allow minus sign followed by a space or ignored char
                        if (token.length() > i + 1) {
                            final char nextChar = token.charAt(i + 1);
                            if (nextChar == ' ' || getIgnoredChars().indexOf(nextChar) > -1) {
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

            isOperatorToken = "OR".equals(token) || "AND".equals(token);

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
                if ("OR".equals(peekedToken) || "AND".equals(peekedToken)) {
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

    /**
     * Overriding classes can append extra where clauses. Enclose the clause between square braces.
     *
     * @param queryStringBuilder with the partly built up xpath query.
     */
    protected void appendExtraWhereClauses(final StringBuilder queryStringBuilder) {
    }

    protected void appendOrderByClause(final StringBuilder queryStringBuilder) {
        queryStringBuilder.append(" order by @jcr:score descending");
    }

    public static String isoLatin1AccentReplacer(final String input) {
        if (input == null) {
            return null;
        }

        final char[] inputChars = input.toCharArray();
        // Worst-case length required:
        final char[] output = new char[inputChars.length * 2];

        int outputPos = 0;

        int pos = 0;
        final int length = inputChars.length;

        for (int i = 0; i < length; i++, pos++) {
            final char c = inputChars[pos];

            // Quick test: if it's not in range then just keep
            // current character
            if (c < '\u00c0') {
                output[outputPos++] = c;
            } else {
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
        final char[] outputChars = new char[outputPos];
        System.arraycopy(output, 0, outputChars, 0, outputPos);
        return new String(outputChars);
    }

}
