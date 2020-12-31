/*
 *  Copyright 2021 Hippo B.V. (http://www.onehippo.com)
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
import org.apache.commons.lang3.StringUtils;
import org.hippoecm.hst.component.pagination.HippoBeanPaginationUtils;
import org.hippoecm.hst.component.pagination.Pagination;
import org.hippoecm.hst.component.support.bean.info.dynamic.DocumentQueryDynamicComponentInfo;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.content.beans.query.HstQuery;
import org.hippoecm.hst.content.beans.query.HstQueryResult;
import org.hippoecm.hst.content.beans.query.builder.Constraint;
import org.hippoecm.hst.content.beans.query.builder.ConstraintBuilder;
import org.hippoecm.hst.content.beans.query.builder.HstQueryBuilder;
import org.hippoecm.hst.content.beans.query.exceptions.QueryException;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.parameters.ParametersInfo;
import org.hippoecm.hst.util.PathUtils;
import org.hippoecm.repository.util.DateTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.hst.content.beans.query.builder.ConstraintBuilder.and;
import static org.hippoecm.hst.content.beans.query.builder.ConstraintBuilder.constraint;

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
        final Pagination<HippoBean> pagination = doSearch(request, info);
        request.setModel(REQUEST_ATTR_PAGINATION, pagination);
    }

    /**
     * Executes the search
     *
     * @param request current HST request
     * @param info    instance of {@link DocumentQueryDynamicComponentInfo}
     * @return a {@link Pagination} object holding the search results, or null if search failed.
     */
    protected Pagination<HippoBean> doSearch(final HstRequest request, final DocumentQueryDynamicComponentInfo info) {
        final int pageSize = getPageSize(request, info);
        final int page = getCurrentPage(request);
        final int pageLimit = getPageLimit(request);

        final HstQuery query = getBuilder(request, info)
                .where(getConstraint(request, info))
                .limit(pageSize)
                .offset((page - 1) * pageSize)
                .orderByCaseInsensitive(HstQueryBuilder.Order.fromString(info.getSortOrder()), info.getSortField())
                .build();

        if (query == null) {
            log.warn("Unexpected error: query object is null");
            return null;
        }

        try {
            final HstQueryResult result = query.execute();
            return HippoBeanPaginationUtils.createPagination(
                    result.getHippoBeans(),
                    result.getTotalSize(),
                    pageSize,
                    page,
                    pageLimit);
        } catch (QueryException e) {
            log.error("Error running query", e.getMessage());
            log.warn("Query exception: ", e);
        }
        return null;
    }

    /**
     * Create an {@link HstQueryBuilder} to build the query
     *
     * @param request current HST request
     * @param info    instance of {@link DocumentQueryDynamicComponentInfo}
     * @return a new instance of {@link HstQueryBuilder}
     */
    protected HstQueryBuilder getBuilder(final HstRequest request, final DocumentQueryDynamicComponentInfo info) {
        final String documentTypes = info.getDocumentTypes();
        final String[] types = parseCommaSeparatedValue(documentTypes);
        if (log.isDebugEnabled()) {
            log.debug("Searching for document types:  {}, and including subtypes: {}", documentTypes, info.getIncludeSubtypes());
        }

        HstQueryBuilder builder = HstQueryBuilder.create(getSearchScope(request, info.getScope()));
        return info.getIncludeSubtypes() ? builder.ofTypes(types) : builder.ofPrimaryTypes(types);
    }

    /**
     * Returns search scope as a HippoBean, for given path relative to site content root. If path is null, the returned
     * scope is the site content root
     *
     * @param request current HST request. Unused, available when overriding
     * @param path    document (or folder) path relative to site-root.
     * @return bean identified by path. Site root bean if path empty or no corresponding bean.
     */
    @SuppressWarnings("unused")
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
     * Create a (composite, AND-ed) constraint to be used for filtering the query
     *
     * @param request current HST request. Unused, available when overriding
     * @param info    instance of {@link DocumentQueryDynamicComponentInfo}
     * @return a {@link Constraint} to be used in the query
     */
    @SuppressWarnings("unused")
    protected Constraint getConstraint(final HstRequest request, final DocumentQueryDynamicComponentInfo info) {
        final List<Constraint> constraints = new ArrayList<>();

        //hide past/future items
        final String dateField = info.getDateField();
        if (StringUtils.isNotBlank(dateField) && (info.getHidePastItems() || info.getHideFutureItems())) {
            ConstraintBuilder constraintBuilder = constraint(dateField);
            if (info.getHidePastItems()) {
                constraints.add(constraintBuilder.greaterOrEqualThan(Calendar.getInstance(), DateTools.Resolution.DAY));
            }
            if (info.getHideFutureItems()) {
                constraints.add(constraintBuilder.lessOrEqualThan(Calendar.getInstance(), DateTools.Resolution.DAY));
            }
        }
        return and(constraints.toArray(new Constraint[0]));
    }

    /**
     * Determine the page size of the query.
     *
     * @param request current HST request. Unused, available when overriding
     * @param info    the settings of the component
     * @return the page size of the query
     */
    @SuppressWarnings("unused")
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
     * Determine the limit of the number of pages shown in the pagination.
     *
     * @param request the current request
     * @return the page limit of the query
     */
    protected int getPageLimit(final HstRequest request) {
        String value = request.getParameter(REQUEST_PARAM_LIMIT);
        if (!Strings.isNullOrEmpty(value)) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException ignore) {
                // ignore exception
            }
        }
        return 10;
    }

    /**
     * For given string, comma separate it and convert to array
     *
     * @param inputString comma separated document types
     * @return array with document types, or empty array if inputString was null or empty
     */
    @Nonnull
    protected static String[] parseCommaSeparatedValue(final String inputString) {
        if (Strings.isNullOrEmpty(inputString)) {
            return ArrayUtils.EMPTY_STRING_ARRAY;
        }
        final Iterable<String> iterable = Splitter.on(",").trimResults().omitEmptyStrings().split(inputString);
        return Iterables.toArray(iterable, String.class);
    }
}