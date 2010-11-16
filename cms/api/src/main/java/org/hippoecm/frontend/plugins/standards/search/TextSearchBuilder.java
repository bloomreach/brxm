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

import java.util.StringTokenizer;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.query.QueryResult;

import org.apache.wicket.IClusterable;
import org.apache.wicket.Session;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.plugins.standards.browse.BrowserSearchResult;
import org.hippoecm.frontend.session.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TextSearchBuilder implements IClusterable {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(TextSearchBuilder.class);

    public static final String TEXT_QUERY_NAME = "text";

    private String text;
    private String[] scope;

    private String[] excludedPrimaryTypes = {};
    private boolean wildcardSearch = false;
    private String ignoredChars = "*?";
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

    public void setExcludedPrimaryTypes(String[] excludedPrimaryTypes) {
        this.excludedPrimaryTypes = excludedPrimaryTypes;
    }

    public void setWildcardSearch(boolean wildcardSearch) {
        this.wildcardSearch = wildcardSearch;
    }

    public void setIgnoredChars(String ignoredChars) {
        this.ignoredChars = ignoredChars;
    }

    public void setText(String value) {
        this.text = value;
    }

    public TextSearchResultModel getResultModel() {
        String value = text.trim();
        if (value.equals("")) {
            return null;
        }

        boolean valid = false;
        StringBuilder querySb = new StringBuilder("//element(*, hippo:harddocument)[");
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
                        tb.append('\'');
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
            querySb.append(tb.toString());
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
        
        final String query = querySb.toString();
        IModel<QueryResult> resultModel = new QueryResultModel(query, limit);
        return new TextSearchResultModel(value, new BrowserSearchResult(TEXT_QUERY_NAME, resultModel));
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
                } else
                    haveTypeRestriction = true;
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
                    log.info("Skipping non-referenceable node at path" + path);
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

    public void setMinimalLength(int minimalLength) {
        this.minimalLength = minimalLength;
    }

    public int getMinimalLength() {
        return minimalLength;
    }

}
