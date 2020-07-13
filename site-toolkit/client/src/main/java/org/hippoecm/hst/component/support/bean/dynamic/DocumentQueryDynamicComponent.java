/*
 *  Copyright 2020 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.component.support.bean.dynamic;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.annotation.Nonnull;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;

import org.apache.commons.lang3.ArrayUtils;
import org.hippoecm.hst.component.pagination.IterablePaginationUtils;
import org.hippoecm.hst.component.pagination.Pagination;
import org.hippoecm.hst.component.support.bean.info.dynamic.DocumentQueryDynamicComponentInfo;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.content.beans.query.HstQuery;
import org.hippoecm.hst.content.beans.query.HstQueryResult;
import org.hippoecm.hst.content.beans.query.builder.HstQueryBuilder;
import org.hippoecm.hst.content.beans.query.exceptions.FilterException;
import org.hippoecm.hst.content.beans.query.exceptions.QueryException;
import org.hippoecm.hst.content.beans.query.filter.BaseFilter;
import org.hippoecm.hst.content.beans.query.filter.Filter;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.parameters.ParametersInfo;
import org.hippoecm.hst.util.PathUtils;
import org.hippoecm.repository.util.DateTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.hst.component.support.bean.info.dynamic.DocumentQueryDynamicComponentInfo.DESC;

/**
 * An HST component implementation that provides querying behaviour on top of the {@link BaseHstDynamicComponent}.
 *
 * <p>
 * Any subclass of this class must include the annotation {@link ParametersInfo}, which must specify the interface
 * {@link DocumentQueryDynamicComponentInfo} or an extension of it as its {@link ParametersInfo#type()}.
 * </p>
 * <p>
 * The component inherits the behaviour of its parent {@link BaseHstDynamicComponent} (echoing all parameters) and
 * additionally allows for querying, via the following parameters:
 * </p>
 *
 * <table>
 *   <col width="20%"/>
 *   <col width="15%"/>
 *   <col width="65%"/>
 *   <thead>
 *     <tr><th>Parameter</th><th>Type</th><th>Description</th></tr>
 *   <thead>
 *   <tbody>
 *      <tr><td>DocumentTypes</td><td>string</td><td>Comma separated list of document type names. This triggers the search (for documents of that type). This parameter is required, otherwise the component only echoes</td></tr>
 *      <tr><td>scope</td><td>JcrPath</td><td>The scope for the search</td></tr>
 *      <tr><td>includeSubtypes</td><td>boolean</td><td>Whether to include subtypes in the search (default false)</td></tr>
 *      <tr><td>sortField</td><td>string</td><td>Field to sort on</td></tr>
 *      <tr><td>sortOrder</td><td>dropdown</td><td>Sort order enum (ASC,DESC) (default ASC)</td></tr>
 *      <tr><td>pageSize</td><td>int</td><td>Max number of items per page (default 10)</td></tr>
 *      <tr><td>dateField</td><td>string</td><td>Path of a date field in the document type(s) used in the query. The value of this field is used in the contributed date filter</td></tr>
 *      <tr><td>hideFutureItems</td><td>boolean</td><td>If set, a date filter is added to the query that filters out items whose dateField property is greater than the current (runtime) date. Default false</td></tr>
 *      <tr><td>hidePastItems</td><td>boolean</td><td>If set, a date filter is added to the query that filters out items whose dateField property is less than the current (runtime) date. Default false</td></tr>
 *   </tbody>
 * </table>
 * <p>
 * Results from the query are set in attribute as model, with key "pagination".
 * Pagination can be controlled via (namespaced) request parameter "page".
 * </p>
 *
 * @version $Id$
 */
@ParametersInfo(type = DocumentQueryDynamicComponentInfo.class)
public class DocumentQueryDynamicComponent extends BaseHstDynamicComponent {

    private static Logger log = LoggerFactory.getLogger(DocumentQueryDynamicComponent.class);

    protected static final String REQUEST_PARAM_PAGE = "page";
    protected static final String REQUEST_ATTR_PAGINATION = "pagination";


    @Override
    public void doBeforeRender(final HstRequest request, final HstResponse response) throws HstComponentException {
        super.doBeforeRender(request, response);

        DocumentQueryDynamicComponentInfo info = getComponentParametersInfo(request);
        final HippoBean scope = getSearchScope(request, info.getScope());

        if (scope == null) {
            log.error("This component requires a valid scope to retrieve documents");
            return;
        }

        final Pagination<HippoBean> pagination = doSearch(request, info, scope);
        request.setModel(REQUEST_ATTR_PAGINATION, pagination);
    }

    /**
     * Returns search scope as a HippoBean, for given path relative to site content root. If path is null, the returned
     * scope is the site content root
     *
     * @param request current HST request. Unused, available when overriding.
     * @param path    document (or folder) path relative to site-root.
     * @return bean identified by path. Site root bean if path empty or no corresponding bean.
     */
    @SuppressWarnings("Unused")
    protected HippoBean getSearchScope(final HstRequest request, final String path) {
        final HippoBean siteBean = RequestContextProvider.get().getSiteContentBaseBean();
        if (Strings.isNullOrEmpty(path)) {
            return siteBean;
        }
        final String scopePath = PathUtils.normalizePath(path);
        log.debug("Looking for bean {}", scopePath);
        final HippoBean scope = siteBean.getBean(scopePath);
        if (scope == null) {
            log.warn("Bean was null for selected path:  {}", scopePath);
            return siteBean;
        }
        return scope;
    }

    /**
     * Execute the search given a scope
     *
     * @param request current HST request
     * @param info    instance of DocumentQueryDynamicComponentInfo
     * @param scope   bean representing search scope
     * @return pagination object holding the search results, or null if search failed.
     */
    protected Pagination<HippoBean> doSearch(final HstRequest request, final DocumentQueryDynamicComponentInfo info, final HippoBean scope) {
        final String documentTypes = info.getDocumentTypes();
        final String[] types = parseCommaSeparatedValue(documentTypes);
        if (log.isDebugEnabled()) {
            log.debug("Searching for document types:  {}, and including subtypes: {}", documentTypes, info.getIncludeSubtypes());
        }

        HstQueryBuilder builder = HstQueryBuilder.create(scope);
        final HstQuery query = info.getIncludeSubtypes() ? builder.ofTypes(types).build() : builder.ofPrimaryTypes(types).build();
        if (query == null) {
            log.warn("Unexpected error: query object is null");
            return null;
        }

        final int pageSize = getPageSize(request, info);
        final int page = getCurrentPage(request);
        query.setLimit(pageSize);
        query.setOffset((page - 1) * pageSize);

        applyOrdering(request, query, info);
        combineAndSetFilters(query, buildFilters(request, query, info));

        try {
            final HstQueryResult result = query.execute();
            return IterablePaginationUtils.createPagination(
                    result.getHippoBeans(),
                    result.getTotalSize(),
                    pageSize,
                    page);
        } catch (QueryException e) {
            log.error("Error running query", e.getMessage());
            log.warn("Query exception: ", e);
        }
        return null;
    }

    /**
     * Apply ordering, if sortField parameter is provided
     *
     * @param request instance of  HstRequest
     * @param query   instance of  HstQuery
     * @param info    instance of DocumentQueryDynamicComponentInfo
     */
    protected void applyOrdering(final HstRequest request, final HstQuery query, final DocumentQueryDynamicComponentInfo info) {
        final String sortField = info.getSortField();
        if (!Strings.isNullOrEmpty(sortField)) {
            final String sortOrder = Strings.isNullOrEmpty(info.getSortOrder()) ? DESC : info.getSortOrder();
            if (sortOrder.equals(DESC)) {
                query.addOrderByDescending(sortField);
            } else {
                query.addOrderByAscending(sortField);
            }
        }
    }

    /**
     * Create a list of filters
     *
     * @param request current HST request
     * @param query   query under construction
     * @param info    instance of DocumentQueryDynamicComponentInfo
     * @return list of filters to be used in the query
     */
    protected List<BaseFilter> buildFilters(final HstRequest request, final HstQuery query, final DocumentQueryDynamicComponentInfo info) {
        final List<BaseFilter> filters = new ArrayList<>();

        //hide past/future items
        final String dateField = info.getDateField();
        if (!Strings.isNullOrEmpty(dateField)) {
            if (info.getHidePastItems()) {
                try {
                    final Filter filter = query.createFilter();
                    filter.addGreaterOrEqualThan(dateField, Calendar.getInstance(), DateTools.Resolution.DAY);
                    filters.add(filter);
                } catch (FilterException e) {
                    log.error("Error while creating query filter to hide past items using date field {}", dateField, e);
                }
            }
            if (info.getHideFutureItems()) {
                try {
                    Filter filter = query.createFilter();
                    filter.addLessOrEqualThan(dateField, Calendar.getInstance(), DateTools.Resolution.DAY);
                    filters.add(filter);
                } catch (FilterException e) {
                    log.error("Error while creating query filter to hide future items using date field {}", dateField, e);
                }

            }
        }
        return filters;
    }

    /**
     * Combine a list of filters and apply them to the query, using AND logic
     * <p>
     * If the query already had a filter, it gets preserved.
     *
     * @param query   query under construction
     * @param filters list of filters to be AND-ed
     */
    protected void combineAndSetFilters(final HstQuery query, final List<BaseFilter> filters) {
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
     * Determine the page size of the query.
     *
     * @param request the current request
     * @param info    the settings of the component
     * @return the page size of the query
     */
    protected int getPageSize(final HstRequest request, final DocumentQueryDynamicComponentInfo info) {
        // NOTE although unused, leave request parameter so devs
        // can use it if they override this method
        return info.getPageSize();
    }

    /**
     * Determine the current page of the query.
     *
     * @param request the current request
     * @return the current page of the query
     */
    protected int getCurrentPage(final HstRequest request) {
        String value = request.getParameter(REQUEST_PARAM_PAGE);
        if (!Strings.isNullOrEmpty(value)) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException ignore) {
                // ignore exception
            }
        }
        return 1;
    }

    /**
     * For given string, comma separate it and convert to array
     *
     * @param inputString comma separated document types
     * @return array with document types, or empty array if inputString was null or empty
     */
    @Nonnull
    public static String[] parseCommaSeparatedValue(final String inputString) {
        if (Strings.isNullOrEmpty(inputString)) {
            return ArrayUtils.EMPTY_STRING_ARRAY;
        }
        final Iterable<String> iterable = Splitter.on(",").trimResults().omitEmptyStrings().split(inputString);
        return Iterables.toArray(iterable, String.class);
    }


}
