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
import org.hippoecm.hst.content.beans.ObjectBeanManagerException;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.linking.HstLink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Home extends BaseHstComponent {

    public static final Logger log = LoggerFactory.getLogger(Home.class);
 
    @Override
    public void doBeforeRender(HstRequest request, HstResponse response) throws HstComponentException {
        
        try {
            // example of REST links for a hippo:resource
            HippoBean image = (HippoBean) this.getObjectBeanManager(request).getObject("/content/gallery/news/screenshot_cms_small.jpg");
            HstLink link = request.getRequestContext().getHstLinkCreator().create(image.getNode(), request.getRequestContext(), "restapi-gallery");
            request.setAttribute("restLink",link);
            
        } catch (ObjectBeanManagerException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
         
        super.doBeforeRender(request, response);
        HippoBean n = this.getContentBean(request);
        
        if(n == null) {
            return;
        }
        request.setAttribute("document",n);
    }
}