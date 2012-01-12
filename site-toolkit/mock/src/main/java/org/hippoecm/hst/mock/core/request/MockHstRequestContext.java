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
package org.hippoecm.hst.mock.core.request;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.security.auth.Subject;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.hosting.VirtualHost;
import org.hippoecm.hst.core.component.HstParameterInfoProxyFactory;
import org.hippoecm.hst.core.component.HstURLFactory;
import org.hippoecm.hst.core.container.ContainerConfiguration;
import org.hippoecm.hst.core.container.HstContainerURL;
import org.hippoecm.hst.core.container.HstContainerURLProvider;
import org.hippoecm.hst.core.linking.HstLinkCreator;
import org.hippoecm.hst.core.request.ContextCredentialsProvider;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.HstSiteMapMatcher;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.core.search.HstQueryManagerFactory;
import org.hippoecm.hst.core.sitemenu.HstSiteMenus;

public class MockHstRequestContext implements HstRequestContext {
    
    private Hashtable<String, Object> attributes = new Hashtable<String, Object>();
    private ServletContext servletContext;
    private HttpServletRequest servletRequest;
    private HttpServletResponse servletResponse;
    private Session session;
    private HstContainerURL baseURL;
    private String contextNamespace;
    private HstURLFactory urlFactory;
    private ResolvedMount resolvedMount;
    private ResolvedSiteMapItem resolvedSiteMapItem;
    private HstLinkCreator linkCreator;
    private HstParameterInfoProxyFactory parameterInfoProxyFactory;
    private HstSiteMapMatcher siteMapMatcher;
    private HstSiteMenus siteMenus;
    private HstQueryManagerFactory hstQueryManagerFactory;
    private Credentials defaultCredentials;
    private ContainerConfiguration containerConfiguration;
    private ContextCredentialsProvider contextCredentialsProvider;
    private Subject subject;
    private Locale preferredLocale;
    private List<Locale> locales;
    private String pathSuffix;
    private VirtualHost virtualHost;
    private boolean embeddedRequest;
    private boolean portletRequest;
    private String embeddingContextPath;
    private ResolvedMount resolvedEmbeddingMount;
    private String targetComponentPath;
    private Map<String, Mount> aliasMountMap = new HashMap<String, Mount>();
    private Map<String, Mount> typeAndAliasMountMap = new HashMap<String, Mount>();
    private Set<String> componentFilterTags;   
    protected boolean fullyQualifiedURLs;
    protected String renderHost;

    public boolean isPreview() {
    	return this.resolvedMount.getMount().isPreview();
    }
    
    public ServletContext getServletContext() {
    	return servletContext;
    }
    
    public void setServletContext(ServletContext servletContext) {
    	this.servletContext = servletContext;
    }
    
    public HttpServletRequest getServletRequest() {
        return servletRequest;
    }

    public void setServletRequest(HttpServletRequest servletRequest) {
        this.servletRequest = servletRequest;
    }

    public HttpServletResponse getServletResponse() {
        return servletResponse;
    }

    public void setServletResponse(HttpServletResponse servletResponse) {
        this.servletResponse = servletResponse;
    }

    public Object getAttribute(String name) {
        return this.attributes.get(name);
    }
    
    public Enumeration<String> getAttributeNames() {
        return this.attributes.keys();
    }
    
    public Map<String, Object> getAttributes() {
        return Collections.unmodifiableMap(attributes);
    }

    public HstContainerURL getBaseURL() {
        return this.baseURL;
    }
    
    public void setBaseURL(HstContainerURL baseURL) {
        this.baseURL = baseURL;
    }
    
    public String getContextNamespace() {
        return this.contextNamespace;
    }
    
    public void setContextNamespace(String contextNamespace) {
        this.contextNamespace = contextNamespace;
    }
    
    public Credentials getDefaultCredentials() {
        return defaultCredentials;
    }
    
    public void setDefaultCredentials(Credentials defaultCredentials) {
        this.defaultCredentials = defaultCredentials;
    }
    
    public HstSiteMapMatcher getSiteMapMatcher(){
        return this.siteMapMatcher;
    }
    
    public void setSiteMapMatcher(HstSiteMapMatcher siteMapMatcher){
        this.siteMapMatcher = siteMapMatcher;
    }
    
    public HstLinkCreator getHstLinkCreator() {
        return this.linkCreator;
    }
    
    public void setHstLinkCreator(HstLinkCreator linkCreator) {
        this.linkCreator = linkCreator;
    }
    
    public HstQueryManagerFactory getHstQueryManagerFactory() {
        return hstQueryManagerFactory;
    }

    public void setHstQueryManagerFactory(HstQueryManagerFactory hstQueryManagerFactory) {
        this.hstQueryManagerFactory = hstQueryManagerFactory;
    }
    
    public void setHstSiteMenus(HstSiteMenus siteMenus) {
        this.siteMenus = siteMenus;
    }
    
    public HstSiteMenus getHstSiteMenus(){
        return this.siteMenus;
    }
    
    public ResolvedMount getResolvedMount() {
        return this.resolvedMount;
    }
    
    public void setResolvedMount(ResolvedMount resolvedMount) {
        this.resolvedMount = resolvedMount;
    }
    
    public ResolvedSiteMapItem getResolvedSiteMapItem() {
        return this.resolvedSiteMapItem;
    }
    
    public void setResolvedSiteMapItem(ResolvedSiteMapItem resolvedSiteMapItem) {
        this.resolvedSiteMapItem = resolvedSiteMapItem;
    }
    
    public void setSession(Session session) {
        this.session = session;
    }
    
    public Session getSession() throws LoginException, RepositoryException {
        return this.session;
    }
    
    public Session getSession(boolean create) throws LoginException, RepositoryException {
        return this.session;
    }
    
    public HstURLFactory getURLFactory() {
        return this.urlFactory;
    }
    
    public void setURLFactory(HstURLFactory urlFactory) {
        this.urlFactory = urlFactory;
    }
    
    @Override
    public HstParameterInfoProxyFactory getParameterInfoProxyFactory() {
        return parameterInfoProxyFactory;
    }
    
    
    public HstContainerURLProvider getContainerURLProvider() {
        return urlFactory != null ? urlFactory.getContainerURLProvider() : null;
    }

    public void removeAttribute(String name) {
        this.attributes.remove(name);
    }
    
    public void setAttribute(String name, Object value) {
        if (value == null) {
            removeAttribute(name);
        } else {
            this.attributes.put(name, value);
        }
    }

    public ContainerConfiguration getContainerConfiguration() {
        return this.containerConfiguration;
    }
    
    public void setContainerConfiguration(ContainerConfiguration containerConfiguration) {
        this.containerConfiguration = containerConfiguration;
    }

    public VirtualHost getVirtualHost() {
        return virtualHost;
    }
    
    public void setVirtualHost(VirtualHost virtualHost) {
        this.virtualHost = virtualHost;
    }

    public boolean isEmbeddedRequest() {
        return embeddedRequest;
    }
    
    public void setEmbeddedRequest(boolean embeddedRequest) {
        this.embeddedRequest = embeddedRequest;
    }

    public boolean isPortletContext() {
        return portletRequest;
    }
    
    public void setPortletContext(boolean portletRequest) {
        this.portletRequest = portletRequest;
    }
    
    public ContextCredentialsProvider getContextCredentialsProvider() {
        return contextCredentialsProvider;
    }
    
    public void setContextCredentialsProvider(ContextCredentialsProvider contextCredentialsProvider) {
        this.contextCredentialsProvider = contextCredentialsProvider;
    }

    public String getEmbeddingContextPath() {
    	return embeddingContextPath;
    }
    
    public void setEmbeddingContextPath(String embeddingContextPath) {
        this.embeddingContextPath = embeddingContextPath;
    }
    
	public ResolvedMount getResolvedEmbeddingMount() {
		return resolvedEmbeddingMount;
	}
	
    public void setResolvedEmbeddingMount(ResolvedMount resolvedEmbeddingMount) {
        this.resolvedEmbeddingMount = resolvedEmbeddingMount;
    }

	public String getTargetComponentPath() {
		return targetComponentPath;
	}
	
	public void setTargetComponentPath(String targetComponentPath) {
	    this.targetComponentPath = targetComponentPath;
	}

    public Subject getSubject() {
        return subject;
    }
    
    public void setSubject(Subject subject) {
        this.subject = subject;
    }
    
    public Locale getPreferredLocale() {
        return preferredLocale;
    }
    
    public void setPreferredLocale(Locale preferredLocale) {
        this.preferredLocale = preferredLocale;
    }
    
    public Enumeration<Locale> getLocales() {
        return Collections.enumeration(locales);
    }
    
    public void setLocales(List<Locale> locales) {
        this.locales = locales;
    }
    
    public void setPathSuffix(String pathSuffix) {
        this.pathSuffix = pathSuffix;
    }
    
    public String getPathSuffix() {
        return pathSuffix;
    }

    public Mount getMount(String alias) {
        if (aliasMountMap.containsKey(alias)) {
            return aliasMountMap.get(alias);
        }
        
        return null;
    }
    
    public void addMount(String alias, Mount mount) {
        aliasMountMap.put(alias, mount);
    }

    public void removeMount(String alias) {
        aliasMountMap.remove(alias);
    }
    
    public Mount getMount(String type, String alias) {
        String key = alias + '\uFFFF' + type;
        
        if (typeAndAliasMountMap.containsKey(key)) {
            return typeAndAliasMountMap.get(key);
        }
        
        return null;
    }
    
    public void addMount(String type, String alias, Mount mount) {
        String key = alias + '\uFFFF' + type;
        typeAndAliasMountMap.put(key, mount);
    }

    public void removeMount(String type, String alias) {
        String key = alias + '\uFFFF' + type;
        typeAndAliasMountMap.remove(key);
    }

    public void setComponentFilterTags(final Set<String> componentFilterTags) {
        this.componentFilterTags = componentFilterTags;
    }

    public Set<String> getComponentFilterTags() {
        if (componentFilterTags == null) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(componentFilterTags);
    }
    
    @Override
    public boolean isFullyQualifiedURLs() {
        return fullyQualifiedURLs;
    }
    
    @Override
    public String getRenderHost() {
        return renderHost;
    }

}