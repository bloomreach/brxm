/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 */

package org.onehippo.cms7.essentials.site.components.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.hst.content.beans.manager.ObjectConverter;
import org.hippoecm.hst.content.beans.query.HstQuery;
import org.hippoecm.hst.content.beans.query.HstQueryResult;
import org.hippoecm.hst.content.beans.query.exceptions.QueryException;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoDocumentIterator;
import org.hippoecm.hst.content.beans.standard.HippoFacetChildNavigationBean;
import org.hippoecm.hst.content.beans.standard.HippoFacetNavigationBean;
import org.hippoecm.hst.content.beans.standard.HippoFolder;
import org.hippoecm.hst.content.beans.standard.HippoResultSetBean;
import org.hippoecm.hst.content.beans.standard.facetnavigation.HippoFacetNavigation;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.util.SearchInputParsingUtils;
import org.hippoecm.hst.utils.BeanUtils;
import org.onehippo.cms7.essentials.site.beans.BaseDocument;
import org.onehippo.cms7.essentials.site.components.BaseComponent;
import org.onehippo.cms7.essentials.site.components.service.ctx.SearchContext;
import org.onehippo.cms7.essentials.site.components.service.filter.QueryFilter;
import org.onehippo.cms7.essentials.site.components.service.filter.TextSearchFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

/**
 * @version "$Id: SearchService.java 157405 2013-03-08 09:15:49Z mmilicevic $"
 */
public class SearchService<T extends BaseDocument> {

    private static Logger log = LoggerFactory.getLogger(SearchService.class);
    private final List<QueryFilter> queryFilters;
    private final int currentPage;
    private final int pageSize;
    private SearchContext context;
    private Builder<?> builder;


    private SearchService(Builder<?> builder, final List<QueryFilter> filters, int currentPage, int pageSize) {
        this.builder = builder;
        this.context = builder.context;
        this.queryFilters = filters;
        this.currentPage = currentPage;
        this.pageSize = pageSize;
    }

    public HstQuery getQuery(final boolean globalScope) {
        HstQuery query = createQuery(globalScope);
        for (QueryFilter filter : queryFilters) {
            filter.apply(query, context);
        }
        return query;
    }

    @SuppressWarnings("unchecked")
    public SearchCollection<T> executeCollection() {

        try {
            if (isFacetedScope(context)) {
                HstQuery query = getQuery(true);
                applySortedByAndLimit(query);
                final HstRequest request = context.getRequest();
                final ObjectConverter objectConverter = context.getComponent().getObjectConverter();
                boolean isFolderScope = context.getScope() instanceof HippoFolder;
                HippoFacetNavigationBean facNavBean;
                if (isFolderScope) {
                    facNavBean = context.getFacetedScope();
                } else {
                    facNavBean = context.getScope();
                }

                if (facNavBean == null) {
                    facNavBean = BeanUtils.getFacetNavigationBean(request, query, objectConverter);
                }


                if (facNavBean == null) {
                    return new SearchCollection<T>(Collections.<T>emptyList(), currentPage, pageSize);
                } else {
                    final HippoResultSetBean result = facNavBean.getResultSet();
                    final HippoDocumentIterator<? extends BaseDocument> beans = result.getDocumentIterator(context.getBeanMappingClass());
                    if (query.getOffset() > 0) {
                        beans.skip(query.getOffset());
                    }
                    final int results = facNavBean.getCount().intValue();
                    return new SearchCollection<T>((HippoDocumentIterator<T>) beans, results, pageSize, currentPage);
                }
            } else {
                HstQuery query = getQuery(false);
                applySortedByAndLimit(query);
                final HstQueryResult result = query.execute();
                return new SearchCollection<T>(result, currentPage, pageSize);
            }

        } catch (QueryException e) {
            log.error("Error executing geo search", e);
        }
        return SearchCollection.emptyCollection();
    }

    public int getPage() {
        return builder.currentPage;
    }

    public int getSize() {
        return builder.pageSize;
    }

    public String getParsedQuery() {
        return builder.parsedQuery;
    }

    public String getOrder() {
        return builder.orderBy;
    }

    @SuppressWarnings("unchecked")
    private HstQuery createQuery(boolean globalScope) {
        try {
            Class<? extends HippoBean>[] filterBeans = context.getFilterBeans();
            Node scope = null;
            if (globalScope) {
                try {
                    Session session = context.getRequest().getRequestContext().getSession();
                    scope = session.getNode("/content/documents");

                } catch (RepositoryException e) {
                    log.error("Error fetching facet scope", e);
                }
            } else {
                scope = context.getScope().getNode();
            }

            HstQuery q;
            if (filterBeans.length == 1) {
                if (context.getBeanMappingClass() != null) {
                    q = context.getComponent().getQueryManager(context.getRequest()).createQuery(scope, context.getBeanMappingClass(), context.isIncludeSubtypeBeans());
                } else {
                    q = context.getComponent().getQueryManager(context.getRequest()).createQuery(scope, filterBeans[0], context.isIncludeSubtypeBeans());
                }
            } else {
                q = context.getComponent().getQueryManager(context.getRequest()).createQuery(scope, filterBeans);
            }

            return q;
        } catch (QueryException e) {
            log.error("Error creating query", e);
        }
        return null;
    }

    private void applySortedByAndLimit(HstQuery query) {
        String sortBy = "hippoplugins:publicationdate";
        if (context.isAscending()) {
            query.addOrderByAscending(sortBy);
        } else {
            query.addOrderByDescending(sortBy);
        }
        setLimit(query, pageSize, currentPage);
    }

    private boolean isFacetedScope(SearchContext context) {
        HippoBean scope = context.getScope();
        if (scope instanceof HippoFacetNavigationBean) {
            return true;
        }
        final HippoBean contentBean = context.getComponent().getContentBean(context.getRequest());
        if (contentBean == null) {
            return false;
        }
        return contentBean instanceof HippoFacetChildNavigationBean || contentBean instanceof HippoFacetNavigation;
    }

    private void setLimit(final HstQuery query, final int pageSize, final int currentPage) {
        query.setLimit(pageSize * currentPage);
        query.setOffset(pageSize * (currentPage - 1));
    }

    public static class Builder<T extends BaseDocument> {
        private final SearchContext context;
        private final List<QueryFilter> filters = new ArrayList<QueryFilter>();
        private int currentPage = 1;
        private int pageSize = 10;
        private String orderBy;
        private String parsedQuery;
        private boolean populateAttributes;

        public Builder(final SearchContext context) {
            this.context = context;
        }

        public Builder<T> paging(int page, int size) {
            this.currentPage = page;
            this.pageSize = size;
            return this;
        }

        public Builder<T> query(String query) {
            this.parsedQuery = SearchInputParsingUtils.parse(query, false);
            if (!Strings.isNullOrEmpty(query)) {
                filters.add(new TextSearchFilter(query));
            }
            return this;
        }

        public SearchService<T> build() {
            if(populateAttributes){
                final HstRequest request = context.getRequest();
                request.setAttribute("page", currentPage);
                request.setAttribute("size", pageSize);
                request.setAttribute("query", parsedQuery);
                request.setAttribute("order", orderBy);
            }
            return new SearchService<T>(this, filters, currentPage, pageSize);
        }

        public Builder<T> paging(final String pageParameter, final String pageSizeParameter) {
            final BaseComponent component = context.getComponent();
            currentPage = component.getIntParameter(context.getRequest(), pageParameter, 1);
            pageSize = component.getIntParameter(context.getRequest(), pageSizeParameter, 10);
            return this;
        }

        public Builder<T> orderBy(final String order) {
            this.orderBy = order;
            return this;
        }


        public Builder<T> setAttributes() {
            this.populateAttributes = true;
            return this;
        }
    }

}
