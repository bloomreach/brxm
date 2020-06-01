/*
 * Copyright 2014-2016 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.components.utils.query;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.lang.math.NumberUtils;
import org.hippoecm.hst.component.support.bean.BaseHstComponent;
import org.hippoecm.hst.content.beans.manager.ObjectConverter;
import org.hippoecm.hst.content.beans.query.HstQuery;
import org.hippoecm.hst.content.beans.query.HstQueryManager;
import org.hippoecm.hst.content.beans.query.exceptions.QueryException;
import org.hippoecm.hst.content.beans.query.filter.Filter;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.onehippo.cms7.essentials.components.utils.SiteUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * QueryBuilder implementation for easier construction of HstQuery objects.
 *
 * @deprecated Deprecated since version since 3.1.1 in favor of the HST fluent API's HstQueryBuilder
 * @see org.hippoecm.hst.content.beans.query.builder.HstQueryBuilder
 */
@Deprecated
public class HstQueryBuilder implements QueryBuilder {

    private static Logger log = LoggerFactory.getLogger(HstQueryBuilder.class);
    private final HstRequest request;
    private final BaseHstComponent component;
    private HippoBean scope;
    private List<Filter> filters;
    private List<Class<? extends HippoBean>> mappings;
    private int size = SiteUtils.DEFAULT_PAGE_SIZE;
    private int page = SiteUtils.DEFAULT_PAGE;
    private boolean includeSubtypes;

    public HstQueryBuilder(final BaseHstComponent component, final HstRequest request) {
        this.request = request;
        this.component = component;
    }

    @Override
    @Nonnull
    public HstQueryBuilder addFilter(final Filter filter) {
        if (filters == null) {
            filters = new ArrayList<>();
        }
        filters.add(filter);
        return this;
    }

    @Override
    @Nonnull
    public HstQueryBuilder scope(final HippoBean scope) {
        this.scope = scope;
        return this;
    }

    @Override
    @Nonnull
    public HstQueryBuilder pageParam(final String parameterName) {
        String value = SiteUtils.getAnyParameter(parameterName, request, component);
        page = NumberUtils.toInt(value, SiteUtils.DEFAULT_PAGE);
        return this;
    }

    @Override
    @Nonnull
    public HstQueryBuilder sizeParam(final String parameterName) {
        String value = SiteUtils.getAnyParameter(parameterName, request, component);
        size = NumberUtils.toInt(value, SiteUtils.DEFAULT_PAGE_SIZE);
        return this;
    }

    @Override
    @Nonnull
    public HstQueryBuilder page(final int page) {
        this.page = page;
        return this;
    }

    @Override
    @Nonnull
    public HstQueryBuilder size(final int size) {
        this.size = size;
        return this;
    }

    @Override
    @Nonnull
    public HstQueryBuilder siteScope() {
        this.scope = getSiteScope();
        return this;
    }

    @Override
    @Nonnull
    @SafeVarargs
    public final HstQueryBuilder documents(final Class<? extends HippoBean>... beans) {
        if (mappings == null) {
            mappings = new ArrayList<>();
        }

        Collections.addAll(mappings, beans);
        return this;
    }

    @Override
    @Nonnull
    public final HstQueryBuilder documents(final String... primaryNodeTypes) {
        if (mappings == null) {
            mappings = new ArrayList<>();
        }
        final HstRequestContext context = request.getRequestContext();
        final ObjectConverter objectConverter = context.getContentBeansTool().getObjectConverter();
        int typeCounter = 0;
        for (String primaryNodeType : primaryNodeTypes) {
            final Class<? extends HippoBean> clazz = objectConverter.getAnnotatedClassFor(primaryNodeType);
            if (clazz != null) {
                mappings.add(clazz);
                typeCounter++;
            }

        }
        if (typeCounter != primaryNodeTypes.length) {
            log.warn("Couldn't resolve all primary node types through object converter: {}", primaryNodeTypes);
        }

        return this;
    }

    @Override
    @Nonnull
    public final HstQueryBuilder includeSubtypes() {
        this.includeSubtypes = true;
        return this;
    }

    @Override
    @Nonnull
    public final HstQueryBuilder excludeSubtypes() {
        this.includeSubtypes = false;
        return this;
    }

    @Override
    @Nullable
    public HstQuery build() {
        if (scope == null) {
            siteScope();
        }
        final HstRequestContext context = request.getRequestContext();
        final HstQueryManager manager = context.getQueryManager();
        try {
            @SuppressWarnings("unchecked")
            final Class<? extends HippoBean>[] classes = mappings.toArray(new Class[mappings.size()]);
            @SuppressWarnings("unchecked")
            final HstQuery query = manager.createQuery(scope, classes);
            query.setLimit(size);
            query.setOffset(size * (page - 1));
            if (filters != null && filters.size() > 0) {
                final Filter root = query.createFilter();
                for (Filter filter : filters) {
                    root.addAndFilter(filter);
                }
                query.setFilter(root);
            }

            return query;
        } catch (QueryException e) {
            log.error("Error creating HST query", e);
        }
        return null;
    }


    @Nullable
    @Override
    public HippoBean getScope() {
        if (scope == null) {
            scope = getSiteScope();
        }
        return scope;
    }

    @Nullable
    @Override
    public HippoBean getSiteScope() {
        final HstRequestContext context = request.getRequestContext();
        return context.getSiteContentBaseBean();
    }
}
