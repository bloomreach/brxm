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
package org.hippoecm.hst.mock.core.request;

import java.util.ArrayList;
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
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.content.beans.manager.ObjectBeanManager;
import org.hippoecm.hst.content.beans.query.HstQueryManager;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.tool.ContentBeansTool;
import org.hippoecm.hst.core.component.HstParameterInfoProxyFactory;
import org.hippoecm.hst.core.component.HstURLFactory;
import org.hippoecm.hst.core.container.ContainerConfiguration;
import org.hippoecm.hst.core.container.HstComponentWindowFilter;
import org.hippoecm.hst.core.container.HstContainerURL;
import org.hippoecm.hst.core.container.HstContainerURLProvider;
import org.hippoecm.hst.core.internal.HstMutableRequestContext;
import org.hippoecm.hst.core.linking.HstLinkCreator;
import org.hippoecm.hst.core.request.ContextCredentialsProvider;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.HstSiteMapMatcher;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.core.search.HstQueryManagerFactory;
import org.hippoecm.hst.core.sitemenu.HstSiteMenus;

public class MockHstRequestContext implements HstMutableRequestContext {


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
    private Map<String, Mount> aliasMountMap = new HashMap<String, Mount>();
    private Map<String, Mount> typeAndAliasMountMap = new HashMap<String, Mount>();
    private Set<String> componentFilterTags;
    private List<HstComponentWindowFilter> filters;
    private boolean fullyQualifiedURLs;
    private String renderHost;
    private boolean cmsRequest;
    private ContentBeansTool contentBeansTool;
    private boolean cachingObjectConverterEnabled;
    private HippoBean contentBean;
    private HippoBean siteContentBean;
    private String siteContentBasePath;

    private ObjectBeanManager defaultObjectBeanManager;
    private Map<Session, ObjectBeanManager> nonDefaultObjectBeanManagers;
    private HstQueryManager defaultHstQueryManager;
    private Map<Session, HstQueryManager>  nonDefaultHstQueryManagers;


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

    public ContextCredentialsProvider getContextCredentialsProvider() {
        return contextCredentialsProvider;
    }

    public void setContextCredentialsProvider(ContextCredentialsProvider contextCredentialsProvider) {
        this.contextCredentialsProvider = contextCredentialsProvider;
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
    public List<HstComponentWindowFilter> getComponentWindowFilters() {
        if (filters == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(filters);
    }

    /**
     * Adds the {@link HstComponentWindowFilter} to the {@link HstRequestContext}
     * @param filter the {@link HstComponentWindowFilter} to be added to the {@link HstRequestContext#getComponentWindowFilters()}
     */
    @Override
    public void addComponentWindowFilter(HstComponentWindowFilter filter) {
        if (filters == null) {
            filters = new ArrayList<HstComponentWindowFilter>();
        }
        filters.add(filter);
    }

    @Override
    public boolean isFullyQualifiedURLs() {
        return fullyQualifiedURLs;
    }

    @Override
    public String getRenderHost() {
        return renderHost;
    }

    @Override
    public boolean isCmsRequest() {
        return cmsRequest;
    }

    @Override
    public void setLinkCreator(HstLinkCreator linkCreator) {
        this.linkCreator = linkCreator;
    }

    @Override
    public void setParameterInfoProxyFactory(HstParameterInfoProxyFactory parameterInfoProxyFactory) {
        this.parameterInfoProxyFactory = parameterInfoProxyFactory;
    }

    @Override
    public void setFullyQualifiedURLs(boolean fullyQualifiedURLs) {
        this.fullyQualifiedURLs = fullyQualifiedURLs;
    }

    @Override
    public void setRenderHost(String renderHost) {
        this.renderHost = renderHost;
    }

    @Override
    public void setCmsRequest(boolean cmsRequest) {
        this.cmsRequest = cmsRequest;
    }

    @Override
    public HippoBean getContentBean() {
        return contentBean;
    }

    public void setContentBean(final HippoBean contentBean) {
        this.contentBean = contentBean;
    }

    public HippoBean getSiteContentBaseBean() {
        return siteContentBean;
    }

    public void setSiteContentBaseBean(final HippoBean siteContentBean) {
        this.siteContentBean = siteContentBean;
    }

    @Override
    public ContentBeansTool getContentBeansTool() {
        return contentBeansTool;
    }

    public void setContentBeansTool(final ContentBeansTool contentBeansTool) {
        this.contentBeansTool = contentBeansTool;
    }

    @Override
    public void setCachingObjectConverter(final boolean enabled) {
        this.cachingObjectConverterEnabled = enabled;
    }

    @Override
    public String getSiteContentBasePath() {
        return siteContentBasePath;
    }

    public void setSiteContentBasePath(final String siteContentBasePath) {
        this.siteContentBasePath = siteContentBasePath;
    }


    public void setDefaultObjectBeanManager(final ObjectBeanManager defaultObjectBeanManager) {
        this.defaultObjectBeanManager = defaultObjectBeanManager;
    }

    public void setNonDefaultObjectBeanManagers(final Map<Session, ObjectBeanManager> nonDefaultObjectBeanManagers) {
        this.nonDefaultObjectBeanManagers = nonDefaultObjectBeanManagers;
    }

    public void setDefaultHstQueryManager(final HstQueryManager defaultHstQueryManager) {
        this.defaultHstQueryManager = defaultHstQueryManager;
    }

    public void setNonDefaultHstQueryManagers(final Map<Session, HstQueryManager> nonDefaultHstQueryManagers) {
        this.nonDefaultHstQueryManagers = nonDefaultHstQueryManagers;
    }

    @Override
    public ObjectBeanManager getObjectBeanManager() throws IllegalStateException {
        return defaultObjectBeanManager;
    }

    @Override
    public ObjectBeanManager getObjectBeanManager(final Session session) throws IllegalStateException {
        return nonDefaultObjectBeanManagers.get(session);
    }

    @Override
    public HstQueryManager getQueryManager() throws IllegalStateException {
        return defaultHstQueryManager;
    }

    @Override
    public HstQueryManager getQueryManager(final Session session) throws IllegalStateException {
        return nonDefaultHstQueryManagers.get(session);
    }

    @Override
    public void clearObjectAndQueryManagers() {
    }
}