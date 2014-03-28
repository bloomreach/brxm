/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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
import org.onehippo.cms7.essentials.components.utils.SiteUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HstQuery wrapper
 *
 * @version "$Id$"
 */
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
        this.scope = component.getSiteContentBaseBean(request);
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
        final ObjectConverter objectConverter = component.getObjectConverter();
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
        HstQueryManager manager = component.getQueryManager(request);
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
        return component.getSiteContentBaseBean(request);
    }
}
