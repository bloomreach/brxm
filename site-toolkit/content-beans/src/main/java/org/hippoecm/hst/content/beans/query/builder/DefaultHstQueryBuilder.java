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
        HstQuery hstQuery = getHstQueryManager(session).createQuery(getScope());

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

    private HippoBean getScope() throws QueryException {
        HippoBean scope = scope();

        if (scope == null) {
            final HstRequestContext requestContext = RequestContextProvider.get();

            if (requestContext != null) {
                scope = requestContext.getSiteContentBaseBean();
            }
        }

        if (scope == null) {
            throw new QueryException("scope is unavailable.");
        }

        return scope;
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
