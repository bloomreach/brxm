/*
 * Copyright 2014-2018 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.components;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.content.beans.query.HstQuery;
import org.hippoecm.hst.content.beans.query.HstQueryResult;
import org.hippoecm.hst.content.beans.query.builder.HstQueryBuilder;
import org.hippoecm.hst.content.beans.query.exceptions.FilterException;
import org.hippoecm.hst.content.beans.query.exceptions.QueryException;
import org.hippoecm.hst.content.beans.query.filter.BaseFilter;
import org.hippoecm.hst.content.beans.query.filter.Filter;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoDocumentIterator;
import org.hippoecm.hst.content.beans.standard.HippoFacetNavigationBean;
import org.hippoecm.hst.content.beans.standard.HippoResultSetBean;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.parameters.ParametersInfo;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.util.ContentBeanUtils;
import org.onehippo.cms7.essentials.components.info.EssentialsListComponentInfo;
import org.onehippo.cms7.essentials.components.info.EssentialsPageable;
import org.onehippo.cms7.essentials.components.info.EssentialsSortable;
import org.onehippo.cms7.essentials.components.paging.DefaultPagination;
import org.onehippo.cms7.essentials.components.paging.Pageable;
import org.onehippo.cms7.essentials.components.utils.SiteUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

/**
 * HST component used for listing of documents.
 *
 */
@ParametersInfo(type = EssentialsListComponentInfo.class)
public class EssentialsListComponent extends CommonComponent {

    private static Logger log = LoggerFactory.getLogger(EssentialsListComponent.class);


    @Override
    public void doBeforeRender(final HstRequest request, final HstResponse response) {
        super.doBeforeRender(request, response);
        final EssentialsListComponentInfo paramInfo = getComponentParametersInfo(request);
        final String path = getScopePath(paramInfo);
        log.debug("Calling EssentialsListComponent for documents path:  [{}]", path);
        final HippoBean scope = getSearchScope(request, path);

        if (scope == null) {
            log.warn("Search scope was null");
            handleInvalidScope(request, response);
            return;
        }

        final Pageable<? extends HippoBean> pageable;
        if (scope instanceof HippoFacetNavigationBean) {
            pageable = doFacetedSearch(request, paramInfo, scope);
        } else {
            pageable = doSearch(request, paramInfo, scope);
        }
        populateRequest(request, paramInfo, pageable);
    }

    /**
     * Populates request with search results
     *
     * @param request   HstRequest
     * @param paramInfo EssentialsPageable instance
     * @param pageable  search results (Pageable<HippoBean>)
     * @see CommonComponent#REQUEST_ATTR_QUERY
     * @see CommonComponent#REQUEST_ATTR_PAGEABLE
     * @see CommonComponent#REQUEST_ATTR_PAGE
     */
    protected void populateRequest(final HstRequest request, final EssentialsPageable paramInfo, final Pageable<? extends HippoBean> pageable) {
        request.setAttribute(REQUEST_ATTR_QUERY, getSearchQuery(request));
        request.setModel(REQUEST_ATTR_PAGEABLE, pageable);
        request.setAttribute(REQUEST_ATTR_PARAM_INFO, paramInfo);

        if (pageable != null) {
            pageable.setShowPagination(isShowPagination(request, paramInfo));
        }
    }

    /**
     * Returns Search scope for given path. If path is null, current scope bean will be used, site wide scope otherwise
     *
     * @param request current HST request. Unused, available when overriding.
     * @param path    path (optional)
     * @return hippo bean or null
     */
    protected HippoBean getSearchScope(final HstRequest request, final String path) {
        final HstRequestContext context = RequestContextProvider.get();
        HippoBean scope = null;

        if (Strings.isNullOrEmpty(path)) {
            scope = context.getContentBean();
        }

        if (scope == null) {
            scope = doGetScopeBean(path);
        }
        return scope;
    }

    /**
     * Apply ordering (if order field name is provided)
     *
     * @param request       instance of  HstRequest
     * @param query         instance of  HstQuery
     * @param componentInfo instance of EssentialsDocumentListComponentInfo
     * @param <T>           component info class.
     */
    protected <T extends EssentialsListComponentInfo> void applyOrdering(final HstRequest request, final HstQuery query, final T componentInfo) {
        final String sortField = componentInfo.getSortField();
        if (Strings.isNullOrEmpty(sortField)) {
            return;
        }
        final String sortOrder = Strings.isNullOrEmpty(componentInfo.getSortOrder()) ? EssentialsSortable.DESC : componentInfo.getSortOrder();
        if (sortOrder.equals(EssentialsSortable.DESC)) {
            query.addOrderByDescending(sortField);
        } else {
            query.addOrderByAscending(sortField);
        }
    }

    protected <T extends EssentialsListComponentInfo>
    Pageable<? extends HippoBean> doSearch(final HstRequest request, final T paramInfo, final HippoBean scope) {
        try {
            final HstQuery build = buildQuery(request, paramInfo, scope);
            if (build != null) {
                return executeQuery(request, paramInfo, build);
            }
        } catch (QueryException e) {
            log.error("Error running query", e.getMessage());
            log.debug("Query exception: ", e);
        }
        return null;
    }

    /**
     * Execute the search given a facet navigation scope.
     *
     * @param request   current HST request
     * @param paramInfo component parameters
     * @param scope     bean representing search scope
     * @param <T>       type of component info interface
     * @return pageable search results, or null if search failed.
     */
    protected <T extends EssentialsListComponentInfo>
    Pageable<HippoBean> doFacetedSearch(final HstRequest request, final T paramInfo, final HippoBean scope) {

        Pageable<HippoBean> pageable = DefaultPagination.emptyCollection();
        final String relPath = SiteUtils.relativePathFrom(scope, request.getRequestContext());
        final HippoFacetNavigationBean facetBean = ContentBeanUtils.getFacetNavigationBean(relPath, getSearchQuery(request));
        if (facetBean != null) {
            final HippoResultSetBean resultSet = facetBean.getResultSet();
            if (resultSet != null) {
                final HippoDocumentIterator<HippoBean> iterator = resultSet.getDocumentIterator(HippoBean.class);
                pageable = getPageableFactory().createPageable(iterator, resultSet.getCount().intValue(), paramInfo.getPageSize(), getCurrentPage(request));
            }
        }
        return pageable;
    }

    protected void handleInvalidScope(final HstRequest request, final HstResponse response) {
        // TODO determine what to do with invalid scope
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        if (log.isDebugEnabled()) {
            throw new HstComponentException("EssentialsListComponent needs a valid scope to display documents");
        }
    }

    /**
     * Build the HST query for the list.
     *
     * @param request   the current request
     * @param paramInfo the parameter info
     * @param scope     the scope of the query
     * @return the HST query to execute
     */
    protected <T extends EssentialsListComponentInfo>
    HstQuery buildQuery(final HstRequest request, final T paramInfo, final HippoBean scope) {
        final String documentTypes = paramInfo.getDocumentTypes();
        final String[] types = SiteUtils.parseCommaSeparatedValue(documentTypes);
        if (log.isDebugEnabled()) {
            log.debug("Searching for document types:  {}, and including subtypes: {}", documentTypes, paramInfo.getIncludeSubtypes());
        }

        HstQueryBuilder builder = HstQueryBuilder.create(scope);
        return paramInfo.getIncludeSubtypes() ? builder.ofTypes(types).build() : builder.ofPrimaryTypes(types).build();
    }

    /**
     * Execute the list query.
     *
     * @param request   the current request
     * @param paramInfo the parameter info
     * @param query     the query to execute
     * @return the pageable result
     * @throws QueryException query exception when query fails
     */
    protected <T extends EssentialsListComponentInfo>
    Pageable<HippoBean> executeQuery(final HstRequest request, final T paramInfo, final HstQuery query) throws QueryException {
        final int pageSize = getPageSize(request, paramInfo);
        final int page = getCurrentPage(request);
        query.setLimit(pageSize);
        query.setOffset((page - 1) * pageSize);
        applyOrdering(request, query, paramInfo);
        applyExcludeScopes(request, query, paramInfo);
        buildAndApplyFilters(request, query);

        final HstQueryResult execute = query.execute();
        return getPageableFactory().createPageable(
                execute.getHippoBeans(),
                execute.getTotalSize(),
                pageSize,
                page);
    }


    protected <T extends EssentialsListComponentInfo>
    void applyExcludeScopes(final HstRequest request, final HstQuery query, final T paramInfo) {
        // just an extension point for time being
    }

    /**
     * Create a list of filters and apply them to the query, using AND logic.
     *
     * @param request current HST request
     * @param query   query under construction
     * @throws FilterException
     */
    protected void buildAndApplyFilters(final HstRequest request, final HstQuery query) throws FilterException {
        final List<BaseFilter> filters = new ArrayList<>();

        contributeAndFilters(filters, request, query);

        final Filter queryFilter = createQueryFilter(request, query);
        if (queryFilter != null) {
            filters.add(queryFilter);
        }

        applyAndFilters(query, filters);
    }

    /**
     * Extension point for sub-classes: contribute to a list of filters to apply using AND logic.
     *
     * @param filters list of filters under construction
     * @param request current HST request
     * @param query   query under construction, provider for new filters
     * @throws FilterException
     */
    protected void contributeAndFilters(final List<BaseFilter> filters, final HstRequest request, final HstQuery query) throws FilterException {
        // empty
    }

    /**
     * Apply a list of filters fo a query, using AND logic.
     * <p/>
     * Make sure that if the query already had a filter, it gets preserved.
     *
     * @param query   query under construction
     * @param filters list of filters to be AND-ed
     * @throws FilterException
     */
    protected void applyAndFilters(final HstQuery query, final List<BaseFilter> filters) throws FilterException {
        final BaseFilter oldRootFilter = query.getFilter();
        if (oldRootFilter != null) {
            filters.add(oldRootFilter);
        }

        if (filters.size() > 1) {
            final Filter andFilter = query.createFilter();
            for (BaseFilter filter : filters) {
                andFilter.addAndFilter(filter);
            }
            query.setFilter(andFilter);
        } else if (filters.size() == 1) {
            query.setFilter(filters.get(0));
        }
    }

    /**
     * Apply search filter (query) to result list
     *
     * @param request current HST request
     * @param query   query under construction, provider for new filters
     * @throws FilterException
     */
    protected Filter createQueryFilter(final HstRequest request, final HstQuery query) throws FilterException {
        Filter queryFilter = null;

        // check if we have query parameter
        final String queryParam = getSearchQuery(request);
        if (!Strings.isNullOrEmpty(queryParam)) {
            log.debug("using search query {}", queryParam);

            queryFilter = query.createFilter();
            queryFilter.addContains(".", queryParam);
        }
        return queryFilter;
    }


    /**
     * Fetches search query from reqest and cleans it
     *
     * @param request HstRequest
     * @return null if query was null or invalid
     */
    protected String getSearchQuery(HstRequest request) {
        return cleanupSearchQuery(getAnyParameter(request, REQUEST_PARAM_QUERY));
    }


    /**
     * Determine the current page of the list query.
     *
     * @param request the current request
     * @return the current page of the query
     */
    protected int getCurrentPage(final HstRequest request) {
        return getAnyIntParameter(request, REQUEST_PARAM_PAGE, 1);
    }

    /**
     * Determine the page size of the list query.
     *
     * @param request   the current request
     * @param paramInfo the settings of the component
     * @return the page size of the query
     */
    protected int getPageSize(final HstRequest request, final EssentialsPageable paramInfo) {
        // NOTE although unused, leave request parameter so devs
        // can use it if they override this method
        return paramInfo.getPageSize();
    }

    /**
     * Determine the path to use for the scope of the query. Returns null
     * when no path is defined.
     *
     * @param paramInfo the settings of the component
     * @return the scope of the query
     */
    protected String getScopePath(final EssentialsListComponentInfo paramInfo) {
        if (paramInfo == null) {
            log.warn("Component parameter was null for:  {}", getClass().getName());
            return null;
        }
        return paramInfo.getPath();
    }
    /**
     * Determine whether pagination should be shown.
     *
     * @param request   the current request
     * @param paramInfo the settings of the component
     * @return          flag indicating whether or not to show pagination
     */
    protected boolean isShowPagination(final HstRequest request, final EssentialsPageable paramInfo) {
        final Boolean showPagination = paramInfo.getShowPagination();
        if (showPagination == null) {
            log.debug("Show pagination not configured, use default value 'true'");
            return true;
        }
        return showPagination;
    }


}
