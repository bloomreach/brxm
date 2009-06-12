/*
 *  Copyright 2008 Hippo.
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

import org.hippoecm.hst.component.support.bean.BaseHstComponent;
import org.hippoecm.hst.content.beans.query.HstQuery;
import org.hippoecm.hst.content.beans.query.HstQueryManager;
import org.hippoecm.hst.content.beans.query.HstQueryResult;
import org.hippoecm.hst.content.beans.query.exceptions.QueryException;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoBeanIterator;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.demo.beans.GeneralPage;
import org.hippoecm.hst.demo.util.PageableCollection;
import org.hippoecm.hst.demo.util.SearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Archive extends BaseHstComponent {

    public static final Logger log = LoggerFactory.getLogger(Archive.class);
    
    private static PageableCollection<SearchResult<HippoBean>> EMPTY_RESULTS = new PageableCollection<SearchResult<HippoBean>>(0);
    
    public static final int DEFAULT_PAGE_SIZE = 5;
    
    @Override
    public void doBeforeRender(HstRequest request, HstResponse response) throws HstComponentException {
       
        HippoBean currentBean = this.getContentBean(request);
        if (currentBean == null) {
            log.error("You need to create home/search document");
            request.setAttribute("searchResults", EMPTY_RESULTS);
            return;
        }
        
        String pageParam = request.getParameter("page");
        if (pageParam == null) {
            pageParam = getPublicRequestParameter(request, "page");
        }
        int page = getIntValue(pageParam, 1);

        request.setAttribute("page", page);
        HstQueryManager manager = getQueryManager();
        try {

            final HstQuery query = manager.createQuery(request.getRequestContext(), currentBean);
            query.addOrderByDescending("demosite:date");
            
            final HstQueryResult result = query.execute();
            PageableCollection<SearchResult<HippoBean>> results = new PageableCollection<SearchResult<HippoBean>>(result.getSize());
            results.setPageNumber(page);
            results.setPageSize(DEFAULT_PAGE_SIZE);
            int startAt = results.getStartOffset();

            final HippoBeanIterator iterator = result.getHippoBeans();
            // don't skip past unreachable item:
            if (startAt < results.getTotal()) {
                iterator.skip(startAt);
            }
            int count = 0;

            while (iterator.hasNext() && count <= DEFAULT_PAGE_SIZE) {
                HippoBean bean = iterator.nextHippoBean();
                // note: bean can be null
                if (bean != null && bean instanceof GeneralPage) {
                    GeneralPage pageBean = (GeneralPage)bean;
                    results.addItem(new SearchResult<HippoBean>(bean, pageBean.getTitle(), pageBean.getSummary(), pageBean.getDate()));
                    count++;
                }
            }
            request.setAttribute("searchResults", results);

        } catch (QueryException e) {
            log.error("Exception in searchComponent:" + e);
            setError(request, "An error occurred, invalid query syntax?");
        }
        
    }
 
    /**
     * Set error message on request so we can display it to user
     *
     * @param errorMessage message to display
     * @param request      HstRequest instance
     */
    public void setError(final HstRequest request, final String errorMessage) {
        request.setAttribute("error", errorMessage);
    }
    
    /**
     * Parses int value from string object.
     * If value is null or parsing error occures, it returns default value
     *
     * @param value        value to be parsed
     * @param defaultValue default value to return
     * @return parsed value or default value on error
     */
    public static int getIntValue(String value, int defaultValue) {

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            //ignore
        }
        return defaultValue;
    }

}