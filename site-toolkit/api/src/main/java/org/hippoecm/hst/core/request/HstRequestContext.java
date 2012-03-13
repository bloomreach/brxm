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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.jcr.LoginException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.security.auth.Subject;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.hosting.VirtualHost;
import org.hippoecm.hst.core.component.HstComponent;
import org.hippoecm.hst.core.component.HstParameterInfoProxyFactory;
import org.hippoecm.hst.core.component.HstURLFactory;
import org.hippoecm.hst.core.container.ContainerConfiguration;
import org.hippoecm.hst.core.container.HstComponentFactory;
import org.hippoecm.hst.core.container.HstComponentWindow;
import org.hippoecm.hst.core.container.HstComponentWindowFilter;
import org.hippoecm.hst.core.container.HstComponentWindowFactory;
import org.hippoecm.hst.core.container.HstContainerURL;
import org.hippoecm.hst.core.container.HstContainerURLProvider;
import org.hippoecm.hst.core.linking.HstLinkCreator;
import org.hippoecm.hst.core.parameters.ParametersInfo;
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
	 * Returns the ServletContext for this request 
     * @return the ServletContext for this request
	 **/
	ServletContext getServletContext();
	
    /** 
     * Returns the current HttpServletRequest
     * @return the current HttpServletRequest
     **/
    HttpServletRequest getServletRequest();

    /** 
     * Returns the current HttpServletResponse
     * @return the current HttpServletResponse
     **/
    HttpServletResponse getServletResponse();
    
    /**
     * Returns a session which is normally retrieved from a session pooling repository.
     * <p>Returns the current <code>javax.jcr.Session</code>
     * associated with this requestContext or, if if there is no
     * current JCR session, creates and returns a new JCR session.<p>
     * 
     * @return a session, which is normally retrieved from a session pooling repository
     * @throws LoginException
     * @throws RepositoryException
     */
    Session getSession() throws LoginException, RepositoryException;
    
    /**
     * Returns a session which can be retrieved from a session pooling repository.
     * <p>Returns the current <code>javax.jcr.Session</code>
     * associated with this requestContext or, if if there is no
     * current JCR session and <code>create</code> is true, returns 
     * a new JCR session.<p>
     * <p>If <code>create</code> is <code>false</code>
     * and the requestContext has no <code>javax.jcr.Session</code>,
     * this method returns <code>null</code>.
     * 
     * @return a session, which is normally retrieved from a session pooling repository
     * @throws LoginException
     * @throws RepositoryException
     */
    Session getSession(boolean create) throws LoginException, RepositoryException;
    
    /**
     * Returns the {@link ResolvedMount} for this request
     * @return the {@link ResolvedMount} for this request
     */
    ResolvedMount getResolvedMount();
    
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
     * @see Mount#isPreview()
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
     * Expert: Returns {@link HstParameterInfoProxyFactory} to create a proxy for an interface that is referred to by a {@link ParametersInfo} annotation
     * on a {@link HstComponent}
     * annotated interface getters 
     * @return the {@link HstParameterInfoProxyFactory} 
     */
    HstParameterInfoProxyFactory getParameterInfoProxyFactory();
    
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
     * Returns attribute map which is unmodifiable. So, do not try to put or remove items directly from the returned map.
     * @return
     */
    Map<String, Object> getAttributes();
    
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
     * Returns true if this request is embedded and link rewriting needs to use the {@link #getResolvedEmbeddingMount()}
     * for the target Mount path and context path (if to be included).
     */
    boolean isEmbeddedRequest();
    
    /**
     * Returns the contextPath of the embedding application for an embedded request, otherwise null
     * @see HstRequestContext#isEmbeddedRequest()
     */
    String getEmbeddingContextPath();
    
    /**
     * Returns the {@link ResolvedMount} to be used for link rewriting when this request is embedded, otherwise null
     * @see HstRequestContext#isEmbeddedRequest()
     */
    ResolvedMount getResolvedEmbeddingMount();
    
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
    
    /**
     * Gets the subject associated with the authorized entity.
     * @return The JAAS subject on this request.
     */
    Subject getSubject();
    
    /**
     * Gets the preferred locale associated with this request.
     *
     * @return The preferred locale associated with this request.
     */
    Locale getPreferredLocale();
    
    /**
     * Returns an Enumeration of Locale objects
     * @return The locale associated with this request.
     */
    Enumeration<Locale> getLocales();
    
    /**
     * Returns the path suffix from the resolved site map item.
     * If it is null, then returns the path suffix from the resolved Mount.
     * @return the matched path suffix and <code>null</code> if there is no path suffix
     */
    String getPathSuffix();
    
    /**
     * <p>
     * a mount with {@link Mount#getAlias()} equal to <code>alias</code> and at least one common type with the mount from the current request. Thus, at least 
     * one of the types of the found {@link Mount#getTypes()} must be equal to one of the types of the mount of the current request. 
     * </p>
     * <p>
     * If there can be found a {@link Mount} with the same primary type ( {@link Mount#getType()} ) as the one for the mount of the current request, this
     * {@link Mount} has precedence. If there is no primary type match, we'll return the mount that has most types in common
     * </p>
     * <p>
     * There will be looked if the {@link Mount} of the current {@link HstRequestContext}
     * has a property that is called <code>hst:mountXXX</code> where <code>XXX</code> is equal to  <code>alias</code>. If so, there will be tried
     * to return a {@link Mount} that has an alias equal to the value of this mappedAlias property <code>hst:mountXXX</code>. 
     * If there cannot be found a {@link Mount} for via a mapped <code>hst:mountXXX</code> property, there will be looked for a {@link Mount} with
     * which has {@link Mount#getAlias()} equal to <code>alias</code>.
     * <b>Thus</b> a mapped alias has <b>precedence</b>!
     * </p>
     * @param alias the alias the found {@link Mount} or XXX in hst:mountXXX property
     * @return a mount with {@link Mount#getAlias()} equal to <code>alias</code> or mappedAlias and at least one common type with the mount from the current request. <code>null</code> if there is no suitable mount.
     * @throws IllegalArgumentException when <code>alias</code> is <code>null</code>
     */
    Mount getMount(String alias);
    
    /**
     * <p>
     * a mount with {@link Mount#getAlias()} equal to <code>alias</code> and one of its {@link Mount#getTypes()}  equal to <code>type</code>.
     * </p>
     * <p>
     * There will be looked if the {@link Mount} of the current {@link HstRequestContext} has a property that is called <code>hst:mountXXX</code> where <code>XXX</code> is equal to  <code>alias</code>. 
     * If so, there will be tried to return a {@link Mount} that has an alias equal to the value of this mappedAlias property <code>hst:mountXXX</code>. If there cannot be found a {@link Mount} for via a mapped <code>hst:mountXXX</code> property, 
     * there will be looked for a {@link Mount} with which has {@link Mount#getAlias()} equal to <code>alias</code>.
     * </p>
     * 
     * @param alias the alias the found {@link Mount}  or or XXX in hst:mountXXX property
     * @param type the type the found {@link Mount} should have
     * @return a mount with {@link Mount#getAlias()} equal to <code>alias</code> or mappedAlias and one of its {@link Mount#getTypes()} equal to <code>type</code>. <code>null</code> if there is no suitable mount.
     * @throws IllegalArgumentException when <code>alias</code> or <code>type</code> is <code>null</code>
     */
    Mount getMount(String alias, String type);
    
    /**
     * <p>
     * <b>Expert:</b> The tags that will be used to render container items. These tags can be used by a {@link HstComponentFactory} 
     * implementation to decide to load some specific {@link HstComponentWindow}s only.
     * </p>
     * <p>
     * This method is in general not useful for frontend developers and is more targetted for the HST Core
     * </p>
     * <p>
     * HST Core {@link HstComponentFactory} implementations behave as follows: 
     * When tags are available, and there is a container item in a container that matches the tag,
     * those container items will be rendered at the exclusion of the other items.
     * If no tags are provided, or none matches any of the tags on the container items,
     * only those container items that do not have a tag will be rendered.
     * </p>
     *
     * @return The (immutable) active set of filter tags, empty when conditional rendering is not used.
     */
    Set<String> getComponentFilterTags();

    /** 
     * <p>
     * <b>Expert:</b> Option to add filters to the {@link HstRequestContext}. Note that this only affects outcome if it is done 
     * before the actual doBeforeRender / doAction etc or rendering is invoked by the HST
     * </p>
     * @return the (immutable) {@link List} of {@link HstComponentWindowFilter}s and if none present, return a empty List
     */
    List<HstComponentWindowFilter> getComponentWindowFilters();
    
    /**
     * @return <code>true</code> when all URLs must be fully qualified, ie, including scheme, domain and portnumber (if present)
     */
    boolean isFullyQualifiedURLs();
    
    /**
     * 
     * @return the render host to use for rendering the request  or <code>null</code> when no specific render host is defined. Typically,
     * there is only a render host when the request originated from the CMS 
     *
     */
    String getRenderHost();
    
}
