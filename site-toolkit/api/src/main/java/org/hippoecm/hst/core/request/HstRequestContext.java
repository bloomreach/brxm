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
package org.hippoecm.hst.core.request;

import java.util.Enumeration;

import javax.jcr.LoginException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.hst.configuration.hosting.SiteMount;
import org.hippoecm.hst.configuration.hosting.VirtualHost;
import org.hippoecm.hst.core.component.HstURLFactory;
import org.hippoecm.hst.core.container.ContainerConfiguration;
import org.hippoecm.hst.core.container.HstContainerURL;
import org.hippoecm.hst.core.container.HstContainerURLProvider;
import org.hippoecm.hst.core.linking.HstLinkCreator;
import org.hippoecm.hst.core.search.HstQueryManagerFactory;
import org.hippoecm.hst.core.sitemenu.HstSiteMenus;

/**
 * HstRequestContext provides repository content context
 * and page/components configuration context.
 * Also, HstRequestContext is shared among all the HstComponent windows in a request lifecycle.
 * 
 * @version $Id$
 */
public interface HstRequestContext {
    
    /**
     * Returns a session which is normally retrieved from a session pooling repository.
     * 
     * @return a session, which is normally retrieved from a session pooling repository
     * @throws LoginException
     * @throws RepositoryException
     */
    Session getSession() throws LoginException, RepositoryException;
    
    /**
     * Returns the {@link ResolvedSiteMount} for this request
     * @return the resolvedSiteMount for this request
     */
    ResolvedSiteMount getResolvedSiteMount();
    
    /**
     * Returns the {@link ResolvedSiteMapItem} for this request
     * @return the resolvedSiteMapItem for this request
     */
    ResolvedSiteMapItem getResolvedSiteMapItem();
    
    /**
     * Returns a target component path relative to {@link HstComponentConfiguration} of the {@link #getResolvedSiteMapItem().
     * If not null the targeted sub component configuration will be used as root component for this request instead.
     */
    String getTargetComponentPath();
    
    /**
     * @return <code>true</code> when this request is matched to a preview site
     * @see SiteMount#isPreview()
     */
    boolean isPreview();
    
    /**
     * Returns the context namespace. If there are multiple HstContainer based applications,
     * it could be necessary to separate the component window's namespaces.
     * This context namespace can be used for the purpose.
     * 
     * @return
     */
    String getContextNamespace();
    
    /**
     * Returns the base container URL ({@link HstContainerURL} ) of the current request lifecycle.
     * 
     * @return HstContainerURL
     */
    HstContainerURL getBaseURL();
    
    /**
     * Returns the {@link HstURLFactory} to create HstURLs
     * 
     * @return HstURLFactory
     */
    HstURLFactory getURLFactory();

    /**
     * Returns the {@link HstContainerURLProvider} to create HstContainerURLs
     * 
     * @return HstContainerURLProvider
     */
    HstContainerURLProvider getContainerURLProvider();

    /**
     * Returns the {@link HstSiteMapMatcher} to be able to match a path to a sitemap item
     * @return HstSiteMapMatcher
     */
    HstSiteMapMatcher getSiteMapMatcher();
    
    /**
     * Returns the {@link HstLinkCreator} to create navigational links
     * 
     * @return HstLinkCreator
     */
    HstLinkCreator getHstLinkCreator();
    
    /**
     * 
     * @return the HstSiteMenus
     */
    HstSiteMenus getHstSiteMenus();
        
    /**
     * Returns a {@link HstQueryManagerFactory} instance responsible for creating a query manager
     * @return HstQueryManagerFactory
     */
    HstQueryManagerFactory getHstQueryManagerFactory();
    
    /**
     * Set an attribute to be shared among each HstComponent windows.
     * Because this attribute is not prefixed by the reference namespace of the HstComponent window,
     * this method can be used if the attribute is to be shared among HstComponent windows.
     * 
     * @param name attribute name
     * @param object attribute value
     */
    void setAttribute(String name, Object object);
    
    /**
     * Retrieve the attribute value by the attribute name.
     * Because this attribute is not prefixed by the reference namespace of the HstComponent window,
     * this method can be used if the attribute is to be shared among HstComponent windows.
     */
    Object getAttribute(String name);
    
    /**
     * Removes the attribute by the attribute name.
     */
    void removeAttribute(String name);
    
    /**
     * Enumerates the attribute names
     */
    Enumeration<String> getAttributeNames();
    
    /**
     * Returns the matched virtual host object 
     * @return
     */
    VirtualHost getVirtualHost();
    
    /**
     * Returns the container configuration
     * @return
     */
    ContainerConfiguration getContainerConfiguration();
    
    /**
     * Returns true if this request is embedded and link rewriting needs to use the {@link #getResolvedEmbeddingSiteMount()}
     * for the target site mount path and context path (if to be included).
     */
    boolean isEmbeddedRequest();
    
    /**
     * Returns the contextPath of the embedding application for an embedded request, otherwise null
     * @see HstRequestContext#isEmbeddedRequest()
     */
    String getEmbeddingContextPath();
    
    /**
     * Returns the ResolvedSiteMount to be used for link rewriting when this request is embedded, otherwise null
     * @see HstRequestContext#isEmbeddedRequest()
     */
    ResolvedSiteMount getResolvedEmbeddingSiteMount();
    
    /**
     * Returns true if invoked from a Portlet.
     * If true, this instance will also implement HstPortletRequestContext.
     */
    boolean isPortletContext();
    
    /**
     * Returns the context credentials provider
     * @return
     */
    ContextCredentialsProvider getContextCredentialsProvider();
    
}
