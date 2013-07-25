/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
import java.util.Date;
import java.util.List;

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
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.demo.beans.BaseBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RSS extends BaseHstComponent {
    
    public static final Logger log = LoggerFactory.getLogger(RSS.class);

    @Override
    public void doBeforeRender(HstRequest request, HstResponse response) throws HstComponentException {
        super.doBeforeRender(request, response);

        final HstRequestContext requestContext = request.getRequestContext();
        HippoBean contentBean = requestContext.getContentBean();
        if (contentBean == null) {
            log.error("Content path defined for RSS feed is invalid.");
            return;
        }

        HstQueryManager manager = requestContext.getContentBeansTool().getQueryManager();
        try {

            final HstQuery query = manager.createQuery(contentBean);
            query.addOrderByDescending("demosite:date");
            
            final HstQueryResult result = query.execute();
            List<HippoBean> results = new ArrayList<HippoBean>();

            final HippoBeanIterator iterator = result.getHippoBeans();

            while (iterator.hasNext()) {
                HippoBean bean = iterator.nextHippoBean();
                // note: bean can be null
                if (bean != null && bean instanceof BaseBean) {
                    results.add(bean);
                }
            }
            request.setAttribute("items", results);

        } catch (QueryException e) {
            log.error("Exception in RSS feed component: " + e);
        }
        
        request.setAttribute("today", new Date());
        
        // set Expires header to 10 minutes
        response.setDateHeader("Expires", System.currentTimeMillis() + 600000L);
    }

}
