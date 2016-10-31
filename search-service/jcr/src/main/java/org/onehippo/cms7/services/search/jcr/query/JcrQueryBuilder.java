/*
 * Copyright 2012-2016 Hippo B.V. (http://www.onehippo.com)
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

import java.util.ArrayList;
import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JcrQueryBuilder {

    static final Logger log = LoggerFactory.getLogger(JcrQueryBuilder.class);

    static final int DEFAULT_LIMIT = 1000;

    private boolean returnParentNode = false;
    private int limit = DEFAULT_LIMIT;
    private int offset = 0;

    private final Session session;

    private List<String> scopes = new ArrayList<String>();
    private List<String> excludeScopes = new ArrayList<String>();
    private List<String> orderByList = new ArrayList<String>();

    private final boolean wildcardPostfixEnabled;
    private final int wildcardPostfixMinLength;

    private Filter filter;

    private String nodeType;
    private List<String> selected = new ArrayList<String>();

    public JcrQueryBuilder(final Session session, final boolean wildcardPostfixEnabled, final int wildcardPostfixMinLength) {
        this.session = session;
        this.wildcardPostfixEnabled = wildcardPostfixEnabled;
        this.wildcardPostfixMinLength = wildcardPostfixMinLength;
    }

    public String getNodeType() {
        return nodeType;
    }

    public void setNodeType(final String nodeType) {
        this.nodeType = nodeType;
    }

    public void addScope(String path) {
        String identifier;
        try {
            identifier = session.getNode(path).getIdentifier();
        } catch (RepositoryException e) {
            log.error("Unable to translate path " + path + " into identifier", e);
            return;
        }
        this.scopes.add(identifier);
    }

    public void addExclusion(final String path) {
        String identifier;
        try {
            identifier = session.getNode(path).getIdentifier();
        } catch (RepositoryException e) {
            log.error("Unable to translate path " + path + " into identifier", e);
            return;
        }
        this.excludeScopes.add(identifier);
    }

    public void addOrderBy(final String orderClause) {
        this.orderByList.add(orderClause);
    }

    public void setFilter(Filter filter) {
        this.filter = filter;
    }

    public String getQueryString() {
        StringBuilder query = new StringBuilder(256);

        // get the list of scope id's to search below:
        boolean hasConstraints = false;
        String scopesWhereClause = getScopesWhereClause();
        if (scopesWhereClause.length() > 0) {
            query.append("(").append(scopesWhereClause.toString()).append(")");
            hasConstraints = true;
        }

        if (this.excludeScopes != null && !this.excludeScopes.isEmpty()) {
            StringBuilder excludeExpr = getExcludedWhereClause();
            if (excludeExpr.length() > 0) {
                if (!hasConstraints) {
                    query.append("(").append(excludeExpr.toString()).append(")");
                } else {
                    query.append(" and (").append(excludeExpr.toString()).append(")");
                }
                hasConstraints = true;
            }
        }

        String jcrExpression;
        if(this.filter != null && (jcrExpression = this.filter.getJcrExpression()) != null) {
            if(!hasConstraints) {
                query.append(jcrExpression);
            } else {
                query.append(" and (").append(jcrExpression).append(")");
            }
            hasConstraints = true;
        }

        if (hasConstraints) {
            query.insert(0, '[');
            query.append("]");
        }

        if (this.nodeType != null) {
            query.insert(0, ")").insert(0, this.nodeType).insert(0, "//element(*,");
        } else {
            query.insert(0, "//*");
        }

        if (this.returnParentNode) {
            query.append("/..");
        }

        if (!this.selected.isEmpty()) {
            boolean first = true;
            for (String property : selected) {
                try {
                    String asXpath = JcrQueryUtils.toXPathProperty(property, true, "select");
                    if (first) {
                        first = false;
                        query.append("/(");
                    } else {
                        query.append(" | ");
                    }
                    query.append(asXpath);
                } catch (JcrQueryException e) {
                    log.error("Unable to build xpath representation for '" + property + "', it will not be available in the search results", e);
                }
            }
            if (!first) {
                query.append(')');
            }
        }

        if (orderByList.size() > 0) {
            query.append(" order by ");
            boolean first = true;
            for (String orderBy : orderByList) {
                if (!first) {
                    query.append(",");
                }
                query.append(orderBy);
                first = false;
            }
        } else {
            // default order is by score descending
            query.append(" order by @jcr:score descending");
        }
        return query.toString();
    }

    private StringBuilder getExcludedWhereClause() {
        StringBuilder excludeExpr = new StringBuilder(80);
        for (String scopeUUID : this.excludeScopes) {
            if (excludeExpr.length() > 0) {
                excludeExpr.append(" and ");
            }
            // do not use a!=b but not(a=b) as this is different for multivalued properties in jcr!
            excludeExpr.append("not(@").append(HippoNodeType.HIPPO_PATHS).append("='").append(scopeUUID).append(
                    "')");
        }
        return excludeExpr;
    }

    private String getScopesWhereClause() {
        StringBuilder scopesWhereClause = new StringBuilder();
        for (String identifier : scopes) {
            if (scopesWhereClause.length() > 0) {
                scopesWhereClause.append(" or ");
            }
            scopesWhereClause.append("@").append(HippoNodeType.HIPPO_PATHS).append("='").append(identifier).append("'");
        }
        return scopesWhereClause.toString();
    }

    public void addSelect(final String property) {
        this.selected.add(property);
    }

    public boolean isReturnParentNode() {
        return returnParentNode;
    }

    public void setReturnParentNode() {
        this.returnParentNode = true;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(final int offset) {
        this.offset = offset;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(final int limit) {
        this.limit = limit;
    }

    public boolean isWildcardPostfixEnabled() {
        return wildcardPostfixEnabled;
    }

    public int getWildcardPostfixMinLength() {
        return wildcardPostfixMinLength;
    }
}
