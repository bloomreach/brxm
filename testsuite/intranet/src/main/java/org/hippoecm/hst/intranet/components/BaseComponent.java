/*
 *  Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.intranet.components;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.hippoecm.hst.intranet.componentsinfo.GeneralListInfo;
import org.hippoecm.hst.intranet.componentsinfo.PageableListInfo;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.component.support.bean.BaseHstComponent;
import org.hippoecm.hst.content.beans.query.HstQuery;
import org.hippoecm.hst.content.beans.query.HstQueryResult;
import org.hippoecm.hst.content.beans.query.exceptions.QueryException;
import org.hippoecm.hst.content.beans.query.filter.Filter;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.util.SearchInputParsingUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BaseComponent extends BaseHstComponent {

    public static final Logger log = LoggerFactory.getLogger(BaseComponent.class);

    /**
     * Creates and executes a search, and puts a {@link org.hippoecm.hst.content.beans.query.HstQueryResult}, {@link PageableListInfo}, crPage, query and optionally a {@link java.util.List<Integer>} of pages on the
     * request
     * @param request
     * @param info
     * @param scope the scope to search below.
     * @param query the free text query to search for. If <code>null</code> or empty, it will be ignored
     */

    protected void createAndExecuteSearch(final HstRequest request, final GeneralListInfo info, final HippoBean scope, final String query) throws HstComponentException {
        if (scope == null) {
            throw new HstComponentException("Scope is not allowed to be null for a search");
        }
        int pageSize = info.getPageSize();
        if (pageSize == 0) {
            log.info("Empty pageSize or set to null. This is not a valid size. Use default size");
        }
        String docType = info.getDocType();
        String sortBy = info.getSortBy();
        String sortOrder = info.getSortOrder();
        String crPageStr = request.getParameter("page");

        int crPage = 1;
        if (crPageStr != null) {
            try {
                crPage = Integer.parseInt(crPageStr);
            } catch (NumberFormatException e) {
                log.info("Invalid page number {}. Use page 1 as default", crPageStr);
            }
        }

        final HstRequestContext requestContext = request.getRequestContext();

        @SuppressWarnings("rawtypes")
        Class filterClass = requestContext.getContentBeansTool().getObjectConverter().getAnnotatedClassFor(docType);
        if (filterClass == null) {
            throw new HstComponentException("There is no bean for docType '"+docType+"'. Cannot use '"+docType+"' as in this search");
        }
        try {
            @SuppressWarnings("unchecked")
            HstQuery hstQuery = requestContext.getQueryManager().createQuery(scope, filterClass, true);
            hstQuery.setLimit(pageSize);
            hstQuery.setOffset(pageSize * (crPage - 1));
            if (sortBy != null && !sortBy.isEmpty()) {
                if (sortOrder == null || sortOrder.isEmpty() || "descending".equals(sortOrder)) {
                    hstQuery.addOrderByDescending(sortBy);
                } else {
                    hstQuery.addOrderByAscending(sortBy);
                }
            }

            String parsedQuery = SearchInputParsingUtils.parse(query, false);
            if (parsedQuery != null && !parsedQuery.equals(query)) {
                log.debug("Replaced query '{}' with '{}' because it contained invalid chars.", query, parsedQuery);
            }
            if (!StringUtils.isEmpty(parsedQuery)) {
                Filter f = hstQuery.createFilter();
                f.addContains(".", parsedQuery);
                hstQuery.setFilter(f);
            }

            HstQueryResult result = hstQuery.execute();

            request.setAttribute("result", result);
            request.setAttribute("info", info);
            request.setAttribute("page", crPage);
            request.setAttribute("query", parsedQuery);


            if (info instanceof PageableListInfo && ((PageableListInfo)info).isPagesVisible()) {
                // add pages
                if (result.getTotalSize() > pageSize) {
                    Collection<Integer> pages = new ArrayList<Integer>();
                    int numberOfPages = result.getTotalSize() / pageSize ;
                    if (result.getTotalSize() % pageSize != 0) {
                        numberOfPages++;
                    }
                    for (int i = 0; i < numberOfPages; i++) {
                        pages.add(i + 1);
                    }
                    request.setAttribute("pages", pages);
                }
            }

        } catch (QueryException e) {
            throw new HstComponentException("Exception occurred during creation or execution of HstQuery. ", e);
        }
    }
}