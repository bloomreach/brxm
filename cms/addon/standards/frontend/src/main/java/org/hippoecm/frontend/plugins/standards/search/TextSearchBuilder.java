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
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.apache.wicket.IClusterable;
import org.apache.wicket.Session;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.hippoecm.frontend.plugins.standards.browse.BrowserSearchResult;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TextSearchBuilder implements IClusterable {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(TextSearchBuilder.class);

    public static final String TEXT_QUERY_NAME = "text";
    public static final int LIMIT = 100;
    
    private String[] searchPaths;

    private String[] excludedPrimaryTypes = {};
    private boolean wildcardSearch = false;
    private String ignoredChars = "";
    private int limit = -1;

    public TextSearchBuilder(String[] searchPaths) {
        this.searchPaths = searchPaths;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public int getLimit() {
        return limit;
    }

    public void setExcludedPrimaryTypes(String[] excludedPrimaryTypes) {
        this.excludedPrimaryTypes = excludedPrimaryTypes;
    }

    public String[] getExcludedPrimaryTypes() {
        return excludedPrimaryTypes;
    }

    public void setWildcardSearch(boolean wildcardSearch) {
        this.wildcardSearch = wildcardSearch;
    }

    public boolean isWildcardSearch() {
        return wildcardSearch;
    }

    public void setIgnoredChars(String ignoredChars) {
        this.ignoredChars = ignoredChars;
    }

    public String getIgnoredChars() {
        return ignoredChars;
    }

    public TextSearchResultModel search(String value) {
        value = value.trim();
        if (value.equals("")) {
            return null;
        }

        final StringBuilder querySb = new StringBuilder(getScope());
        for (StringTokenizer st = new StringTokenizer(value, " "); st.hasMoreTokens();) {
            querySb.append(" and jcr:contains(., '");
            String token = st.nextToken();
            for (int i = 0; i < token.length(); i++) {
                char c = token.charAt(i);
                if (getIgnoredChars().indexOf(c) == -1) {
                    if (c == '\'') {
                        querySb.append('\'');
                    }
                    querySb.append(c);
                }
            }
            if (isWildcardSearch()) {
                querySb.append('*');
            }
            querySb.append("')");
        }
        querySb.append(']').append("/rep:excerpt(.)");
        IModel<QueryResult> resultModel = new LoadableDetachableModel<QueryResult>() {
            private static final long serialVersionUID = 1L;

            @Override
            protected QueryResult load() {
                QueryResult result = null;
                javax.jcr.Session session = ((UserSession) Session.get()).getJcrSession();
                try {
                    QueryManager queryManager = ((UserSession) Session.get()).getQueryManager();
                    HippoQuery hippoQuery = (HippoQuery) queryManager.createQuery(querySb.toString(), "xpath");
                    session.refresh(true);
                    if (limit > 0 && limit < LIMIT) {
                        hippoQuery.setLimit(limit);
                    } else {
                        hippoQuery.setLimit(LIMIT);
                    }

                    long start = System.currentTimeMillis();
                    result = hippoQuery.execute();
                    long end = System.currentTimeMillis();
                    log.info("Executing search query: " + TEXT_QUERY_NAME + " took " + (end - start) + "ms");
                } catch (RepositoryException e) {
                    log.error("Error executing query[" + TEXT_QUERY_NAME + "]", e);
                }
                return result;
            }
        };
        return new TextSearchResultModel(value, new BrowserSearchResult(TEXT_QUERY_NAME, resultModel));
    }

    private String getScope() {
        javax.jcr.Session session = ((UserSession) Session.get()).getJcrSession();

        StringBuilder sb = new StringBuilder("//element(*, hippo:harddocument)[");
        if (excludedPrimaryTypes.length > 0) {
            sb.append("not(");
            boolean addOr = false;
            for (String exclude : excludedPrimaryTypes) {
                if (addOr) {
                    sb.append(" or ");
                } else
                    addOr = true;
                sb.append("@jcr:primaryType='").append(exclude).append('\'');
            }
            sb.append(") and ");
        }
        sb.append("(");

        boolean addOr = false;
        for (String path : searchPaths) {
            if (!path.startsWith("/")) {
                throw new IllegalArgumentException("Search path should be absolute: " + path);
            }
            try {
                Node content = (Node) session.getItem(path);
                NodeIterator ni = content.getNodes();
                while (ni.hasNext()) {
                    Node cn = ni.nextNode();
                    if (cn.isNodeType("mix:referenceable")) {
                        String uuid = cn.getUUID();
                        if (uuid != null) {
                            if (addOr) {
                                sb.append(" or ");
                            } else {
                                addOr = true;
                            }
                            sb.append("hippo:paths = '").append(uuid).append('\'');
                        }
                    }
                }
            } catch (PathNotFoundException e) {
                log.warn("Search path not found: " + path);
            } catch (RepositoryException e) {
                throw new IllegalStateException(
                        "An error occured while constructing the default search where-clause part", e);
            }
        }
        sb.append(')');
        return sb.toString();
    }

}