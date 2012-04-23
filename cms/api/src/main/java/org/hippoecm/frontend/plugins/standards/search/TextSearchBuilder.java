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
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

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
        boolean hasCriteria = (scope != null);
        if (scope != null) {
            querySb.append(scope);
        }
        for (StringTokenizer st = new StringTokenizer(value, " "); st.hasMoreTokens();) {
            String token = st.nextToken();
            if (token.length() < getMinimalLength()) {
                continue;
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
            if (tb.length() < getMinimalLength()) {
                continue;
            }
            if (hasCriteria) {
                querySb.append(" and ");
            } else {
                hasCriteria = true;
            }
            querySb.append("jcr:contains(., '");
            querySb.append(tb);
            valid = true;
            if (wildcardSearch) {
                querySb.append('*');
            }
            querySb.append("')");
        }
        querySb.append(']').append("/rep:excerpt(.)");

        if (!valid) {
            return null;
        }
        return querySb;
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
                    String uuid = content.getUUID();
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
