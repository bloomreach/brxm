/*
 *  Copyright 2009-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.demo.components;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringEscapeUtils;
import org.hippoecm.hst.component.support.bean.BaseHstComponent;
import org.hippoecm.hst.content.beans.query.HstQuery;
import org.hippoecm.hst.content.beans.query.HstQueryResult;
import org.hippoecm.hst.content.beans.query.builder.Constraint;
import org.hippoecm.hst.content.beans.query.builder.HstQueryBuilder;
import org.hippoecm.hst.content.beans.query.exceptions.QueryException;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.demo.util.DateRangeQueryConstraints;
import org.hippoecm.hst.util.SearchInputParsingUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.hst.content.beans.query.builder.ConstraintBuilder.and;
import static org.hippoecm.hst.content.beans.query.builder.ConstraintBuilder.constraint;

public abstract class AbstractSearchComponent extends BaseHstComponent {

    public static final Logger log = LoggerFactory.getLogger(AbstractSearchComponent.class);

    public static final int DEFAULT_PAGE_SIZE = 5;

    
    
    
    @Override
    public void doBeforeRender(HstRequest request, HstResponse response) throws HstComponentException {
       super.doBeforeRender(request, response);
    }

    protected void doSearch(final HstRequest request,
                            final HstResponse response,
                            final String query,
                            final String nodeType,
                            final String sortBy,
                            final int pageSize,
                            final HippoBean scope) {
        doSearch(request, response, query, nodeType, sortBy, pageSize, scope, null);
    }

    protected void doSearch(final HstRequest request,
                            final HstResponse response,
                            final String query,
                            final String nodeType,
                            final String sortBy,
                            final int pageSize,
                            final HippoBean scope,
                            final DateRangeQueryConstraints drqc) {

        if (scope == null) {
            log.error("Scope for search is null.");
            return;
        }

        String pageParam = request.getParameter("page");
        if (pageParam == null) {
            pageParam = getPublicRequestParameter(request, "page");
        }
        int page = getIntValue(pageParam, 1);

        request.setAttribute("page", page);

        try {

            String parsedQuery = null;
            if (query != null) {
                request.setAttribute("query", StringEscapeUtils.escapeHtml(query));
                parsedQuery = SearchInputParsingUtils.parse(query, true);
            }

            int offset = (page - 1) * pageSize;

            Constraint constraint;
            if (drqc == null) {
                constraint = constraint(".").contains(parsedQuery);
            } else {
                constraint =
                        and(
                                constraint(".").contains(parsedQuery),
                                // below on purpose not between because drqc.getFromDate() or drqc.getToDate() can be
                                // null. #between expects both values not to be null
                                constraint(drqc.getProperty()).greaterOrEqualThan(drqc.getFromDate(), drqc.getResolution()),
                                constraint(drqc.getProperty()).lessOrEqualThan(drqc.getToDate(), drqc.getResolution())
                        );
            }
            HstQuery hstQuery = HstQueryBuilder.create(scope)
                    .ofTypes(nodeType)
                    .where(constraint)
                    .limit(pageSize)
                    .offset(offset)
                    .orderByDescending(sortBy)
                    .build();

            final HstQueryResult result = hstQuery.execute();

            request.setAttribute("result", result);
            request.setAttribute("crPage", page);

            int maxPages = 20;

            // add pages
            if(result.getTotalSize() > pageSize) {
                List<Integer> pages = new ArrayList<Integer>();
                int numberOfPages = result.getTotalSize() / pageSize ;
                if(result.getTotalSize() % pageSize != 0) {
                    numberOfPages++;
                }

                if (numberOfPages > maxPages) {
                    int startAt = 0;
                    if (offset > (10 * pageSize)) {
                        startAt = offset / pageSize;
                        startAt = startAt - 10;
                    }
                    for(int i = startAt; i < numberOfPages; i++) {
                        pages.add(i + 1);
                        if (i == (startAt + maxPages)) {
                            break;
                        }
                    }

                } else {
                    for(int i = 0; i < numberOfPages; i++) {
                        pages.add(i + 1);
                    }
                }
                request.setAttribute("pages", pages);
            }

        } catch (QueryException e) {
            log.error("Exception in searchComponent: ", e);
        }
        
    }

    
    /**
     * Parses int value from string object.
     * If value is null or parsing error occures, it returns default value
     *
     * @param value        value to be parsed
     * @param defaultValue default value to return
     * @return parsed value or default value on error
     */
    protected static int getIntValue(String value, int defaultValue) {

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            //ignore
        }
        return defaultValue;
    }
    
    
}
