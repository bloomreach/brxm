/**
 * Copyright 2015-2016 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.container;

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

public class GenericRequestContextWrapper implements  HstMutableRequestContext {

    private final HstMutableRequestContext context;

    public GenericRequestContextWrapper(final HstMutableRequestContext context) {
        this.context = context;
    }

    @Override
    public void setServletContext(final ServletContext servletContext) {
        context.setServletContext(servletContext);
    }

    @Override
    public void setServletRequest(final HttpServletRequest servletRequest) {
        context.setServletRequest(servletRequest);
    }

    @Override
    public void setServletResponse(final HttpServletResponse servletResponse) {
        context.setServletResponse(servletResponse);
    }

    @Override
    public void setSession(final Session session) {
        context.setSession(session);
    }

    @Override
    public void setResolvedMount(final ResolvedMount resolvedMount) {
        context.setResolvedMount(resolvedMount);
    }

    @Override
    public void setResolvedSiteMapItem(final ResolvedSiteMapItem resolvedSiteMapItem) {
        context.setResolvedSiteMapItem(resolvedSiteMapItem);
    }

    @Override
    public void setBaseURL(final HstContainerURL baseURL) {
        context.setBaseURL(baseURL);
    }

    @Override
    public void setURLFactory(final HstURLFactory urlFactory) {
        context.setURLFactory(urlFactory);
    }

    @Override
    public void setSiteMapMatcher(final HstSiteMapMatcher siteMapMatcher) {
        context.setSiteMapMatcher(siteMapMatcher);
    }

    @Override
    public void setLinkCreator(final HstLinkCreator linkCreator) {
        context.setLinkCreator(linkCreator);
    }

    @Override
    public void setParameterInfoProxyFactory(final HstParameterInfoProxyFactory parameterInfoProxyFactory) {
        context.setParameterInfoProxyFactory(parameterInfoProxyFactory);
    }

    @Override
    public void setHstSiteMenus(final HstSiteMenus siteMenus) {
        context.setHstSiteMenus(siteMenus);
    }

    @Override
    public void setHstQueryManagerFactory(final HstQueryManagerFactory hstQueryManagerFactory) {
        context.setHstQueryManagerFactory(hstQueryManagerFactory);
    }

    @Override
    public void setContainerConfiguration(final ContainerConfiguration containerConfiguration) {
        context.setContainerConfiguration(containerConfiguration);
    }

    @Override
    public void setSubject(final Subject subject) {
        context.setSubject(subject);
    }

    @Override
    public void setPreferredLocale(final Locale locale) {
        context.setPreferredLocale(locale);
    }

    @Override
    public void setLocales(final List<Locale> locales) {
        context.setLocales(locales);
    }

    @Override
    public void setPathSuffix(final String pathSuffix) {
        context.setPathSuffix(pathSuffix);
    }

    @Override
    public void setComponentFilterTags(final Set<String> conditions) {
        context.setComponentFilterTags(conditions);
    }

    @Override
    public void addComponentWindowFilter(final HstComponentWindowFilter filter) {
        context.addComponentWindowFilter(filter);
    }

    @Override
    public void setComponentWindowFilters(final List<HstComponentWindowFilter> filters) {
        context.setComponentWindowFilters(filters);
    }

    @Override
    public void setFullyQualifiedURLs(final boolean fullyQualifiedURLs) {
        context.setFullyQualifiedURLs(fullyQualifiedURLs);
    }

    @Override
    public void setRenderHost(final String renderHost) {
        context.setRenderHost(renderHost);
    }

    @Override
    public void setCmsRequest(final boolean cmsRequest) {
        context.setCmsRequest(cmsRequest);
    }

    @Override
    public void setContentBeansTool(final ContentBeansTool contentBeansTool) {
        context.setContentBeansTool(contentBeansTool);
    }

    @Override
    public void setCachingObjectConverter(final boolean enabled) {
        context.setCachingObjectConverter(enabled);
    }

    @Override
    public void setHstSiteMenusManager(final HstSiteMenusManager siteMenusManager) {
        context.setHstSiteMenusManager(siteMenusManager);
    }

    @Override
    public void clearObjectAndQueryManagers() {
        context.clearObjectAndQueryManagers();
    }

    @Override
    public ServletContext getServletContext() {
        return context.getServletContext();
    }

    @Override
    public HttpServletRequest getServletRequest() {
        return context.getServletRequest();
    }

    @Override
    public HttpServletResponse getServletResponse() {
        return context.getServletResponse();
    }

    @Override
    public Session getSession() throws LoginException, RepositoryException {
        return context.getSession();
    }

    @Override
    public Session getSession(final boolean create) throws LoginException, RepositoryException {
        return context.getSession(create);
    }

    @Override
    public ResolvedMount getResolvedMount() {
        return context.getResolvedMount();
    }

    @Override
    public ResolvedSiteMapItem getResolvedSiteMapItem() {
        return context.getResolvedSiteMapItem();
    }

    @Override
    public boolean isPreview() {
        return context.isPreview();
    }

    @Override
    public HstContainerURL getBaseURL() {
        return context.getBaseURL();
    }

    @Override
    public HstURLFactory getURLFactory() {
        return context.getURLFactory();
    }

    @Override
    public HstContainerURLProvider getContainerURLProvider() {
        return context.getContainerURLProvider();
    }

    @Override
    public HstSiteMapMatcher getSiteMapMatcher() {
        return context.getSiteMapMatcher();
    }

    @Override
    public HstLinkCreator getHstLinkCreator() {
        return context.getHstLinkCreator();
    }

    @Override
    public HstSiteMenus getHstSiteMenus() {
        return context.getHstSiteMenus();
    }

    @Override
    public HstQueryManagerFactory getHstQueryManagerFactory() {
        return context.getHstQueryManagerFactory();
    }

    @Override
    public HstParameterInfoProxyFactory getParameterInfoProxyFactory() {
        return context.getParameterInfoProxyFactory();
    }

    @Override
    public <T> T getModel(String name) {
        return context.getModel(name);
    }

    @Override
    public Iterable<String> getModelNames() {
        return context.getModelNames();
    }

    @Override
    public Map<String, Object> getModelsMap() {
        return context.getModelsMap();
    }

    @Override
    public Object setModel(String name, Object model) {
        return context.setModel(name, model);
    }

    @Override
    public void removeModel(String name) {
        context.removeModel(name);
    }

    @Override
    public void setAttribute(final String name, final Object object) {
        context.setAttribute(name, object);
    }

    @Override
    public Object getAttribute(final String name) {
        return context.getAttribute(name);
    }

    @Override
    public void removeAttribute(final String name) {
        context.removeAttribute(name);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return context.getAttributeNames();
    }

    @Override
    public Map<String, Object> getAttributes() {
        return context.getAttributes();
    }

    @Override
    public VirtualHost getVirtualHost() {
        return context.getVirtualHost();
    }

    @Override
    public ContainerConfiguration getContainerConfiguration() {
        return context.getContainerConfiguration();
    }

    @Override
    public boolean isCmsRequest() {
        return context.isCmsRequest();
    }

    @Override
    public ContextCredentialsProvider getContextCredentialsProvider() {
        return context.getContextCredentialsProvider();
    }

    @Override
    public Subject getSubject() {
        return context.getSubject();
    }

    @Override
    public Locale getPreferredLocale() {
        return context.getPreferredLocale();
    }

    @Override
    public Enumeration<Locale> getLocales() {
        return context.getLocales();
    }

    @Override
    public String getPathSuffix() {
        return context.getPathSuffix();
    }

    @Override
    public Mount getMount(final String alias) {
        return context.getMount(alias);
    }

    @Override
    public Mount getMount(final String alias, final String type) {
        return context.getMount(alias, type);
    }

    @Override
    public Set<String> getComponentFilterTags() {
        return context.getComponentFilterTags();
    }

    @Override
    public List<HstComponentWindowFilter> getComponentWindowFilters() {
        return context.getComponentWindowFilters();
    }

    @Override
    public boolean isFullyQualifiedURLs() {
        return context.isFullyQualifiedURLs();
    }

    @Override
    public String getRenderHost() {
        return context.getRenderHost();
    }

    @Override
    public ContentBeansTool getContentBeansTool() {
        return context.getContentBeansTool();
    }

    @Override
    public ObjectConverter getObjectConverter() {
        return context.getObjectConverter();
    }

    @Override
    public String getSiteContentBasePath() {
        return context.getSiteContentBasePath();
    }

    @Override
    public HippoBean getSiteContentBaseBean() {
        return context.getSiteContentBaseBean();
    }

    @Override
    public HippoBean getContentBean() {
        return context.getContentBean();
    }

    @Override
    public <T extends HippoBean> T getContentBean(final Class<T> beanMappingClass) {
        return context.getContentBean(beanMappingClass);
    }

    @Override
    public ObjectBeanManager getObjectBeanManager() throws IllegalStateException {
        return context.getObjectBeanManager();
    }

    @Override
    public ObjectBeanManager getObjectBeanManager(final Session session) throws IllegalStateException {
        return context.getObjectBeanManager(session);
    }

    @Override
    public HstQueryManager getQueryManager() throws IllegalStateException {
        return context.getQueryManager();
    }

    @Override
    public HstQueryManager getQueryManager(final Session session) throws IllegalStateException {
        return context.getQueryManager(session);
    }

    @Override
    public void dispose() {
        context.dispose();
    }

    @Override
    public void matchingFinished() {
        context.matchingFinished();
    }
}
