/*
 *  Copyright 2008-2016 Hippo B.V. (http://www.onehippo.com)
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
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedHashMap;
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
import org.hippoecm.hst.content.beans.manager.ObjectBeanManager;
import org.hippoecm.hst.content.beans.manager.ObjectConverter;
import org.hippoecm.hst.content.beans.query.HstQueryManager;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.tool.ContentBeansTool;
import org.hippoecm.hst.core.component.HstParameterInfoProxyFactory;
import org.hippoecm.hst.core.component.HstURLFactory;
import org.hippoecm.hst.core.container.ContainerConfiguration;
import org.hippoecm.hst.core.container.HeadContributable;
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
import org.hippoecm.hst.core.sitemenu.HstSiteMenusManager;
import org.hippoecm.hst.mock.util.IteratorEnumeration;

public class MockHstRequestContext implements HstMutableRequestContext {


    private Hashtable<String, Object> attributes = new Hashtable<String, Object>();
    private ServletContext servletContext;
    private HttpServletRequest servletRequest;
    private HttpServletResponse servletResponse;
    private Session session;
    private HstContainerURL baseURL;
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
    private HstSiteMenusManager siteMenusManager;

    private ObjectBeanManager defaultObjectBeanManager;
    private Map<Session, ObjectBeanManager> nonDefaultObjectBeanManagers;
    private HstQueryManager defaultHstQueryManager;
    private Map<Session, HstQueryManager> nonDefaultHstQueryManagers;

    private Map<String, Object> modelsMap = new HashMap<String, Object>();
    private Map<String, Object> unmodifiableModelsMap = Collections.unmodifiableMap(modelsMap);

    private Map<String, HeadContributable> headContributablesMap;

    private boolean disposed;

    public boolean isPreview() {
        checkStateValidity();
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

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getModel(String name) {
        return (T) getModelsMap().get(name);
    }

    @Override
    public Iterable<String> getModelNames() {
        return Collections.unmodifiableSet(getModelsMap().keySet());
    }

    @Override
    public Map<String, Object> getModelsMap() {
        return unmodifiableModelsMap;
    }

    @Override
    public Object setModel(String name, Object model) {
        return modelsMap.put(name, model);
    }

    @Override
    public void removeModel(String name) {
        modelsMap.remove(name);
    }

    public Object getAttribute(String name) {
        checkStateValidity();
        Object val = this.attributes.get(name);
        if (val != null) {
            return val;
        }
        return getModel(name);
    }

    public Enumeration<String> getAttributeNames() {
        checkStateValidity();
        Set<String> mergedAttrs = new HashSet<>(getModelsMap().keySet());
        if (attributes != null) {
            mergedAttrs.addAll(attributes.keySet());
        }
        return new IteratorEnumeration<String>(mergedAttrs.iterator());
    }

    public Map<String, Object> getAttributes() {
        checkStateValidity();
        return Collections.unmodifiableMap(attributes);
    }

    public HstContainerURL getBaseURL() {
        checkStateValidity();
        return this.baseURL;
    }

    public void setBaseURL(HstContainerURL baseURL) {
        checkStateValidity();
        this.baseURL = baseURL;
    }

    public Credentials getDefaultCredentials() {
        checkStateValidity();
        return defaultCredentials;
    }

    public void setDefaultCredentials(Credentials defaultCredentials) {
        checkStateValidity();
        this.defaultCredentials = defaultCredentials;
    }

    public HstSiteMapMatcher getSiteMapMatcher() {
        checkStateValidity();
        return this.siteMapMatcher;
    }

    public void setSiteMapMatcher(HstSiteMapMatcher siteMapMatcher) {
        checkStateValidity();
        this.siteMapMatcher = siteMapMatcher;
    }

    public HstLinkCreator getHstLinkCreator() {
        checkStateValidity();
        return this.linkCreator;
    }

    public void setHstLinkCreator(HstLinkCreator linkCreator) {
        checkStateValidity();
        this.linkCreator = linkCreator;
    }

    public HstQueryManagerFactory getHstQueryManagerFactory() {
        checkStateValidity();
        return hstQueryManagerFactory;
    }

    public void setHstQueryManagerFactory(HstQueryManagerFactory hstQueryManagerFactory) {
        checkStateValidity();
        this.hstQueryManagerFactory = hstQueryManagerFactory;
    }

    public void setHstSiteMenus(HstSiteMenus siteMenus) {
        checkStateValidity();
        this.siteMenus = siteMenus;
    }

    public HstSiteMenus getHstSiteMenus() {
        checkStateValidity();
        return this.siteMenus;
    }

    public ResolvedMount getResolvedMount() {
        checkStateValidity();
        return this.resolvedMount;
    }

    public void setResolvedMount(ResolvedMount resolvedMount) {
        checkStateValidity();
        this.resolvedMount = resolvedMount;
    }

    public ResolvedSiteMapItem getResolvedSiteMapItem() {
        checkStateValidity();
        return this.resolvedSiteMapItem;
    }

    public void setResolvedSiteMapItem(ResolvedSiteMapItem resolvedSiteMapItem) {
        checkStateValidity();
        this.resolvedSiteMapItem = resolvedSiteMapItem;
    }

    public void setSession(Session session) {
        checkStateValidity();
        this.session = session;
    }

    public Session getSession() throws LoginException, RepositoryException {
        checkStateValidity();
        return this.session;
    }

    public Session getSession(boolean create) throws LoginException, RepositoryException {
        checkStateValidity();
        return this.session;
    }

    public HstURLFactory getURLFactory() {
        checkStateValidity();
        return this.urlFactory;
    }

    public void setURLFactory(HstURLFactory urlFactory) {
        checkStateValidity();
        this.urlFactory = urlFactory;
    }

    @Override
    public HstParameterInfoProxyFactory getParameterInfoProxyFactory() {
        checkStateValidity();
        return parameterInfoProxyFactory;
    }

    public HstContainerURLProvider getContainerURLProvider() {
        checkStateValidity();
        return urlFactory != null ? urlFactory.getContainerURLProvider() : null;
    }

    public void removeAttribute(String name) {
        checkStateValidity();
        this.attributes.remove(name);
    }

    public void setAttribute(String name, Object value) {
        checkStateValidity();
        if (value == null) {
            removeAttribute(name);
        } else {
            this.attributes.put(name, value);
        }
    }

    public ContainerConfiguration getContainerConfiguration() {
        checkStateValidity();
        return this.containerConfiguration;
    }

    public void setContainerConfiguration(ContainerConfiguration containerConfiguration) {
        checkStateValidity();
        this.containerConfiguration = containerConfiguration;
    }

    public VirtualHost getVirtualHost() {
        checkStateValidity();
        return virtualHost;
    }

    public void setVirtualHost(VirtualHost virtualHost) {
        checkStateValidity();
        this.virtualHost = virtualHost;
    }

    public ContextCredentialsProvider getContextCredentialsProvider() {
        checkStateValidity();
        return contextCredentialsProvider;
    }

    public void setContextCredentialsProvider(ContextCredentialsProvider contextCredentialsProvider) {
        checkStateValidity();
        this.contextCredentialsProvider = contextCredentialsProvider;
    }

    public Subject getSubject() {
        checkStateValidity();
        return subject;
    }

    public void setSubject(Subject subject) {
        checkStateValidity();
        this.subject = subject;
    }

    public Locale getPreferredLocale() {
        checkStateValidity();
        return preferredLocale;
    }

    public void setPreferredLocale(Locale preferredLocale) {
        checkStateValidity();
        this.preferredLocale = preferredLocale;
    }

    public Enumeration<Locale> getLocales() {
        checkStateValidity();
        return Collections.enumeration(locales);
    }

    public void setLocales(List<Locale> locales) {
        checkStateValidity();
        this.locales = locales;
    }

    public void setPathSuffix(String pathSuffix) {
        checkStateValidity();
        this.pathSuffix = pathSuffix;
    }

    public String getPathSuffix() {
        checkStateValidity();
        return pathSuffix;
    }

    public Mount getMount(String alias) {
        checkStateValidity();
        if (aliasMountMap.containsKey(alias)) {
            return aliasMountMap.get(alias);
        }

        return null;
    }

    public void addMount(String alias, Mount mount) {
        checkStateValidity();
        aliasMountMap.put(alias, mount);
    }

    public void removeMount(String alias) {
        checkStateValidity();
        aliasMountMap.remove(alias);
    }

    public Mount getMount(String type, String alias) {
        checkStateValidity();
        String key = alias + '\uFFFF' + type;

        if (typeAndAliasMountMap.containsKey(key)) {
            return typeAndAliasMountMap.get(key);
        }

        return null;
    }

    public void addMount(String type, String alias, Mount mount) {
        checkStateValidity();
        String key = alias + '\uFFFF' + type;
        typeAndAliasMountMap.put(key, mount);
    }

    public void removeMount(String type, String alias) {
        checkStateValidity();
        String key = alias + '\uFFFF' + type;
        typeAndAliasMountMap.remove(key);
    }

    public void setComponentFilterTags(final Set<String> componentFilterTags) {
        checkStateValidity();
        this.componentFilterTags = componentFilterTags;
    }

    public Set<String> getComponentFilterTags() {
        checkStateValidity();
        if (componentFilterTags == null) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(componentFilterTags);
    }

    @Override
    public List<HstComponentWindowFilter> getComponentWindowFilters() {
        checkStateValidity();
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
        checkStateValidity();
        if (filters == null) {
            filters = new ArrayList<HstComponentWindowFilter>();
        }
        filters.add(filter);
    }

    @Override
    public void setComponentWindowFilters(final List<HstComponentWindowFilter> filters) {
        this.filters = filters;
    }

    @Override
    public boolean isFullyQualifiedURLs() {
        checkStateValidity();
        return fullyQualifiedURLs;
    }

    @Override
    public String getRenderHost() {
        checkStateValidity();
        return renderHost;
    }

    @Override
    public boolean isCmsRequest() {
        checkStateValidity();
        return cmsRequest;
    }

    @Override
    public void setLinkCreator(HstLinkCreator linkCreator) {
        checkStateValidity();
        this.linkCreator = linkCreator;
    }

    @Override
    public void setParameterInfoProxyFactory(HstParameterInfoProxyFactory parameterInfoProxyFactory) {
        checkStateValidity();
        this.parameterInfoProxyFactory = parameterInfoProxyFactory;
    }

    @Override
    public void setFullyQualifiedURLs(boolean fullyQualifiedURLs) {
        checkStateValidity();
        this.fullyQualifiedURLs = fullyQualifiedURLs;
    }

    @Override
    public void setRenderHost(String renderHost) {
        checkStateValidity();
        this.renderHost = renderHost;
    }

    @Override
    public void setCmsRequest(boolean cmsRequest) {
        checkStateValidity();
        this.cmsRequest = cmsRequest;
    }

    @Override
    public HippoBean getContentBean() {
        checkStateValidity();
        return contentBean;
    }

    @Override
    public <T extends HippoBean> T getContentBean(final Class<T> beanMappingClass) {
        checkStateValidity();
        return (T)getContentBean();
    }

    public void setContentBean(final HippoBean contentBean) {
        checkStateValidity();
        this.contentBean = contentBean;
    }

    public HippoBean getSiteContentBaseBean() {
        checkStateValidity();
        return siteContentBean;
    }

    public void setSiteContentBaseBean(final HippoBean siteContentBean) {
        checkStateValidity();
        this.siteContentBean = siteContentBean;
    }

    @Override
    public ContentBeansTool getContentBeansTool() {
        checkStateValidity();
        return contentBeansTool;
    }

    public void setContentBeansTool(final ContentBeansTool contentBeansTool) {
        checkStateValidity();
        this.contentBeansTool = contentBeansTool;
    }

    @Override
    public ObjectConverter getObjectConverter() {
        return getContentBeansTool().getObjectConverter();
    }

    @Override
    public void setCachingObjectConverter(final boolean enabled) {
        checkStateValidity();
        this.cachingObjectConverterEnabled = enabled;
    }

    @Override
    public void setHstSiteMenusManager(final HstSiteMenusManager siteMenusManager) {
        checkStateValidity();
        this.siteMenusManager = siteMenusManager;
    }

    @Override
    public String getSiteContentBasePath() {
        checkStateValidity();
        return siteContentBasePath;
    }

    public void setSiteContentBasePath(final String siteContentBasePath) {
        checkStateValidity();
        this.siteContentBasePath = siteContentBasePath;
    }


    public void setDefaultObjectBeanManager(final ObjectBeanManager defaultObjectBeanManager) {
        checkStateValidity();
        this.defaultObjectBeanManager = defaultObjectBeanManager;
    }

    public void setNonDefaultObjectBeanManagers(final Map<Session, ObjectBeanManager> nonDefaultObjectBeanManagers) {
        checkStateValidity();
        this.nonDefaultObjectBeanManagers = nonDefaultObjectBeanManagers;
    }

    public void setDefaultHstQueryManager(final HstQueryManager defaultHstQueryManager) {
        checkStateValidity();
        this.defaultHstQueryManager = defaultHstQueryManager;
    }

    public void setNonDefaultHstQueryManagers(final Map<Session, HstQueryManager> nonDefaultHstQueryManagers) {
        checkStateValidity();
        this.nonDefaultHstQueryManagers = nonDefaultHstQueryManagers;
    }

    @Override
    public ObjectBeanManager getObjectBeanManager() throws IllegalStateException {
        checkStateValidity();
        return defaultObjectBeanManager;
    }

    @Override
    public ObjectBeanManager getObjectBeanManager(final Session session) throws IllegalStateException {
        checkStateValidity();
        return nonDefaultObjectBeanManagers.get(session);
    }

    @Override
    public HstQueryManager getQueryManager() throws IllegalStateException {
        checkStateValidity();
        return defaultHstQueryManager;
    }

    @Override
    public HstQueryManager getQueryManager(final Session session) throws IllegalStateException {
        checkStateValidity();
        return nonDefaultHstQueryManagers.get(session);
    }

    @Override
    public void clearObjectAndQueryManagers() {
        checkStateValidity();
    }

    @Override
    public void dispose() {
        attributes = null;
        session = null;
        baseURL = null;
        urlFactory = null;
        resolvedMount = null;
        resolvedSiteMapItem = null;
        linkCreator = null;
        parameterInfoProxyFactory = null;
        siteMapMatcher = null;
        siteMenus = null;
        hstQueryManagerFactory = null;
        defaultCredentials = null;
        containerConfiguration = null;
        contextCredentialsProvider = null;
        subject = null;
        preferredLocale = null;
        locales = null;
        pathSuffix = null;
        virtualHost = null;
        aliasMountMap = null;
        typeAndAliasMountMap = null;
        componentFilterTags = null;
        filters = null;
        renderHost = null;
        contentBeansTool = null;
        contentBean = null;
        siteContentBean = null;
        siteContentBasePath = null;
        defaultObjectBeanManager = null;
        nonDefaultObjectBeanManagers = null;
        defaultHstQueryManager = null;
        nonDefaultHstQueryManagers = null;

        disposed = true;
    }

    @Override
    public void matchingFinished() {
    }

    @Override
    public Map<String, HeadContributable> getHeadContributableMap() {
        if (headContributablesMap != null) {
            return Collections.unmodifiableMap(headContributablesMap);
        }

        return Collections.emptyMap();
    }

    @Override
    public HeadContributable getHeadContributable(String name) {
        if (headContributablesMap != null) {
            return headContributablesMap.get(name);
        }

        return null;
    }

    @Override
    public void setHeadContributable(String name, HeadContributable headContributable) {
        if (headContributablesMap == null) {
            headContributablesMap = new LinkedHashMap<>();
        }

        headContributablesMap.put(name, headContributable);
    }

    private void checkStateValidity() {
        if (disposed) {
            throw new IllegalStateException("Invocation on an invalid HstRequestContext instance. \n" +
                    "An HstRequestContext instance MUST not be used after a request processing cycle.\n" +
                    "Check if your component implementation is thread-safe!!!");
        }
    }

}
