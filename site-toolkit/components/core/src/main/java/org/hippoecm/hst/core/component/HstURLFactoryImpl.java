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
package org.hippoecm.hst.core.component;

import org.hippoecm.hst.core.container.HstContainerURL;
import org.hippoecm.hst.core.container.HstContainerURLProvider;
import org.hippoecm.hst.core.container.HstContainerURLProviderImpl;
import org.hippoecm.hst.core.request.HstRequestContext;

/**
 * HstURLFactoryImpl
 * 
 * @version $Id$
 */
public class HstURLFactoryImpl implements HstURLFactory {
    
    protected boolean referenceNamespaceIgnored;
    protected String urlNamespacePrefix;
    protected String parameterNameComponentSeparator;

    protected HstContainerURLProvider containerURLProvider;

    public void setUrlNamespacePrefix(String urlNamespacePrefix) {
        this.urlNamespacePrefix = urlNamespacePrefix;
    }
    
    public void setParameterNameComponentSeparator(String parameterNameComponentSeparator) {
        this.parameterNameComponentSeparator = parameterNameComponentSeparator;
    }
    
    public void setReferenceNamespaceIgnored(boolean referenceNamespaceIgnored) {
        this.referenceNamespaceIgnored = referenceNamespaceIgnored;
    }
    
    public boolean isReferenceNamespaceIgnored() {
        return referenceNamespaceIgnored;
    }
    

    public HstContainerURLProvider getContainerURLProvider() {
        if (containerURLProvider == null) {
            HstContainerURLProviderImpl provider = new HstContainerURLProviderImpl();
            provider.setUrlNamespacePrefix(urlNamespacePrefix);
            provider.setParameterNameComponentSeparator(parameterNameComponentSeparator);
            containerURLProvider = provider;
        }
        return containerURLProvider;
     }
    
    public HstURL createURL(String type, String referenceNamespace, HstContainerURL containerURL, HstRequestContext requestContext) {
        return createURL(type, referenceNamespace, containerURL, requestContext, null);
    }
    
    public HstURL createURL(String type, String referenceNamespace, HstContainerURL containerURL, HstRequestContext requestContext, String contextPath) {
        // if container url == null, use the requestContext baseUrl
        HstContainerURL baseContainerURL = (containerURL == null ? requestContext.getBaseURL() : containerURL);
        
        if (referenceNamespaceIgnored && HstURL.RENDER_TYPE.equals(type)) {
            referenceNamespace = "";
        }
        
        return new HstURLImpl(type, baseContainerURL, referenceNamespace, getContainerURLProvider(), requestContext, contextPath);
    }
}
