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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.demo.util.DateRangeQueryConstraints;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Search extends AbstractSearchComponent {

    public static final Logger log = LoggerFactory.getLogger(Search.class);

    private final static String DATE_FORMAT = "MM/dd/yyyy";

    @Override
    public void doBeforeRender(HstRequest request, HstResponse response) throws HstComponentException {
        super.doBeforeRender(request, response);

        String query = getPublicRequestParameter(request, "query");
        String pageSizeString = getPublicRequestParameter(request, "pageSize");

        if (StringUtils.isBlank(query)) {
            return;
        }

        int pageSize;
        try {
            pageSize = Integer.parseInt(pageSizeString);

            if (pageSize <= 0) {
                pageSize = DEFAULT_PAGE_SIZE;
            } else {
                request.setAttribute("pageSize", pageSize);
            }
        } catch (NumberFormatException e) {
            pageSize = DEFAULT_PAGE_SIZE;
        }

        Calendar fromDate = getDateFor(request, "fromdate");
        Calendar toDate = getDateFor(request, "todate");
        Calendar fromDateResolution = getDateFor(request, "fromDateResolution");
        Calendar toDateResolution = getDateFor(request, "toDateResolution");

        String resolution = getPublicRequestParameter(request, "resolution");
        if (resolution == null) {
            resolution = request.getParameter("resolution");
        }
        request.setAttribute("resolution", resolution);

        DateRangeQueryConstraints dateRangeQueryConstraints;

        if (resolution == null){
            dateRangeQueryConstraints = new DateRangeQueryConstraints("hippostdpubwf:creationDate", fromDate, toDate, null);
        } else {
            dateRangeQueryConstraints = new DateRangeQueryConstraints("hippostdpubwf:creationDate", fromDateResolution, toDateResolution, resolution);
        }

        doSearch(request, response, query, null, null, pageSize, request.getRequestContext().getSiteContentBaseBean(), dateRangeQueryConstraints);
    }

    private Calendar getDateFor(final HstRequest request, final String param) {
        // since SimpleDateFormat is not thread safe, create new instance
        SimpleDateFormat formatter = new SimpleDateFormat(DATE_FORMAT);
        String dateString = getPublicRequestParameter(request, param);
        if (dateString == null) {
            dateString = request.getParameter(param);
        }
        if (StringUtils.isNotBlank(dateString)) {
            request.setAttribute(param, dateString);
            try {
                Date date = formatter.parse(dateString);
                Calendar cal = Calendar.getInstance();
                cal.setTime(date);
                return cal;
            } catch (ParseException e) {
                log.warn("Skip invalid date '{}' for '{}'", dateString, param);
            }
        }
        return null;
    }

}
