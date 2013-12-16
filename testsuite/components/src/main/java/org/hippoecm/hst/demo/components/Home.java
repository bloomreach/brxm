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

import org.hippoecm.hst.component.support.bean.BaseHstComponent;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.content.beans.ObjectBeanManagerException;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.demo.channel.DemoChannelInfo;
import org.hippoecm.hst.site.HstServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Home extends BaseHstComponent {

    public static final Logger log = LoggerFactory.getLogger(Home.class);
 
    @Override
    public void doBeforeRender(HstRequest request, HstResponse response) throws HstComponentException {
        super.doBeforeRender(request, response);

        final HstRequestContext requestContext = request.getRequestContext();
        final Mount mount = requestContext.getResolvedMount().getMount();
        final DemoChannelInfo info = mount.getChannelInfo();
        if (info != null) {
            request.setAttribute("channelInfoExample", info.getExampleValue());
        }
        try {
            HippoBean image = (HippoBean) requestContext.getObjectBeanManager().getObject("/content/gallery/images/screenshot_cms_small.jpg");
            request.setAttribute("image",image);
        } catch (ObjectBeanManagerException e) {
            throw new HstComponentException(e);
        }

        HippoBean n = this.getContentBean(request);
        if (n == null) {
            return;
        }
        
        String greetingComponent = HstServices.getComponentManager().getComponent("greeting");
        if (!greetingComponent.equals("Hello, HST-2!")) {
            throw new HstComponentException("Client component 'greeting' should state 'Hello, HST-2'");
        }

        request.setAttribute("document", n);

        // test parametere from mount property
        request.setAttribute("testParamFromMount", getComponentParameter("testParamFromMount"));
    }


}