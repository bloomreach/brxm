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
package org.hippoecm.hst.core.component;

import javax.servlet.http.HttpServletRequest;

import org.hippoecm.hst.core.container.HstContainerURL;
import org.hippoecm.hst.core.container.HstContainerURLProvider;

public class HstURLFactoryImpl implements HstURLFactory {
    
    protected HstContainerURLProvider servletUrlProvider;
    protected HstContainerURLProvider portletUrlProvider;

    public void setServletUrlProvider(HstContainerURLProvider servletUrlProvider) {
        this.servletUrlProvider = servletUrlProvider;
    }
    
    public void setPortletUrlProvider(HstContainerURLProvider portletUrlProvider) {
        this.portletUrlProvider = portletUrlProvider;
    }
    
    public HstContainerURLProvider getUrlProvider(HttpServletRequest request) {
        HstContainerURLProvider urlProvider = null;
        
        if (request.getAttribute("javax.portlet.request") != null) {
            urlProvider = this.portletUrlProvider;
        } else {
            urlProvider = this.servletUrlProvider;
        }
        
        return urlProvider;
    }
    
    public HstURL createURL(String type, String referenceNamespace, HstContainerURL baseContainerURL) {
        HstURLImpl url = new HstURLImpl(type, baseContainerURL.isViaPortlet() ? this.portletUrlProvider : this.servletUrlProvider);
        url.setReferenceNamespace(referenceNamespace);
        url.setBaseContainerURL(baseContainerURL);
        return url;
    }

}
