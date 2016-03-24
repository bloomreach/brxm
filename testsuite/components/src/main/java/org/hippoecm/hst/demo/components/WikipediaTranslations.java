/*
 *  Copyright 2009-2016 Hippo B.V. (http://www.onehippo.com)
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

import java.util.LinkedList;
import java.util.List;

import org.hippoecm.hst.component.support.bean.BaseHstComponent;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.content.beans.query.HstQuery;
import org.hippoecm.hst.content.beans.query.HstQueryResult;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoBeanIterator;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.demo.beans.WikiBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WikipediaTranslations extends BaseHstComponent {

    public static final Logger log = LoggerFactory.getLogger(WikipediaTranslations.class);

    @Override
    public void doBeforeRender(HstRequest request, HstResponse response) throws HstComponentException {
        super.doBeforeRender(request, response);

        final HstRequestContext requestContext = RequestContextProvider.get();

        HippoBean scope = requestContext.getContentBean();

        try {
            HstQuery hstQuery = requestContext.getQueryManager().createQuery(scope, WikiBean.class, true);
            hstQuery.setLimit(500);
            HstQueryResult queryResult = hstQuery.execute();
            List<WikiBean> result = new LinkedList<>();

            for (HippoBeanIterator bit = queryResult.getHippoBeans(); bit.hasNext(); ) {
                WikiBean wikiBean = (WikiBean) bit.nextHippoBean();

                if (wikiBean != null) {
                    result.add(wikiBean);
                }
            }

            request.setAttribute("result", result);
        } catch (Exception e) {
            log.warn("Failed to query WikiBeans.", e);
        }
    }

}