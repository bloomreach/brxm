/*
 *  Copyright 2010 Hippo.
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

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.IClusterable;
import org.apache.wicket.Session;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.plugins.standards.browse.BrowserSearchResult;
import org.hippoecm.frontend.session.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.query.QueryResult;
import java.util.StringTokenizer;

public class TextSearchBuilder implements IClusterable {

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(TextSearchBuilder.class);

    public static final String TEXT_QUERY_NAME = "text";
    
    private static final String DEFAULT_IGNORED_CHARS = "&|!(){}[]^\"~*?:\\";

    private String text;
    private String[] scope;

    private String[] includePrimaryTypes;
    private String[] excludedPrimaryTypes = {};
    private boolean wildcardSearch = false;
    private String ignoredChars = DEFAULT_IGNORED_CHARS;
    private int limit = -1;
    private int minimalLength = 3;

    public TextSearchBuilder() {
        this.scope = new String[] { "/" };
    }

    public void setScope(String[] paths) {
        this.scope = paths;
    }
    
    public void setLimit(int limit) {
        this.limit = limit;
    }

    /**
     * Sets the JCR primary types to search for.
     * @param includePrimaryTypes {@link String}[] of primary types
     */
    public void setIncludePrimaryTypes(final String[] includePrimaryTypes) {
        this.includePrimaryTypes = includePrimaryTypes;
    }

    public void setExcludedPrimaryTypes(String[] excludedPrimaryTypes) {
        this.excludedPrimaryTypes = excludedPrimaryTypes;
    }

    public void setWildcardSearch(boolean wildcardSearch) {
        this.wildcardSearch = wildcardSearch;
    }

    public void setIgnoredChars(String ignoredChars) {
        this.ignoredChars = DEFAULT_IGNORED_CHARS + ignoredChars;
    }

    public void setText(String value) {
        this.text = value;
    }

    public TextSearchResultModel getResultModel() {
        String value = text.trim();
        if (value.equals("")) {
            return null;
        }

        StringBuilder querySb = getQueryStringBuilder();
        if (querySb == null) {
            return null;
        }
        
        final String query = querySb.toString();
        IModel<QueryResult> resultModel = new QueryResultModel(query, limit);
        return new TextSearchResultModel(value, new BrowserSearchResult(TEXT_QUERY_NAME, resultModel));
    }

    /**
     * Makes the JCR Xpath query string
     * @return StringBuilder that represents the JCR Xpath query
     */
    StringBuilder getQueryStringBuilder() {
        String value = text.trim();
        boolean valid = false;
        StringBuilder querySb = new StringBuilder();
        querySb.append(getIncludedPrimaryTypeFilter()).append('[');
        StringBuilder scope = getScope();
        if (scope != null) {
            querySb.append(scope);
        }

        if (!StringUtils.isBlank(value)) {
            if (scope != null) {
                querySb.append(" and ");
            }
            String whereClause = getWhereClause(value, false);
            if (wildcardSearch) {
                String whereClauseWildCards = getWhereClause(value, true);
                if (whereClauseWildCards.length() > 0) {
                    valid = true;
                    if (whereClause.length() > 0) {
                        querySb.append("(");
                        querySb.append("jcr:contains(.,'").append(whereClause).append("')");
                        querySb.append(" or ");
                        querySb.append("jcr:contains(.,'").append(whereClauseWildCards).append("')");
                        querySb.append(")");
                    } else {
                        querySb.append("jcr:contains(.,'").append(whereClauseWildCards).append("')");
                    }
                } else if (whereClause.length() > 0) {
                    valid = true;
                    querySb.append("jcr:contains(.,'").append(whereClause).append("')");
                }
            } else if (whereClause.length() > 0) {
                valid = true;
                querySb.append("jcr:contains(.,'").append(whereClause).append("')");
            }
        }

        querySb.append(']').append("/rep:excerpt(.)");

        if (!valid) {
            log.debug("Cannot create a Xpath query for '{}'. Return null", value);
            return null;
        }
        log.debug("Xpath query = {} ", querySb.toString());
        return querySb;
    }

    private String getWhereClause(final String value, final boolean wildcardPostfix) {
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
                    tb.append(c);
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

    private StringBuilder getScope() {
        javax.jcr.Session session = ((UserSession) Session.get()).getJcrSession();

        StringBuilder sb = new StringBuilder();
        boolean haveTypeRestriction = false;
        if (excludedPrimaryTypes.length > 0) {
            sb.append("not(");
            for (String exclude : excludedPrimaryTypes) {
                if (haveTypeRestriction) {
                    sb.append(" or ");
                } else {
                    haveTypeRestriction = true;
                }
                sb.append("@jcr:primaryType='").append(exclude).append('\'');
            }
            sb.append(")");
        }
        boolean haveScope = false;
        for (String path : scope) {
            if (!path.startsWith("/")) {
                throw new IllegalArgumentException("Search path should be absolute: " + path);
            }
            try {
                Node content = (Node) session.getItem(path);
                if (content.isNodeType("mix:referenceable")) {
                    String uuid = content.getIdentifier();
                    if (haveScope) {
                        sb.append(" or ");
                    } else {
                        if (haveTypeRestriction) {
                            sb.append(" and ");
                        }
                        sb.append("(");
                        haveScope = true;
                    }
                    sb.append("hippo:paths = '").append(uuid).append('\'');
                } else {
                    log.info("Skipping non-referenceable node at path {}", path);
                }
            } catch (PathNotFoundException e) {
                log.warn("Search path not found: " + path);
            } catch (RepositoryException e) {
                throw new IllegalStateException(
                        "An error occured while constructing the default search where-clause part", e);
            }
        }
        if (haveScope) {
            sb.append(')');
        }
        if (haveScope || haveTypeRestriction) {
            return sb;
        }
        return null;
    }

    /**
     * Translates the included primary type(s) to a filter for a JCR xpath query.
     *
     * @return xpath condition with configured document types or a clause that queries {@literal hippo:harddocument} and all its subtypes
     *          if no document type filter is configured
     */
    private StringBuilder getIncludedPrimaryTypeFilter() {
        StringBuilder sb = new StringBuilder();
        if (includePrimaryTypes == null || includePrimaryTypes.length == 0) {
            sb.append("//element(*, hippo:harddocument)");
        } else {
            sb.append("//node()[");

            int i = 0, size = includePrimaryTypes.length;
            while (i < size) {
                if (i > 0) {
                    sb.append(" or ");
                }
                sb.append("@jcr:primaryType='").append(includePrimaryTypes[i]).append('\'');
                i++;
            }
            sb.append(']');
        }
        return sb;
    }

    public void setMinimalLength(int minimalLength) {
        this.minimalLength = minimalLength;
    }

    public int getMinimalLength() {
        return minimalLength;
    }

}
