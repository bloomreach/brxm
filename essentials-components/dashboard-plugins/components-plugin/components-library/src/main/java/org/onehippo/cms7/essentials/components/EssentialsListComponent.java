/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 */

package org.onehippo.cms7.essentials.components;

import javax.servlet.http.HttpServletResponse;

import org.hippoecm.hst.content.beans.query.HstQuery;
import org.hippoecm.hst.content.beans.query.HstQueryResult;
import org.hippoecm.hst.content.beans.query.exceptions.FilterException;
import org.hippoecm.hst.content.beans.query.exceptions.QueryException;
import org.hippoecm.hst.content.beans.query.filter.Filter;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoDocumentIterator;
import org.hippoecm.hst.content.beans.standard.HippoFacetNavigationBean;
import org.hippoecm.hst.content.beans.standard.HippoResultSetBean;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.parameters.ParametersInfo;
import org.onehippo.cms7.essentials.components.info.EssentialsDocumentListComponentInfo;
import org.onehippo.cms7.essentials.components.info.EssentialsPageable;
import org.onehippo.cms7.essentials.components.paging.IterablePagination;
import org.onehippo.cms7.essentials.components.paging.Pageable;
import org.onehippo.cms7.essentials.components.utils.SiteUtils;
import org.onehippo.cms7.essentials.components.utils.query.HstQueryBuilder;
import org.onehippo.cms7.essentials.components.utils.query.QueryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

/**
 * HST component used for listing of documents.
 *
 * @version "$Id$"
 */
@ParametersInfo(type = EssentialsDocumentListComponentInfo.class)
public class EssentialsListComponent extends CommonComponent {

    private static Logger log = LoggerFactory.getLogger(EssentialsListComponent.class);


    @Override
    public void doBeforeRender(final HstRequest request, final HstResponse response) {
        final EssentialsDocumentListComponentInfo paramInfo = getComponentParametersInfo(request);
        final String path = getScopePath(paramInfo);
        log.debug("Calling EssentialsListComponent for documents path:  [{}]", path);
        final HippoBean scope = getSearchScope(request, path);

        if (scope == null) {
            log.warn("Search scope was null");
            handleInvalidScope(request, response);
            return;
        }

        final Pageable<HippoBean> pageable;
        if (scope instanceof HippoFacetNavigationBean) {
            final HippoFacetNavigationBean facetBean = (HippoFacetNavigationBean) scope;
            final HippoResultSetBean resultSet = facetBean.getResultSet();
            final HippoDocumentIterator<HippoBean> iterator = resultSet.getDocumentIterator(HippoBean.class);
            pageable = new IterablePagination<>(iterator, resultSet.getCount().intValue(), paramInfo.getPageSize(), getCurrentPage(request));
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
     * @see CommonComponent#REQUEST_PARAM_QUERY
     * @see CommonComponent#REQUEST_PARAM_PAGEABLE
     * @see CommonComponent#REQUEST_PARAM_PAGE
     * @see CommonComponent#REQUEST_PARAM_PAGE_SIZE
     * @see CommonComponent#REQUEST_PARAM_PAGE_PAGINATION
     */
    protected void populateRequest(final HstRequest request, final EssentialsPageable paramInfo, final Pageable<? extends HippoBean> pageable) {
        request.setAttribute(REQUEST_PARAM_QUERY, getSearchQuery(request));
        request.setAttribute(REQUEST_PARAM_PAGEABLE, pageable);
        request.setAttribute(REQUEST_PARAM_PAGE, getCurrentPage(request));
        request.setAttribute(REQUEST_PARAM_PAGE_SIZE, paramInfo.getPageSize());
        request.setAttribute(REQUEST_PARAM_PAGE_PAGINATION, paramInfo.getShowPagination());
    }

    /**
     * Returns Search scope for given path. If path is null, current scope bean will be used, site wide scope otherwise
     *
     * @param request
     * @param path    path (optional)
     * @return hippo bean or null
     */
    protected HippoBean getSearchScope(final HstRequest request, final String path) {
        HippoBean scope;
        if (Strings.isNullOrEmpty(path)) {
            scope = getContentBean(request);
            if (scope == null) {
                // use global scope by default
                scope = getSiteContentBaseBean(request);
            }
        } else {
            scope = getScopeBean(request, path);
        }
        return scope;
    }

    protected <T extends EssentialsDocumentListComponentInfo> Pageable<HippoBean> doSearch(final HstRequest request, final T paramInfo, final HippoBean scope) {
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
    protected <T extends EssentialsDocumentListComponentInfo> HstQuery buildQuery(final HstRequest request, final T paramInfo, final HippoBean scope) {
        final QueryBuilder builder = new HstQueryBuilder(this, request);
        final String documentTypes = paramInfo.getDocumentTypes();
        final String[] types = SiteUtils.parseCommaSeparatedValue(documentTypes);
        if (log.isDebugEnabled()) {
            log.debug("Searching for document types:  {}, and including subtypes: {}", documentTypes, paramInfo.getIncludeSubtypes());
        }
        return builder.scope(scope).documents(types).includeSubtypes().build();
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
    protected <T extends EssentialsPageable> Pageable<HippoBean> executeQuery(final HstRequest request, final T paramInfo, final HstQuery query) throws QueryException {
        final int pageSize = getPageSize(request, paramInfo);
        final int page = getCurrentPage(request);
        query.setLimit(pageSize);
        query.setOffset((page - 1) * pageSize);
        applySearchFilter(request, query);


        final HstQueryResult execute = query.execute();
        final Pageable<HippoBean> pageable = new IterablePagination<>(
                execute.getHippoBeans(),
                execute.getTotalSize(),
                pageSize,
                page);
        pageable.setShowPagination(isShowPagination(request, paramInfo));
        return pageable;
    }

    /**
     * Apply search filter (query) to result list
     *
     * @param request HstRequest
     * @param query   HstQuery
     * @throws FilterException
     */
    protected void applySearchFilter(final HstRequest request, final HstQuery query) throws FilterException {
        // check if we have query parameter
        final String queryParam = getSearchQuery(request);
        if (!Strings.isNullOrEmpty(queryParam)) {
            final Filter filter = query.createFilter();
            filter.addContains(".", queryParam);
            log.debug("using search query {}", queryParam);
            query.setFilter(filter);
        }
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
    protected String getScopePath(final EssentialsDocumentListComponentInfo paramInfo) {
        return paramInfo.getPath();
    }

    /**
     * Determine whether pagination should be shown.
     *
     * @param request   the current request
     * @param paramInfo the settings of the component
     * @return
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
