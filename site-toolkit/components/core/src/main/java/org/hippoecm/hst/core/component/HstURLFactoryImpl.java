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

import org.hippoecm.hst.core.container.HstContainerURL;
import org.hippoecm.hst.core.container.HstContainerURLProvider;
import org.hippoecm.hst.core.container.HstContainerURLProviderProvider;
import org.hippoecm.hst.core.request.HstRequestContext;

public class HstURLFactoryImpl implements HstURLFactory {
    
    protected HstContainerURLProviderProvider urlProviderProvider;
    protected boolean referenceNamespaceIgnored;

    public void setUrlProviderProvider(HstContainerURLProviderProvider urlProviderProvider) {
        this.urlProviderProvider = urlProviderProvider;
    }
    
    public HstContainerURLProviderProvider getUrlProviderProvider() {
        return this.urlProviderProvider;
    }
    
    public HstContainerURLProvider getServletUrlProvider() {
        return this.urlProviderProvider.getServletUrlProvider();
    }
    
    public HstContainerURLProvider getPortletUrlProvider() {
        return this.urlProviderProvider.getPortletUrlProvider();
    }
    
    public void setReferenceNamespaceIgnored(boolean referenceNamespaceIgnored) {
        this.referenceNamespaceIgnored = referenceNamespaceIgnored;
    }
    
    public boolean isReferenceNamespaceIgnored() {
        return referenceNamespaceIgnored;
    }
    
    public HstURL createURL(String type, String referenceNamespace, HstContainerURL containerURL) {
        return new HstURLImpl(type, containerURL, referenceNamespaceIgnored ? "" : referenceNamespace, containerURL.isViaPortlet() ? getPortletUrlProvider() : getServletUrlProvider(), null);
    }
    
    public HstURL createURL(String type, String referenceNamespace, HstContainerURL containerURL, HstRequestContext requestContext) {
        // if container url == null, use the requestContext baseUrl
        HstContainerURL baseContainerURL = (containerURL == null ? requestContext.getBaseURL() : containerURL);
        return new HstURLImpl(type, baseContainerURL, referenceNamespaceIgnored ? "" : referenceNamespace, requestContext.getBaseURL().isViaPortlet() ? getPortletUrlProvider() : getServletUrlProvider(), requestContext);
    }

}
