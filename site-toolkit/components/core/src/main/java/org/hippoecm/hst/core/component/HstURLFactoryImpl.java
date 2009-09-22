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
import org.hippoecm.hst.core.container.HstContainerURLProviderImpl;
import org.hippoecm.hst.core.container.HstContainerURLProviderPortletImpl;
import org.hippoecm.hst.core.container.HstEmbeddedPortletContainerURLProviderImpl;
import org.hippoecm.hst.core.container.HstNavigationalStateCodec;
import org.hippoecm.hst.core.request.HstRequestContext;

public class HstURLFactoryImpl implements HstURLFactory {
    
    protected boolean referenceNamespaceIgnored;
    protected String urlNamespacePrefix;
    protected String parameterNameComponentSeparator;
    protected HstNavigationalStateCodec navigationalStateCodec;
    protected boolean portletResourceURLEnabled;

    protected HstContainerURLProvider servletURLProvider;
    protected HstContainerURLProvider portletURLProvider;
    protected HstContainerURLProvider embeddedPortletURLProvider;


    public void setUrlNamespacePrefix(String urlNamespacePrefix) {
        this.urlNamespacePrefix = urlNamespacePrefix;
    }
    
    public void setParameterNameComponentSeparator(String parameterNameComponentSeparator) {
        this.parameterNameComponentSeparator = parameterNameComponentSeparator;
    }
    
    public void setNavigationalStateCodec(HstNavigationalStateCodec navigationalStateCodec) {
        this.navigationalStateCodec = navigationalStateCodec;
    }
    
    public void setPortletResourceURLEnabled(boolean portletResourceURLEnabled) {
        this.portletResourceURLEnabled = portletResourceURLEnabled;
    }
    
    public void setReferenceNamespaceIgnored(boolean referenceNamespaceIgnored) {
        this.referenceNamespaceIgnored = referenceNamespaceIgnored;
    }
    
    public boolean isReferenceNamespaceIgnored() {
        return referenceNamespaceIgnored;
    }
    
    public HstContainerURLProvider getContainerURLProvider(HstRequestContext requestContext) {
        return requestContext.isPortletContext() ? requestContext.isEmbeddedRequest() ? getEmbeddedPortletURLProvider() : getPortletURLProvider() : getServletURLProvider();
    }
    
    public HstURL createURL(String type, String referenceNamespace, HstContainerURL containerURL, HstRequestContext requestContext) {
        // if container url == null, use the requestContext baseUrl
        HstContainerURL baseContainerURL = (containerURL == null ? requestContext.getBaseURL() : containerURL);
        
        if (referenceNamespaceIgnored && HstURL.RENDER_TYPE.equals(type)) {
            referenceNamespace = "";
        }
        
        return new HstURLImpl(type, baseContainerURL, referenceNamespace, getContainerURLProvider(requestContext), requestContext);
    }

    protected HstContainerURLProvider getServletURLProvider() {
        if (servletURLProvider == null) {
            HstContainerURLProviderImpl provider = new HstContainerURLProviderImpl();
            provider.setUrlNamespacePrefix(urlNamespacePrefix);
            provider.setParameterNameComponentSeparator(parameterNameComponentSeparator);
            provider.setNavigationalStateCodec(navigationalStateCodec);
            servletURLProvider = provider;
        }
        
        return servletURLProvider;
    }
    
    protected HstContainerURLProvider getPortletURLProvider() {
        if (portletURLProvider == null) {
            HstContainerURLProviderPortletImpl provider = new HstContainerURLProviderPortletImpl();
            provider.setUrlNamespacePrefix(urlNamespacePrefix);
            provider.setParameterNameComponentSeparator(parameterNameComponentSeparator);
            provider.setNavigationalStateCodec(navigationalStateCodec);
            provider.setPortletResourceURLEnabled(portletResourceURLEnabled);
            portletURLProvider = provider;
        }
        
        return portletURLProvider;
    }

    protected HstContainerURLProvider getEmbeddedPortletURLProvider() {
        if (embeddedPortletURLProvider == null) {
            HstEmbeddedPortletContainerURLProviderImpl provider = new HstEmbeddedPortletContainerURLProviderImpl();
            provider.setUrlNamespacePrefix(urlNamespacePrefix);
            provider.setParameterNameComponentSeparator(parameterNameComponentSeparator);
            provider.setNavigationalStateCodec(navigationalStateCodec);
            provider.setPortletResourceURLEnabled(portletResourceURLEnabled);
            embeddedPortletURLProvider = provider;
        }
        
        return embeddedPortletURLProvider;
    }
}
