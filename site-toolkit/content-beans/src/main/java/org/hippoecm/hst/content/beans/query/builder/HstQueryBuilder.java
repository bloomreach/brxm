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

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.content.beans.query.HstQuery;
import org.hippoecm.hst.content.beans.query.HstQueryManager;
import org.hippoecm.hst.content.beans.query.exceptions.QueryException;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.repository.util.DateTools;

public abstract class HstQueryBuilder {

    /*
     * Static methods to create a query builder or filter builder.
     */

    public static HstQueryBuilder create() {
        return new DefaultHstQueryBuilder();
    }

    public static HstQueryBuilder create(final HstQueryManager queryManager) {
        return new DefaultHstQueryBuilder(queryManager);
    }

    public static FilterBuilder filter(String fieldName) {
        FieldFilterBuilder filterBuilder = new FieldFilterBuilder(fieldName);
        return filterBuilder;
    }

    public static FilterBuilder and(FilterBuilder ... filterBuilders) {
        AndFilterBuilder filterBuilder = new AndFilterBuilder(filterBuilders);
        return filterBuilder;
    }

    public static FilterBuilder or(FilterBuilder ... filterBuilders) {
        OrFilterBuilder filterBuilder = new OrFilterBuilder(filterBuilders);
        return filterBuilder;
    }

    /*
     * Members of a builder instance.
     */

    private DateTools.Resolution defaultResolution;
    private HippoBean scopeBean;
    private FilterBuilder filterBuilder;
    private List<OrderByConstruct> orderByConstructs;
    private Integer offset;
    private Integer limit;

    protected HstQueryBuilder() {
    }

    public HstQuery build() throws QueryException, RepositoryException {
        final HstRequestContext requestContext = RequestContextProvider.get();
        return build(requestContext.getSession());
    }

    abstract public HstQuery build(final Session session) throws QueryException;

    public HstQueryBuilder defaultResolution(final DateTools.Resolution defaultResolution) {
        this.defaultResolution = defaultResolution;
        return this;
    }

    protected DateTools.Resolution defaultResolution() {
        return defaultResolution;
    }

    public HstQueryBuilder scope(final HippoBean scopeBean) {
        this.scopeBean = scopeBean;
        return this;
    }

    protected HippoBean scope() {
        return scopeBean;
    }

    public HstQueryBuilder filter(final FilterBuilder filterBuilder) {
        this.filterBuilder = filterBuilder;
        return this;
    }

    protected FilterBuilder filter() {
        return filterBuilder;
    }

    public HstQueryBuilder orderByAscending(final String fieldName) {
        OrderByConstruct orderBy = new OrderByConstruct(fieldName, true);
        addOrderByConstruct(orderBy);
        return this;
    }

    public HstQueryBuilder orderByAscendingCaseInsensitive(final String fieldName) {
        OrderByConstruct orderBy = new OrderByConstruct(fieldName, true).caseSensitive(false);
        addOrderByConstruct(orderBy);
        return this;
    }

    public HstQueryBuilder orderByDescending(final String fieldName) {
        OrderByConstruct orderBy = new OrderByConstruct(fieldName, false);
        addOrderByConstruct(orderBy);
        return this;
    }

    public HstQueryBuilder orderByDescendingCaseInsensitive(final String fieldName) {
        OrderByConstruct orderBy = new OrderByConstruct(fieldName, false).caseSensitive(false);
        addOrderByConstruct(orderBy);
        return this;
    }

    protected List<OrderByConstruct> orderByConstructs() {
        return orderByConstructs;
    }

    public HstQueryBuilder offset(int offset) {
        this.offset = offset;
        return this;
    }

    public Integer offset() {
        return offset;
    }

    public HstQueryBuilder limit(int limit) {
        this.limit = limit;
        return this;
    }

    public Integer limit() {
        return limit;
    }

    private void addOrderByConstruct(final OrderByConstruct orderBy) {
        if (orderByConstructs == null) {
            orderByConstructs = new ArrayList<>();
        }

        orderByConstructs.add(orderBy);
    }
}
