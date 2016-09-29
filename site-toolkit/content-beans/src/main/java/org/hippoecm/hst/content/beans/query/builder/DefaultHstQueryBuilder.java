/*
 *  Copyright 2016 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.content.beans.query.builder;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Session;

import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.content.beans.query.HstQuery;
import org.hippoecm.hst.content.beans.query.HstQueryManager;
import org.hippoecm.hst.content.beans.query.exceptions.QueryException;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.request.HstRequestContext;

class DefaultHstQueryBuilder extends HstQueryBuilder {

    private HstQueryManager queryManager;

    protected DefaultHstQueryBuilder() {
        super();
    }

    protected DefaultHstQueryBuilder(final HstQueryManager queryManager) {
        super();
        this.queryManager = queryManager;
    }

    @Override
    public HstQuery build(final Session session) throws QueryException {
        final HstQuery hstQuery;

        final Node [] scopes = getScopes();

        if (scopes == null || scopes.length == 0) {
            throw new QueryException("Empty scopes.");
        }

        final String [] primaryNodeTypes = getPrimaryNodeTypes();
        final Class<? extends HippoBean> [] filterBeanTyes = getFilterBeanTypes();

        if (scopes.length == 1) {
            if (primaryNodeTypes != null && primaryNodeTypes.length > 0) {
                hstQuery = getHstQueryManager(session).createQuery(scopes[0], includeSubTypes(), primaryNodeTypes);
            } else if (filterBeanTyes != null && filterBeanTyes.length > 0) {
                hstQuery = getHstQueryManager(session).createQuery(scopes[0], includeSubTypes(), filterBeanTyes);
            } else {
                hstQuery = getHstQueryManager(session).createQuery(scopes[0]);
            }
        } else {
            if (primaryNodeTypes != null && primaryNodeTypes.length > 0) {
                hstQuery = getHstQueryManager(session).createQuery((Node) null, includeSubTypes(), primaryNodeTypes);
            } else if (filterBeanTyes != null && filterBeanTyes.length > 0) {
                hstQuery = getHstQueryManager(session).createQuery((Node) null, includeSubTypes(), filterBeanTyes);
            } else {
                hstQuery = getHstQueryManager(session).createQuery((Node) null);
            }

            hstQuery.addScopes(scopes);
        }

        final Node [] excludeScopes = getExcludeScopes();

        if (excludeScopes != null && excludeScopes.length > 0) {
            hstQuery.excludeScopes(excludeScopes);
        }

        if (filter() != null) {
            hstQuery.setFilter(filter().build(this, session));
        }

        if (orderByConstructs() != null) {
            for (OrderByConstruct orderBy : orderByConstructs()) {
                if (orderBy.ascending()) {
                    if (orderBy.caseSensitive()) {
                        hstQuery.addOrderByAscending(orderBy.fieldName());
                    } else {
                        hstQuery.addOrderByAscendingCaseInsensitive(orderBy.fieldName());
                    }
                } else {
                    if (orderBy.caseSensitive()) {
                        hstQuery.addOrderByDescending(orderBy.fieldName());
                    } else {
                        hstQuery.addOrderByDescendingCaseInsensitive(orderBy.fieldName());
                    }
                }
            }
        }

        if (offset() != null) {
            hstQuery.setOffset(offset());
        }

        if (limit() != null) {
            hstQuery.setLimit(limit());
        }

        return hstQuery;
    }

    private Node [] getScopes() throws QueryException {
        Node [] scopes = null;

        List<Node> scopesList = scopes();

        if (scopesList == null || scopesList.isEmpty()) {
            final HstRequestContext requestContext = RequestContextProvider.get();

            if (requestContext != null) {
                if (scopesList == null) {
                    scopesList = new ArrayList<>();
                }

                scopesList.add(requestContext.getSiteContentBaseBean().getNode());
            }
        }

        if (scopesList != null) {
            scopes = scopesList.toArray(new Node[scopesList.size()]);
        }

        return scopes;
    }

    private Node [] getExcludeScopes() throws QueryException {
        Node [] excludeScopes = null;

        List<Node> excludeScopesList = excludeScopes();

        if (excludeScopesList != null) {
            excludeScopes = excludeScopesList.toArray(new Node[excludeScopesList.size()]);
        }

        return excludeScopes;
    }

    private String [] getPrimaryNodeTypes() {
        String [] primaryNodeTypes = null;

        List<String> primaryNodeTypeList = primaryNodeTypes();

        if (primaryNodeTypeList != null && !primaryNodeTypeList.isEmpty()) {
            primaryNodeTypes = primaryNodeTypeList.toArray(new String[primaryNodeTypeList.size()]);
        }

        return primaryNodeTypes;
    }

    private Class<? extends HippoBean> [] getFilterBeanTypes() {
        Class<? extends HippoBean> [] filterBeanTypes = null;

        List<Class<? extends HippoBean>> filterBeanTypeList = filterBeanTypes();

        if (filterBeanTypeList != null && !filterBeanTypeList.isEmpty()) {
            filterBeanTypes = filterBeanTypeList.toArray(new Class[filterBeanTypeList.size()]);
        }

        return filterBeanTypes;
    }

    private HstQueryManager getHstQueryManager(final Session session) throws QueryException {
        HstQueryManager qm = queryManager;

        if (qm == null) {
            final HstRequestContext requestContext = RequestContextProvider.get();

            if (requestContext != null) {
                if (session == null) {
                    qm = requestContext.getQueryManager();
                } else {
                    qm = requestContext.getQueryManager(session);
                }
            }
        }

        if (qm == null) {
            throw new QueryException("HstQueryManager is unavailable.");
        }

        return qm;
    }
}
