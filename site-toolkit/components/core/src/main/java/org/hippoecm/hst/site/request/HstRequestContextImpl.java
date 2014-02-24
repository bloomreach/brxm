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
package org.hippoecm.hst.site.request;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.jcr.LoginException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.security.auth.Subject;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.hosting.VirtualHost;
import org.hippoecm.hst.configuration.hosting.VirtualHosts;
import org.hippoecm.hst.container.RequestContextProvider;
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
import org.hippoecm.hst.core.container.CmsSecurityValve;
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
import org.hippoecm.hst.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HstRequestContextImpl
 *
 * @version $Id$
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
    protected String contextNamespace = "";
    protected HstLinkCreator linkCreator;
    protected HstParameterInfoProxyFactory parameterInfoProxyFactory;
    protected HstSiteMapMatcher siteMapMatcher;
    protected HstSiteMenus siteMenus;
    protected HstQueryManagerFactory hstQueryManagerFactory;
    protected ContentBeansTool contentBeansTool;
    protected boolean cachingObjectConverterEnabled;
    protected Map<String, Object> attributes;
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

    private ObjectBeanManager defaultObjectBeanManager;
    private Map<Session, ObjectBeanManager> nonDefaultObjectBeanManagers;
    private HstQueryManager defaultHstQueryManager;
    private Map<Session, HstQueryManager> nonDefaultHstQueryManagers;

    private Map<String, Object> unmodifiableAttributes;

    public HstRequestContextImpl(Repository repository) {
        this(repository, null);
    }

    public HstRequestContextImpl(Repository repository, ContextCredentialsProvider contextCredentialsProvider) {
        this.repository = repository;
        this.contextCredentialsProvider = contextCredentialsProvider;
    }

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

    public void setContextNamespace(String contextNamespace) {
        this.contextNamespace = contextNamespace;
    }

    public String getContextNamespace() {
        return this.contextNamespace;
    }

    public Session getSession() throws LoginException, RepositoryException {
        return getSession(true);
    }

    public Session getSession(boolean create) throws LoginException, RepositoryException {
        if (this.session == null) {
            if (create) {
                if (contextCredentialsProvider != null) {
                    this.session = this.repository.login(contextCredentialsProvider.getDefaultCredentials(this));
                } else {
                    this.session = this.repository.login();
                }
            }
        } else if (!this.session.isLive()) {
            throw new HstComponentException("Invalid session.");
        }
        return this.session;
    }

    public void setSession(Session session) {
        this.session = session;
    }

    public void setResolvedMount(ResolvedMount resolvedMount) {
        this.resolvedMount = resolvedMount;
    }

    public ResolvedMount getResolvedMount() {
        return this.resolvedMount;
    }

    public void setResolvedSiteMapItem(ResolvedSiteMapItem resolvedSiteMapItem) {
        this.resolvedSiteMapItem = resolvedSiteMapItem;
    }

    public ResolvedSiteMapItem getResolvedSiteMapItem() {
        return this.resolvedSiteMapItem;
    }

    public void setBaseURL(HstContainerURL baseURL) {
        this.baseURL = baseURL;
    }

    public HstContainerURL getBaseURL() {
        return this.baseURL;
    }

    public void setURLFactory(HstURLFactory urlFactory) {
        this.urlFactory = urlFactory;
    }

    public HstURLFactory getURLFactory() {
        return this.urlFactory;
    }

    public HstContainerURLProvider getContainerURLProvider() {
        return urlFactory != null ? urlFactory.getContainerURLProvider() : null;
    }

    public void setSiteMapMatcher(HstSiteMapMatcher siteMapMatcher) {
        this.siteMapMatcher = siteMapMatcher;
    }

    public HstSiteMapMatcher getSiteMapMatcher() {
        return this.siteMapMatcher;
    }

    public void setLinkCreator(HstLinkCreator linkCreator) {
        this.linkCreator = linkCreator;
    }

    public HstLinkCreator getHstLinkCreator() {
        return this.linkCreator;
    }

    @Override
    public void setParameterInfoProxyFactory(HstParameterInfoProxyFactory parameterInfoProxyFactory) {
        this.parameterInfoProxyFactory = parameterInfoProxyFactory;
    }

    @Override
    public HstParameterInfoProxyFactory getParameterInfoProxyFactory() {
        if (parameterInfoProxyFactory == null) {
            return HST_PARAMETER_INFO_PROXY_FACTORY;
        }
        return parameterInfoProxyFactory;
    }

    public void setHstSiteMenus(HstSiteMenus siteMenus) {
        this.siteMenus = siteMenus;
    }

    public HstSiteMenus getHstSiteMenus() {
        return this.siteMenus;
    }

    public HstQueryManagerFactory getHstQueryManagerFactory() {
        return hstQueryManagerFactory;
    }

    public void setHstQueryManagerFactory(HstQueryManagerFactory hstQueryManagerFactory) {
        this.hstQueryManagerFactory = hstQueryManagerFactory;
    }

    public Object getAttribute(String name) {
        if (name == null) {
            throw new IllegalArgumentException("attribute name cannot be null.");
        }

        Object value = null;

        if (this.attributes != null) {
            value = this.attributes.get(name);
        }

        return value;
    }

    public Enumeration<String> getAttributeNames() {

        if (this.attributes != null) {
            return Collections.enumeration(attributes.keySet());
        } else {
            List<String> emptyAttrNames = Collections.emptyList();
            return Collections.enumeration(emptyAttrNames);
        }
    }

    public void removeAttribute(String name) {
        if (name == null) {
            throw new IllegalArgumentException("attribute name cannot be null.");
        }

        if (this.attributes != null) {
            this.attributes.remove(name);
        }
    }

    public void setAttribute(String name, Object object) {
        if (name == null) {
            throw new IllegalArgumentException("attribute name cannot be null.");
        }

        if (object == null) {
            removeAttribute(name);
        }

        if (this.attributes == null) {
            synchronized (this) {
                if (this.attributes == null) {
                    this.attributes = new HashMap<String, Object>();
                }
            }
        }

        this.attributes.put(name, object);
    }

    public Map<String, Object> getAttributes() {
        if (unmodifiableAttributes == null && attributes != null) {
            unmodifiableAttributes = Collections.unmodifiableMap(attributes);
        }

        if (unmodifiableAttributes == null) {
            return Collections.emptyMap();
        }

        return unmodifiableAttributes;
    }

    public ContainerConfiguration getContainerConfiguration() {
        return this.containerConfiguration;
    }

    public void setContainerConfiguration(ContainerConfiguration containerConfiguration) {
        this.containerConfiguration = containerConfiguration;
    }

    public VirtualHost getVirtualHost() {
        return resolvedMount.getMount().getVirtualHost();
    }

    public ContextCredentialsProvider getContextCredentialsProvider() {
        return contextCredentialsProvider;
    }

    public void setSubject(Subject subject) {
        this.subject = subject;
    }

    public Subject getSubject() {
        return subject;
    }

    public void setPreferredLocale(Locale preferredLocale) {
        this.preferredLocale = preferredLocale;
    }

    public Locale getPreferredLocale() {
        return preferredLocale;
    }

    public void setLocales(List<Locale> locales) {
        this.locales = locales;
    }

    public Enumeration<Locale> getLocales() {
        if (locales != null) {
            return Collections.enumeration(locales);
        }

        return null;
    }

    public void setPathSuffix(String pathSuffix) {
        this.pathSuffix = pathSuffix;
    }

    public String getPathSuffix() {
        return pathSuffix;
    }

    @Override
    public void setComponentFilterTags(final Set<String> componentFilterTags) {
        this.componentFilterTags = componentFilterTags;
    }

    @Override
    public Set<String> getComponentFilterTags() {
        if (componentFilterTags == null) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(componentFilterTags);
    }

    public Mount getMount(String alias) {
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

    public Mount getMount(String alias, String type) {
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
        this.fullyQualifiedURLs = fullyQualifiedURLs;
    }

    @Override
    public boolean isFullyQualifiedURLs() {
        return fullyQualifiedURLs;
    }

    @Override
    public void setRenderHost(String renderHost) {
        this.renderHost = renderHost;
    }

    @Override
    public String getRenderHost() {
        return renderHost;
    }

    @Override
    public List<HstComponentWindowFilter> getComponentWindowFilters() {
        if (filters == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(filters);
    }

    @Override
    public void addComponentWindowFilter(HstComponentWindowFilter filter) {
        if (filters == null) {
            filters = new ArrayList<HstComponentWindowFilter>();
        }
        filters.add(filter);
    }

    @Override
    public boolean isCmsRequest() {
        return cmsRequest;
    }

    @Override
    public void setCmsRequest(final boolean cmsRequest) {
        this.cmsRequest = cmsRequest;
    }

    @Override
    public ContentBeansTool getContentBeansTool() {
        return contentBeansTool;
    }

    @Override
    public void setContentBeansTool(ContentBeansTool contentBeansTool) {
        this.contentBeansTool = contentBeansTool;
    }

    @Override
    public void setCachingObjectConverter(final boolean enabled) {
        this.cachingObjectConverterEnabled = enabled;
    }

    @Override
    public HippoBean getContentBean() {
        HstRequestContext requestContext = RequestContextProvider.get();
        if (requestContext == null || requestContext.getResolvedSiteMapItem() == null) {
            return null;
        }
        return getBeanForResolvedSiteMapItem(requestContext.getResolvedSiteMapItem());
    }

    @Override
    public String getSiteContentBasePath() {
        HstRequestContext requestContext = RequestContextProvider.get();

        if (requestContext == null) {
            throw new IllegalStateException("HstRequestContext is not set in handler.");
        }

        return PathUtils.normalizePath(requestContext.getResolvedMount().getMount().getContentPath());
    }

    @Override
    public HippoBean getSiteContentBaseBean() {
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

        try {
            if ("".equals(relPath)) {
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
        if (defaultObjectBeanManager != null) {
            return defaultObjectBeanManager;
        }
        try {
            defaultObjectBeanManager = createObjectBeanManager(getSession());
        } catch (RepositoryException e) {
            throw new IllegalStateException("Cannot get ObjectBeanManager", e);
        }
        return defaultObjectBeanManager;
    }

    @Override
    public ObjectBeanManager getObjectBeanManager(final Session session) {
        if (nonDefaultObjectBeanManagers == null) {
            nonDefaultObjectBeanManagers = new IdentityHashMap<Session, ObjectBeanManager>();
        }
        ObjectBeanManager nonDefaultObjectBeanManager = nonDefaultObjectBeanManagers.get(session);
        if (nonDefaultObjectBeanManager == null) {
            nonDefaultObjectBeanManager = createObjectBeanManager(session);
            nonDefaultObjectBeanManagers.put(session, nonDefaultObjectBeanManager);
        }
        return nonDefaultObjectBeanManager;
    }

    @Override
    public HstQueryManager getQueryManager() throws IllegalStateException {
        if (defaultHstQueryManager != null) {
            return defaultHstQueryManager;
        }
        try {
            defaultHstQueryManager = createQueryManager(getSession());
        } catch (RepositoryException e) {
            throw new IllegalStateException("Cannot get HstQueryManager", e);
        }
        return defaultHstQueryManager;
    }

    @Override
    public HstQueryManager getQueryManager(final Session session) throws IllegalStateException {
        if (nonDefaultHstQueryManagers == null) {
            nonDefaultHstQueryManagers = new IdentityHashMap<Session, HstQueryManager>();
        }
        HstQueryManager nonDefaultHstQueryManager = nonDefaultHstQueryManagers.get(session);
        if (nonDefaultHstQueryManager == null) {
            nonDefaultHstQueryManager = createQueryManager(session);
            nonDefaultHstQueryManagers.put(session, nonDefaultHstQueryManager);
        }
        return nonDefaultHstQueryManager;
    }

    @Override
    public void clearObjectAndQueryManagers() {
        defaultHstQueryManager = null;
        defaultObjectBeanManager = null;
        if (nonDefaultObjectBeanManagers != null) {
            nonDefaultObjectBeanManagers.clear();
        }
        if (nonDefaultHstQueryManagers != null) {
            nonDefaultHstQueryManagers.clear();
        }
    }

    private ObjectBeanManager createObjectBeanManager(Session session) {
        return new ObjectBeanManagerImpl(session, getObjectConverter());
    }

    private HstQueryManager createQueryManager(Session session) throws IllegalStateException {
        return hstQueryManagerFactory.createQueryManager(session, getObjectConverter());
    }

    private ObjectConverter getObjectConverter() {
        final ObjectConverter converter = getContentBeansTool().getObjectConverter();
        if (cachingObjectConverterEnabled) {
            return new CachingObjectConverter(converter);
        }
        return converter;
    }


}
