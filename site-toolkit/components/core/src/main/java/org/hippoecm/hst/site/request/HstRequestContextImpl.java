/*
 *  Copyright 2008-2015 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.site.request;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.jcr.LoginException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.security.auth.Subject;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.hosting.VirtualHost;
import org.hippoecm.hst.configuration.hosting.VirtualHosts;
import org.hippoecm.hst.content.beans.ObjectBeanManagerException;
import org.hippoecm.hst.content.beans.manager.ObjectBeanManager;
import org.hippoecm.hst.content.beans.manager.ObjectBeanManagerImpl;
import org.hippoecm.hst.content.beans.manager.ObjectConverter;
import org.hippoecm.hst.content.beans.query.HstQueryManager;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.tool.ContentBeansTool;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstParameterInfoProxyFactory;
import org.hippoecm.hst.core.component.HstParameterInfoProxyFactoryImpl;
import org.hippoecm.hst.core.component.HstURLFactory;
import org.hippoecm.hst.core.container.ContainerConfiguration;
import org.hippoecm.hst.core.container.HstComponentWindowFilter;
import org.hippoecm.hst.core.container.HstContainerURL;
import org.hippoecm.hst.core.container.HstContainerURLProvider;
import org.hippoecm.hst.core.internal.HstMutableRequestContext;
import org.hippoecm.hst.core.linking.HstLinkCreator;
import org.hippoecm.hst.core.request.ContextCredentialsProvider;
import org.hippoecm.hst.core.request.HstSiteMapMatcher;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.core.search.HstQueryManagerFactory;
import org.hippoecm.hst.core.sitemenu.HstSiteMenus;
import org.hippoecm.hst.core.sitemenu.HstSiteMenusManager;
import org.hippoecm.hst.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HstRequestContextImpl
 *
 */
public class HstRequestContextImpl implements HstMutableRequestContext {

    private final static Logger log = LoggerFactory.getLogger(HstRequestContextImpl.class);

    private final static HstParameterInfoProxyFactory HST_PARAMETER_INFO_PROXY_FACTORY = new HstParameterInfoProxyFactoryImpl();

    protected ServletContext servletContext;
    protected HttpServletRequest servletRequest;
    protected HttpServletResponse servletResponse;
    protected Repository repository;
    protected ContextCredentialsProvider contextCredentialsProvider;
    protected Session session;
    protected ResolvedMount resolvedMount;
    protected ResolvedSiteMapItem resolvedSiteMapItem;
    protected HstURLFactory urlFactory;
    protected HstContainerURL baseURL;
    protected HstLinkCreator linkCreator;
    protected HstParameterInfoProxyFactory parameterInfoProxyFactory;
    protected HstSiteMapMatcher siteMapMatcher;
    protected Optional<HstSiteMenus> siteMenus;
    protected HstQueryManagerFactory hstQueryManagerFactory;
    protected ContentBeansTool contentBeansTool;
    protected HstSiteMenusManager siteMenusManager;
    protected boolean cachingObjectConverterEnabled;
    protected Map<String, Object> attributes = new HashMap<>();
    protected ContainerConfiguration containerConfiguration;
    protected Subject subject;
    protected Locale preferredLocale;
    protected List<Locale> locales;
    protected String pathSuffix;
    protected Set<String> componentFilterTags;
    private List<HstComponentWindowFilter> filters;
    protected boolean fullyQualifiedURLs;
    protected String renderHost;
    // default a request is considered to be not from a cms. If cmsRequest is true, this means the
    // request is done from a cms context. This can influence for example how a link is created
    protected boolean cmsRequest;

    private Map<Session, ObjectBeanManager> objectBeanManagers;
    private Map<Session, HstQueryManager> hstQueryManagers;

    private Map<String, Object> unmodifiableAttributes;

    private boolean disposed;
    private boolean matchingFinished;

    private Map<String, Object> modelsMap = new HashMap<>();
    private Map<String, Object> unmodifiableModelsMap = Collections.unmodifiableMap(modelsMap);

    public HstRequestContextImpl(Repository repository) {
        this(repository, null);
    }

    public HstRequestContextImpl(Repository repository, ContextCredentialsProvider contextCredentialsProvider) {
        this.repository = repository;
        this.contextCredentialsProvider = contextCredentialsProvider;
    }

    @Override
    public boolean isPreview() {
        checkStateValidity();
        return this.resolvedMount.getMount().isPreview();
    }

    @Override
    public ServletContext getServletContext() {
        checkStateValidity();
        return servletContext;
    }

    @Override
    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    @Override
    public HttpServletRequest getServletRequest() {
        checkStateValidity();
        return servletRequest;
    }

    @Override
    public void setServletRequest(HttpServletRequest servletRequest) {
        this.servletRequest = servletRequest;
    }

    @Override
    public HttpServletResponse getServletResponse() {
        checkStateValidity();
        return servletResponse;
    }

    @Override
    public void setServletResponse(HttpServletResponse servletResponse) {
        this.servletResponse = servletResponse;
    }

    @Override
    public Session getSession() throws RepositoryException {
        return getSession(true);
    }

    @Override
    public Session getSession(boolean create) throws RepositoryException {
        checkStateValidity();

        if (this.session == null) {
            if (create) {
                final ContextCredentialsProvider credsProvider = getContextCredentialsProvider();
                if (credsProvider != null) {
                    final SimpleCredentials defaultCredentials = (SimpleCredentials) credsProvider.getDefaultCredentials(this);
                    try {
                        this.session = this.repository.login(defaultCredentials);
                    } catch (LoginException e) {
                        log.warn("Login Exception for session for userID {}. Cannot create session.", defaultCredentials.getUserID());
                        throw e;
                    }
                } else {
                    try {
                        this.session = this.repository.login();
                    } catch (LoginException e) {
                        log.warn("Login Exception for anonymous login.");
                        throw e;
                    }
                }
            }
        } else if (!this.session.isLive()) {
            throw new HstComponentException("Invalid session.");
        }
        return this.session;
    }

    @Override
    public void setSession(Session session) {
        checkStateValidity();
        this.session = session;
    }

    @Override
    public void setResolvedMount(ResolvedMount resolvedMount) {
        checkStateValidity();
        this.resolvedMount = resolvedMount;
    }

    @Override
    public ResolvedMount getResolvedMount() {
        checkStateValidity();
        return this.resolvedMount;
    }

    @Override
    public void setResolvedSiteMapItem(ResolvedSiteMapItem resolvedSiteMapItem) {
        checkStateValidity();
        this.resolvedSiteMapItem = resolvedSiteMapItem;
    }

    @Override
    public ResolvedSiteMapItem getResolvedSiteMapItem() {
        checkStateValidity();
        return this.resolvedSiteMapItem;
    }

    @Override
    public void setBaseURL(HstContainerURL baseURL) {
        checkStateValidity();
        this.baseURL = baseURL;
    }

    @Override
    public HstContainerURL getBaseURL() {
        checkStateValidity();
        return this.baseURL;
    }

    @Override
    public void setURLFactory(HstURLFactory urlFactory) {
        checkStateValidity();
        this.urlFactory = urlFactory;
    }

    @Override
    public HstURLFactory getURLFactory() {
        checkStateValidity();
        return this.urlFactory;
    }

    @Override
    public HstContainerURLProvider getContainerURLProvider() {
        checkStateValidity();
        return urlFactory != null ? urlFactory.getContainerURLProvider() : null;
    }

    @Override
    public void setSiteMapMatcher(HstSiteMapMatcher siteMapMatcher) {
        checkStateValidity();
        this.siteMapMatcher = siteMapMatcher;
    }

    @Override
    public HstSiteMapMatcher getSiteMapMatcher() {
        checkStateValidity();
        return this.siteMapMatcher;
    }

    @Override
    public void setLinkCreator(HstLinkCreator linkCreator) {
        checkStateValidity();
        this.linkCreator = linkCreator;
    }

    @Override
    public HstLinkCreator getHstLinkCreator() {
        checkStateValidity();
        return this.linkCreator;
    }

    @Override
    public void setParameterInfoProxyFactory(HstParameterInfoProxyFactory parameterInfoProxyFactory) {
        checkStateValidity();
        this.parameterInfoProxyFactory = parameterInfoProxyFactory;
    }

    @Override
    public HstParameterInfoProxyFactory getParameterInfoProxyFactory() {
        checkStateValidity();
        if (parameterInfoProxyFactory == null) {
            return HST_PARAMETER_INFO_PROXY_FACTORY;
        }
        return parameterInfoProxyFactory;
    }

    @Override
    public void setHstSiteMenusManager(final HstSiteMenusManager siteMenusManager) {
        checkStateValidity();
        this.siteMenusManager = siteMenusManager;
    }

    @Override

    public void setHstSiteMenus(HstSiteMenus siteMenus) {
        checkStateValidity();
        this.siteMenus = Optional.ofNullable(siteMenus);
    }

    @Override
    public HstSiteMenus getHstSiteMenus() {
        checkStateValidity();
        checkMatchingPhaseFinished("getHstSiteMenus");
        if (resolvedSiteMapItem == null) {
            throw new IllegalStateException("HstRequestContext#getHstSiteMenus() is not allowed to be invoked without " +
                    "there being a ResolvedSiteMapItem matched and set on this HstRequestContext.");
        }
        if (siteMenus == null) {
            siteMenus = Optional.ofNullable(siteMenusManager.getSiteMenus(this));
        }
        return siteMenus.orElse(null);
    }

    @Override
    public HstQueryManagerFactory getHstQueryManagerFactory() {
        checkStateValidity();
        return hstQueryManagerFactory;
    }

    @Override
    public void setHstQueryManagerFactory(HstQueryManagerFactory hstQueryManagerFactory) {
        checkStateValidity();
        this.hstQueryManagerFactory = hstQueryManagerFactory;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getModel(String name) {
        checkStateValidity();

        return (T) getModelsMap().get(name);
    }

    @Override
    public Iterable<String> getModelNames() {
        checkStateValidity();

        return Collections.unmodifiableSet(getModelsMap().keySet());
    }

    @Override
    public Map<String, Object> getModelsMap() {
        checkStateValidity();

        return unmodifiableModelsMap;
    }

    @Override
    public Object setModel(String name, Object model) {
        checkStateValidity();

        setAttribute(name, model);
        return modelsMap.put(name, model);
    }

    @Override
    public void removeModel(String name) {
        checkStateValidity();

        if (modelsMap.remove(name) != null) {
            removeAttribute(name);
        }
    }

    @Override
    public Object getAttribute(String name) {
        checkStateValidity();

        if (name == null) {
            throw new IllegalArgumentException("attribute name cannot be null.");
        }

        Object value = null;

        if (this.attributes != null) {
            value = this.attributes.get(name);
        }

        return value;
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        checkStateValidity();

        if (this.attributes != null) {
            return Collections.enumeration(attributes.keySet());
        } else {
            List<String> emptyAttrNames = Collections.emptyList();
            return Collections.enumeration(emptyAttrNames);
        }
    }

    @Override
    public void removeAttribute(String name) {
        checkStateValidity();

        if (name == null) {
            throw new IllegalArgumentException("attribute name cannot be null.");
        }

        this.attributes.remove(name);
    }

    @Override
    public void setAttribute(String name, Object object) {
        checkStateValidity();

        if (name == null) {
            throw new IllegalArgumentException("attribute name cannot be null.");
        }

        if (object == null) {
            removeAttribute(name);
        }

        this.attributes.put(name, object);
    }

    @Override
    public Map<String, Object> getAttributes() {
        checkStateValidity();

        if (unmodifiableAttributes == null && attributes != null) {
            unmodifiableAttributes = Collections.unmodifiableMap(attributes);
        }

        if (unmodifiableAttributes == null) {
            return Collections.emptyMap();
        }

        return unmodifiableAttributes;
    }

    @Override
    public ContainerConfiguration getContainerConfiguration() {
        checkStateValidity();
        return this.containerConfiguration;
    }

    @Override
    public void setContainerConfiguration(ContainerConfiguration containerConfiguration) {
        checkStateValidity();
        this.containerConfiguration = containerConfiguration;
    }

    @Override
    public VirtualHost getVirtualHost() {
        checkStateValidity();
        return resolvedMount.getMount().getVirtualHost();
    }

    @Override
    public ContextCredentialsProvider getContextCredentialsProvider() {
        checkStateValidity();
        return contextCredentialsProvider;
    }

    @Override
    public void setSubject(Subject subject) {
        checkStateValidity();
        this.subject = subject;
    }

    @Override
    public Subject getSubject() {
        checkStateValidity();
        return subject;
    }

    @Override
    public void setPreferredLocale(Locale preferredLocale) {
        checkStateValidity();
        this.preferredLocale = preferredLocale;
    }

    @Override
    public Locale getPreferredLocale() {
        checkStateValidity();
        return preferredLocale;
    }

    @Override
    public void setLocales(List<Locale> locales) {
        checkStateValidity();
        this.locales = locales;
    }

    @Override
    public Enumeration<Locale> getLocales() {
        checkStateValidity();

        if (locales != null) {
            return Collections.enumeration(locales);
        }

        return null;
    }

    @Override
    public void setPathSuffix(String pathSuffix) {
        checkStateValidity();
        this.pathSuffix = pathSuffix;
    }

    @Override
    public String getPathSuffix() {
        checkStateValidity();
        checkMatchingPhaseFinished("getPathSuffix");
        return pathSuffix;
    }

    @Override
    public void setComponentFilterTags(final Set<String> componentFilterTags) {
        checkStateValidity();
        this.componentFilterTags = componentFilterTags;
    }

    @Override
    public Set<String> getComponentFilterTags() {
        checkStateValidity();
        if (componentFilterTags == null) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(componentFilterTags);
    }

    @Override
    public Mount getMount(String alias) {
        checkStateValidity();
        checkMatchingPhaseFinished("getMount");
        if (alias == null) {
            throw new IllegalArgumentException("Alias is not allowed to be null");
        }
        // we first check whether there is a mapped alias: Mapped aliases have precedence
        Mount currentMount = getResolvedMount().getMount();
        String mappedAlias = currentMount.getMountProperties().get(alias.toLowerCase());
        if (mappedAlias != null) {
            log.debug("Did find mappedAlias '{}' for alias '{}'. Try to find a mount for this mappedAlias.", mappedAlias, alias);
            Mount mount = lookupMount(mappedAlias.toLowerCase());
            if (mount != null) {
                return mount;
            }
            log.debug("did not find a Mount for mappedAlias '{}'. Try fallback to find a Mount having alias '{}'", mappedAlias, alias);
        }
        return lookupMount(alias.toLowerCase());
    }

    @Override
    public Mount getMount(String alias, String type) {
        checkStateValidity();
        checkMatchingPhaseFinished("getMount");
        if (alias == null || type == null) {
            throw new IllegalArgumentException("Alias and type are not allowed to be null");
        }
        String mappedAlias = getResolvedMount().getMount().getMountProperties().get(alias.toLowerCase());
        if (mappedAlias != null) {
            Mount mount = getVirtualHost().getVirtualHosts().getMountByGroupAliasAndType(getVirtualHost().getHostGroupName(), mappedAlias, type);
            if (mount != null) {
                return mount;
            } else {
                log.debug("We did not find a mapped mount for mappedAlias '{}'. Try to find a mount for alias '{}' directly", mappedAlias, alias);
            }
        } else {
            log.debug("Did not find a mappedAlias for alias '{}'. Try alias directly", alias);
        }

        Mount mount = getVirtualHost().getVirtualHosts().getMountByGroupAliasAndType(getVirtualHost().getHostGroupName(), alias.toLowerCase(), type);
        if (mount == null) {
            log.debug("We did not find a direct mount for alias '{}'. Return null.", alias);
        }
        return mount;
    }

    private Mount lookupMount(String alias) {
        Mount currentMount = getResolvedMount().getMount();
        String hostGroupName = currentMount.getVirtualHost().getHostGroupName();
        VirtualHosts hosts = currentMount.getVirtualHost().getVirtualHosts();
        List<Mount> possibleMounts = new ArrayList<Mount>();

        for (String type : currentMount.getTypes()) {
            Mount possibleMount = hosts.getMountByGroupAliasAndType(hostGroupName, alias, type);
            if (possibleMount != null) {
                possibleMounts.add(possibleMount);
            }
        }

        if (possibleMounts.size() == 0) {
            log.debug("Did not find a mount for alias '{}'. Return null", alias);
            return null;
        }

        if (possibleMounts.size() == 1) {
            return possibleMounts.get(0);
        }

        // there are multiple possible. Let's return the best.
        for (Mount possibleMount : possibleMounts) {
            if (possibleMount.getType().equals(currentMount.getType())) {
                // found a primary match
                return possibleMount;
            }
        }

        // we did not find a primary match for best match. We return the mount with the most types in common.

        List<Mount> narrowedPossibleMounts = new ArrayList<Mount>();
        if (possibleMounts.size() > 1) {
            // find the Mount's with the most types in common
            int mostCommon = 0;
            for (Mount s : possibleMounts) {
                int inCommon = countCommon(s.getTypes(), currentMount.getTypes());
                if (inCommon > mostCommon) {
                    mostCommon = inCommon;
                    narrowedPossibleMounts.clear();
                    narrowedPossibleMounts.add(s);
                } else if (inCommon == mostCommon) {
                    narrowedPossibleMounts.add(s);
                } else {
                    // do nothing, there where less types in common
                }
            }
        }

        // we won't continue searching for a better possible match as this most likely is never need:
        if (narrowedPossibleMounts.size() > 0) {
            return narrowedPossibleMounts.get(0);
        }

        // should not get here
        return possibleMounts.get(0);
    }


    private int countCommon(List<String> types, List<String> types2) {
        int counter = 0;
        for (String type : types) {
            if (types2.contains(type)) {
                counter++;
            }
        }
        return counter;
    }

    @Override
    public void setFullyQualifiedURLs(boolean fullyQualifiedURLs) {
        checkStateValidity();
        this.fullyQualifiedURLs = fullyQualifiedURLs;
    }

    @Override
    public boolean isFullyQualifiedURLs() {
        checkStateValidity();
        checkMatchingPhaseFinished("isFullyQualifiedURLs");
        return fullyQualifiedURLs;
    }

    @Override
    public void setRenderHost(String renderHost) {
        checkStateValidity();
        this.renderHost = renderHost;
    }

    @Override
    public String getRenderHost() {
        checkStateValidity();
        checkMatchingPhaseFinished("getRenderHost");
        return renderHost;
    }

    @Override
    public List<HstComponentWindowFilter> getComponentWindowFilters() {
        checkStateValidity();
        if (filters == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(filters);
    }

    @Override
    public void addComponentWindowFilter(HstComponentWindowFilter filter) {
        log.warn("HstRequestContextImpl.addComponentWindowFilter is deprecated. Use spring bean " +
                "'org.hippoecm.hst.core.container.HstComponentWindowFilter.list' to add HstComponentWindowFilter instances");
        checkStateValidity();
        if (filters == null) {
            filters = new ArrayList<>();
        }
        filters.add(filter);
    }

    @Override
    public void setComponentWindowFilters(final List<HstComponentWindowFilter> filters) {
        this. filters = filters;
    }

    @Override
    public boolean isCmsRequest() {
        checkStateValidity();
        return cmsRequest;
    }

    @Override
    public void setCmsRequest(final boolean cmsRequest) {
        checkStateValidity();
        this.cmsRequest = cmsRequest;
    }

    @Override
    public ContentBeansTool getContentBeansTool() {
        checkStateValidity();
        return contentBeansTool;
    }

    @Override
    public void setContentBeansTool(ContentBeansTool contentBeansTool) {
        checkStateValidity();
        this.contentBeansTool = contentBeansTool;
    }

    @Override
    public void setCachingObjectConverter(final boolean enabled) {
        checkStateValidity();
        this.cachingObjectConverterEnabled = enabled;
    }

    @Override
    public HippoBean getContentBean() {
        checkStateValidity();
        checkMatchingPhaseFinished("getContentBean");
        if (getResolvedSiteMapItem() != null) {
            return getBeanForResolvedSiteMapItem(getResolvedSiteMapItem());
        }

        // this might be a request for a mount that is not mapped (does not have a sitemap item)
        if (!getResolvedMount().getMount().isMapped()) {
            final String contentPathInfo = PathUtils.normalizePath(getBaseURL().getPathInfo());
            return getHippoBean(getSiteContentBasePath(), contentPathInfo);
        }

        log.info("Did not find a content bean for '{}'", getServletRequest());
        return null;
    }

    @Override
    public <T extends HippoBean> T getContentBean(final Class<T> beanMappingClass) {
        checkStateValidity();
        checkMatchingPhaseFinished("getContentBean");
        HippoBean bean = getContentBean();
        if (bean == null) {
            return null;
        }
        if (!beanMappingClass.isAssignableFrom(bean.getClass())) {
            log.debug("Required bean of type '{}' but found of type '{}'. Return null.", beanMappingClass.getName(), bean.getClass().getName());
            return null;
        }
        return (T) bean;
    }

    @Override
    public String getSiteContentBasePath() {
        checkStateValidity();
        checkMatchingPhaseFinished("getSiteContentBasePath");
        return PathUtils.normalizePath(getResolvedMount().getMount().getContentPath());
    }

    @Override
    public HippoBean getSiteContentBaseBean() {
        checkStateValidity();
        checkMatchingPhaseFinished("getSiteContentBaseBean");
        String base = getSiteContentBasePath();
        try {
            return (HippoBean) getObjectBeanManager().getObject("/" + base);
        } catch (ObjectBeanManagerException e) {
            log.error("ObjectBeanManagerException. Return null : {}", e);
            return null;
        }
    }


    private HippoBean getBeanForResolvedSiteMapItem(ResolvedSiteMapItem resolvedSiteMapItem) {
        String base = getSiteContentBasePath();
        String relPath = PathUtils.normalizePath(resolvedSiteMapItem.getRelativeContentPath());

        if (relPath == null) {
            log.debug("Cannot return a content bean for relative path null for resolvedSitemapItem belonging to '{}'. Return null", resolvedSiteMapItem.getHstSiteMapItem().getId());
            return null;
        }

        return getHippoBean(base, relPath);

    }

    private HippoBean getHippoBean(final String base, final String relPath) {
        try {
            if (StringUtils.isEmpty(relPath)) {
                return (HippoBean) getObjectBeanManager().getObject("/" + base);
            } else {
                return (HippoBean) getObjectBeanManager().getObject("/" + base + "/" + relPath);
            }
        } catch (ObjectBeanManagerException e) {
            log.error("ObjectBeanManagerException. Return null : {}", e);
            return null;
        }
    }

    @Override
    public ObjectBeanManager getObjectBeanManager() {
        checkStateValidity();
        try {
            return getObjectBeanManager(getSession());
        } catch (RepositoryException e) {
            throw new IllegalStateException("Cannot get ObjectBeanManager. Cause : '"+e.toString()+"'", e);
        }
    }

    @Override
    public ObjectBeanManager getObjectBeanManager(final Session session) {
        checkStateValidity();
        if (objectBeanManagers == null) {
            objectBeanManagers = new IdentityHashMap<>();
        }
        ObjectBeanManager objectBeanManager = objectBeanManagers.get(session);
        if (objectBeanManager == null) {
            objectBeanManager = createObjectBeanManager(session);
            objectBeanManagers.put(session, objectBeanManager);
        }
        return objectBeanManager;
    }

    @Override
    public HstQueryManager getQueryManager() throws IllegalStateException {
        checkStateValidity();
        try {
            return getQueryManager(getSession());
        } catch (RepositoryException e) {
            throw new IllegalStateException("Cannot get HstQueryManager. Cause : '"+e.toString()+"'", e);
        }
    }

    @Override
    public HstQueryManager getQueryManager(final Session session) throws IllegalStateException {
        checkStateValidity();
        if (hstQueryManagers == null) {
            hstQueryManagers = new IdentityHashMap<>();
        }
        HstQueryManager hstQueryManager = hstQueryManagers.get(session);
        if (hstQueryManager == null) {
            hstQueryManager = createQueryManager(session);
            hstQueryManagers.put(session, hstQueryManager);
        }
        return hstQueryManager;
    }

    @Override
    public void clearObjectAndQueryManagers() {
        checkStateValidity();
        if (objectBeanManagers != null) {
            objectBeanManagers.clear();
        }
        if (hstQueryManagers != null) {
            hstQueryManagers.clear();
        }
    }

    @Override
    public void dispose() {

        servletContext = null;
        servletRequest = null;
        servletResponse = null;
        repository = null;
        contextCredentialsProvider = null;
        session = null;
        resolvedMount = null;
        resolvedSiteMapItem = null;
        urlFactory = null;
        baseURL = null;
        linkCreator = null;
        parameterInfoProxyFactory = null;
        siteMapMatcher = null;
        siteMenus = null;
        hstQueryManagerFactory = null;
        contentBeansTool = null;
        attributes.clear();
        containerConfiguration = null;
        subject = null;
        preferredLocale = null;
        locales = null;
        pathSuffix = null;
        componentFilterTags = null;
        filters = null;
        renderHost = null;
        objectBeanManagers = null;
        hstQueryManagers = null;
        unmodifiableAttributes = null;

        disposed = true;
    }

    @Override
    public void matchingFinished() {
        matchingFinished = true;
    }

    private ObjectBeanManager createObjectBeanManager(Session session) {
        return new ObjectBeanManagerImpl(session, getObjectConverter());
    }

    private HstQueryManager createQueryManager(Session session) throws IllegalStateException {
        return hstQueryManagerFactory.createQueryManager(session, getObjectConverter());
    }

    public ObjectConverter getObjectConverter() {
        final ObjectConverter converter = getContentBeansTool().getObjectConverter();
        if (cachingObjectConverterEnabled) {
            return new CachingObjectConverter(converter);
        }
        return converter;
    }

    private void checkStateValidity() {
        if (disposed) {
            throw new IllegalStateException("Invocation on an invalid HstRequestContext instance. \n" +
                    "An HstRequestContext instance MUST not be used after a request processing cycle.\n" +
                    "Check if your component implementation is thread-safe!!!");
        }
    }

    private void checkMatchingPhaseFinished(final String methodName) {
        if (!matchingFinished) {
            throw new IllegalStateException(String.format("Invocation of method '%s' is only allowed after " +
                    "all matching to host, mount and optionally sitemap has finished, but matching " +
                    "is not yes finished. Problematic request is '%s'", methodName, getServletRequest()));
        }
    }
}
