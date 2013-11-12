/*
 * Copyright 2009-2013 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.core.container;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.cache.ForwardPlaceHolderHstPageInfo;
import org.hippoecm.hst.cache.HstPageInfo;
import org.hippoecm.hst.cache.UncacheableHstPageInfo;
import org.hippoecm.hst.configuration.components.DelegatingHstComponentInfo;
import org.hippoecm.hst.configuration.components.HstComponentInfo;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.model.HstManager;
import org.hippoecm.hst.container.valves.AbstractOrderableValve;
import org.hippoecm.hst.content.tool.ContentBeansTool;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.component.HstURLFactory;
import org.hippoecm.hst.core.internal.HstMutableRequestContext;
import org.hippoecm.hst.core.internal.HstRequestContextComponent;
import org.hippoecm.hst.core.linking.HstLinkCreator;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.HstSiteMapMatcher;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.core.search.HstQueryManagerFactory;
import org.hippoecm.hst.core.sitemenu.HstSiteMenusManager;
import org.hippoecm.hst.resourcebundle.ResourceBundleRegistry;
import org.hippoecm.hst.util.DefaultKeyValue;
import org.hippoecm.hst.util.KeyValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.ehcache.constructs.web.GenericResponseWrapper;

/**
 * AbstractBaseOrderableValve
 */
public abstract class AbstractBaseOrderableValve extends AbstractOrderableValve implements Valve {

    protected final static Logger log = LoggerFactory.getLogger(AbstractBaseOrderableValve.class);

    private final static String PAGE_INFO_CACHEABLE = PageInfoRenderingValve.class.getName() + ".pageInfoCacheable";

    protected ContainerConfiguration containerConfiguration;
    protected HstManager hstManager;
    protected HstSiteMapMatcher siteMapMatcher;
    protected HstRequestContextComponent requestContextComponent;
    protected HstComponentFactory componentFactory;
    protected HstComponentWindowFactory componentWindowFactory;
    protected HstComponentInvoker componentInvoker;
    protected HstURLFactory urlFactory;
    protected HstLinkCreator linkCreator;
    protected HstSiteMenusManager siteMenusManager;
    protected HstQueryManagerFactory hstQueryManagerFactory;
    protected PageErrorHandler defaultPageErrorHandler;
    protected ResourceBundleRegistry resourceBundleRegistry;
    protected ContentBeansTool contentBeansTool;
    protected boolean cachingObjectConverter = true;

    protected boolean alwaysRedirectLocationToAbsoluteUrl = true;

    protected String defaultAsynchronousComponentWindowRenderingMode = "ajax";

    public ContainerConfiguration getContainerConfiguration() {
        return this.containerConfiguration;
    }

    public void setContainerConfiguration(ContainerConfiguration containerConfiguration) {
        this.containerConfiguration = containerConfiguration;
    }

    public HstManager getHstManager() {
        return hstManager;
    }

    public void setHstManager(HstManager hstManager) {
        this.hstManager = hstManager;
    }

    public HstSiteMapMatcher getSiteMapMatcher() {
        return siteMapMatcher;
    }

    public void setSiteMapMatcher(HstSiteMapMatcher siteMapMatcher) {
        this.siteMapMatcher = siteMapMatcher;
    }

    public HstRequestContextComponent getRequestContextComponent() {
        return this.requestContextComponent;
    }

    public void setRequestContextComponent(HstRequestContextComponent requestContextComponent) {
        this.requestContextComponent = requestContextComponent;
    }

    public HstComponentFactory getComponentFactory() {
        return this.componentFactory;
    }

    public void setComponentFactory(HstComponentFactory componentFactory) {
        this.componentFactory = componentFactory;
    }

    public HstComponentWindowFactory getComponentWindowFactory() {
        return this.componentWindowFactory;
    }

    public void setComponentWindowFactory(HstComponentWindowFactory componentWindowFactory) {
        this.componentWindowFactory = componentWindowFactory;
    }

    public HstComponentInvoker getComponentInvoker() {
        return this.componentInvoker;
    }

    public void setComponentInvoker(HstComponentInvoker componentInvoker) {
        this.componentInvoker = componentInvoker;
    }

    public HstURLFactory getUrlFactory() {
        return this.urlFactory;
    }

    public void setUrlFactory(HstURLFactory urlFactory) {
        this.urlFactory = urlFactory;
    }

    public HstLinkCreator getLinkCreator(){
        return this.linkCreator;
    }

    public void setLinkCreator(HstLinkCreator linkCreator) {
        this.linkCreator = linkCreator;
    }

    public void setSiteMenusManager(HstSiteMenusManager siteMenusManager) {
        this.siteMenusManager = siteMenusManager;
    }

    public HstSiteMenusManager getHstSiteMenusManager(){
        return this.siteMenusManager;
    }

    public HstQueryManagerFactory getHstQueryManagerFactory(){
        return this.hstQueryManagerFactory;
    }

    public void setHstQueryManagerFactory(HstQueryManagerFactory hstQueryManagerFactory){
        this.hstQueryManagerFactory = hstQueryManagerFactory;
    }

    public PageErrorHandler getDefaultPageErrorHandler() {
        return defaultPageErrorHandler;
    }

    public void setDefaultPageErrorHandler(PageErrorHandler defaultPageErrorHandler) {
        this.defaultPageErrorHandler = defaultPageErrorHandler;
    }

    public ResourceBundleRegistry getResourceBundleRegistry() {
        return resourceBundleRegistry;
    }

    public void setResourceBundleRegistry(ResourceBundleRegistry resourceBundleRegistry) {
        this.resourceBundleRegistry = resourceBundleRegistry;
    }

    public ContentBeansTool getContentBeansTool() {
        return contentBeansTool;
    }

    public void setContentBeansTool(ContentBeansTool contentBeansTool) {
        this.contentBeansTool = contentBeansTool;
    }

    public boolean isCachingObjectConverter() {
        return cachingObjectConverter;
    }

    public void setCachingObjectConverter(final boolean cachingObjectConverter) {
        this.cachingObjectConverter = cachingObjectConverter;
    }

    public abstract void invoke(ValveContext context) throws ContainerException;

    public void initialize() throws ContainerException {
    }

    public void destroy() {
    }

    public boolean isAlwaysRedirectLocationToAbsoluteUrl() {
        return alwaysRedirectLocationToAbsoluteUrl;
    }

    public void setAlwaysRedirectLocationToAbsoluteUrl(boolean alwaysRedirectLocationToAbsoluteUrl) {
        this.alwaysRedirectLocationToAbsoluteUrl = alwaysRedirectLocationToAbsoluteUrl;
    }

    public void setDefaultAsynchronousComponentWindowRenderingMode(String defaultAsynchronousComponentWindowRenderingMode) {
        this.defaultAsynchronousComponentWindowRenderingMode = StringUtils.defaultIfEmpty(defaultAsynchronousComponentWindowRenderingMode, "ajax");
    }

    protected HstComponentWindow findComponentWindow(HstComponentWindow rootWindow, String windowReferenceNamespace) {
        HstComponentWindow componentWindow = null;

        String rootReferenceNamespace = rootWindow.getReferenceNamespace();

        if (rootReferenceNamespace.equals(windowReferenceNamespace)) {
            componentWindow = rootWindow;
        } else {
            String [] rootReferenceNamespaces = rootReferenceNamespace.split(getComponentWindowFactory().getReferenceNameSeparator());
            String [] referenceNamespaces = windowReferenceNamespace.split(getComponentWindowFactory().getReferenceNameSeparator());
            int index = 0;
            while (index < rootReferenceNamespaces.length && index < referenceNamespaces.length && rootReferenceNamespaces[index].equals(referenceNamespaces[index])) {
                index++;
            }

            if (index < referenceNamespaces.length) {
                HstComponentWindow tempWindow = rootWindow;
                for ( ; index < referenceNamespaces.length; index++) {
                    if (tempWindow != null) {
                        tempWindow = tempWindow.getChildWindowByReferenceName(referenceNamespaces[index]);
                    } else {
                        break;
                    }
                }

                if (index == referenceNamespaces.length) {
                    componentWindow = tempWindow;
                }
            }
        }

        return componentWindow;
    }

    protected HstComponentWindow findErrorCodeSendingWindow(HstComponentWindow [] sortedComponentWindows) {
        for (HstComponentWindow window : sortedComponentWindows) {
            if (((HstComponentWindowImpl) window).getResponseState().getErrorCode() > 0) {
                return window;
            }
        }

        return null;
    }

    protected PageErrors getPageErrors(HstComponentWindow [] sortedComponentWindows, boolean clearExceptions) {
        List<KeyValue<HstComponentInfo, Collection<HstComponentException>>> componentExceptions = null;

        for (HstComponentWindow window : sortedComponentWindows) {
            if (window.hasComponentExceptions()) {
                if (componentExceptions == null) {
                    componentExceptions = new ArrayList<KeyValue<HstComponentInfo, Collection<HstComponentException>>>();
                }

                HstComponentInfo componentInfo = new DelegatingHstComponentInfo(window.getComponentInfo(), window.getComponentName());
                KeyValue<HstComponentInfo, Collection<HstComponentException>> pair =
                    new DefaultKeyValue<HstComponentInfo, Collection<HstComponentException>>(componentInfo, new ArrayList<HstComponentException>(window.getComponentExceptions()));
                componentExceptions.add(pair);

                if (clearExceptions) {
                    window.clearComponentExceptions();
                }
            }
        }

        if (componentExceptions != null && !componentExceptions.isEmpty()) {
            return new DefaultPageErrors(componentExceptions);
        } else {
            return null;
        }
    }

    protected PageErrorHandler.Status handleComponentExceptions(PageErrors pageErrors, HstContainerConfig requestContainerConfig, HstComponentWindow window, HstRequest hstRequest, HstResponse hstResponse) {
        if (!pageErrors.isEmpty()) {
            final HttpServletRequest request = hstRequest.getRequestContext().getServletRequest();
            String requestInfo = request.getRequestURI();
            if (request.getQueryString() != null) {
                requestInfo += "?" + request.getQueryString();
            }
            log.warn("Component exception(s) found in page request, '{}'.", requestInfo);
        }

        PageErrorHandler pageErrorHandler = (PageErrorHandler) hstRequest.getAttribute(ContainerConstants.CUSTOM_ERROR_HANDLER_PARAM_NAME);

        if (pageErrorHandler == null) {
            String pageErrorHandlerClassName = window.getPageErrorHandlerClassName();
            if(pageErrorHandlerClassName == null) {
                /* fallback to the original implementation through parametername/value. This is due to historical reasons and backwards
                 * compatibility
                 */
                pageErrorHandlerClassName = (String) window.getParameter(ContainerConstants.CUSTOM_ERROR_HANDLER_PARAM_NAME);
            }
            while (pageErrorHandlerClassName == null && window.getParentWindow() != null) {
                window = window.getParentWindow();
                pageErrorHandlerClassName = window.getPageErrorHandlerClassName();
                if(pageErrorHandlerClassName == null) {
                    /* fallback to the original implementation through parametername/value. This is due to historical reasons and backwards
                     * compatibility
                     */
                    pageErrorHandlerClassName = (String) window.getParameter(ContainerConstants.CUSTOM_ERROR_HANDLER_PARAM_NAME);
                }
            }

            if (pageErrorHandlerClassName != null) {
                try {
                    pageErrorHandler = getComponentFactory().getObjectInstance(requestContainerConfig, pageErrorHandlerClassName);
                } catch (Exception e) {
                    if (log.isDebugEnabled()) {
                        log.warn("Failed to get object of " + pageErrorHandlerClassName, e);
                    } else if (log.isWarnEnabled()) {
                        log.warn("Failed to get object of {}. {}", pageErrorHandlerClassName, e.toString());
                    }
                }
            }
        }

        if (pageErrorHandler == null) {
            pageErrorHandler = defaultPageErrorHandler;
        }

        if (pageErrorHandler == null) {
            return PageErrorHandler.Status.NOT_HANDLED;
        }

        try {
            return pageErrorHandler.handleComponentExceptions(pageErrors, hstRequest, hstResponse);
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.warn("Exception during custom error handling.", e);
            } else if (log.isWarnEnabled()) {
                log.warn("Exception during custom error handling. {}", e.toString());
            }

            return PageErrorHandler.Status.HANDLED_BUT_CONTINUE;
        }
    }

    /**
     * Creates HstPageInfo object by invoking the next valves with response wrapper
     *
     * @param context
     * @param timeToLiveSeconds
     * @return a Serializable value object for the page or page fragment
     * @throws Exception
     */
    protected HstPageInfo createHstPageInfoByInvokingNextValve(final ValveContext context, long timeToLiveSeconds) throws Exception {
        HstRequestContext requestContext = context.getRequestContext();
        final HttpServletResponse nonWrappedReponse = requestContext.getServletResponse();

        try {
            final ByteArrayOutputStream outstr = new ByteArrayOutputStream(4096);
            final GenericResponseWrapper responseWrapper = new GenericResponseWrapper(nonWrappedReponse, outstr);

            ((HstMutableRequestContext) requestContext).setServletResponse(responseWrapper);

            context.invokeNext();
            responseWrapper.flush();

            if (context.getServletRequest().getAttribute(ContainerConstants.HST_FORWARD_PATH_INFO) != null) {
                // page is forwarded. We cache empty placeholder ForwardPageInfo
                String forwardPathInfo = (String) context.getServletRequest().getAttribute(ContainerConstants.HST_FORWARD_PATH_INFO);
                return new ForwardPlaceHolderHstPageInfo(forwardPathInfo);
            }

            if (responseWrapper.getCookies() != null && !responseWrapper.getCookies().isEmpty()) {
                // we do not cache pages that contain cookies that are set during hst request rendering after the
                // page caching valve
                log.info("Response for '{}' contain cookies that are set after the page caching valve: Response won't be cached. " +
                        "Better to mark the hst component that sets cookies as uncacheable or load it asynchronous.",
                        context.getServletRequest().getRequestURI());
                return new UncacheableHstPageInfo(responseWrapper.getStatus(), responseWrapper.getContentType(),
                        responseWrapper.getCookies(), outstr.toByteArray(), responseWrapper.getCharacterEncoding(),
                        timeToLiveSeconds, responseWrapper.getAllHeaders());
            }

            String contentType = responseWrapper.getContentType();

            return new HstPageInfo(responseWrapper.getStatus(), contentType,
                    responseWrapper.getCookies(), outstr.toByteArray(), responseWrapper.getCharacterEncoding(),
                    timeToLiveSeconds, responseWrapper.getAllHeaders());
        } finally {
            ((HstMutableRequestContext) requestContext).setServletResponse(nonWrappedReponse);
        }
    }

    protected boolean isRequestCacheable(final ValveContext context) throws ContainerException {
        HttpServletRequest servletRequest = context.getServletRequest();
        if (servletRequest.getAttribute(PAGE_INFO_CACHEABLE) != null) {
            return ((Boolean)servletRequest.getAttribute(PAGE_INFO_CACHEABLE)).booleanValue();
        }

        boolean requestCacheable = isRequestCacheable(context.getRequestContext(), context);
        servletRequest.setAttribute(PAGE_INFO_CACHEABLE, requestCacheable);
        return  requestCacheable;
    }

    private boolean isRequestCacheable(final HstRequestContext requestContext, final ValveContext context) throws ContainerException {
        HttpServletRequest servletRequest = requestContext.getServletRequest();
        String method = servletRequest.getMethod();
        String requestURI = servletRequest.getRequestURI();

        if (!"GET".equals(method)) {
            log.debug("Only GET requests are cacheable. Skipping it because the request method is '{}'.", method);
            return false;
        }

        HstContainerURL baseURL = requestContext.getBaseURL();
        String actionWindowReferenceNamespace = baseURL.getActionWindowReferenceNamespace();

        if (actionWindowReferenceNamespace != null) {
            log.debug("Request '{}' is not cacheable because the url is action url.", requestURI);
            return false;
        }

        String resourceWindowRef = baseURL.getResourceWindowReferenceNamespace();

        if (resourceWindowRef != null) {
            log.debug("Request '{}' is not cacheable because the url is resource url.", requestURI);
            return false;
        }

        if (!context.getPageCacheContext().isCacheable()) {
            log.debug("Request '{}' is not cacheable because PageCacheContext is marked to not cache this request: {} ", requestURI, context.getPageCacheContext().getReasonsUncacheable());
            return false;
        }

        if (requestContext.isCmsRequest()) {
            log.debug("Request '{}' is not cacheable because request is cms request", requestURI);
            return false;
        }

        Mount mount = requestContext.getResolvedMount().getMount();

        if (mount.isPreview()) {
            log.debug("Request '{}' is not cacheable because request is preview request", requestURI);
            return false;
        }

        ResolvedSiteMapItem resolvedSitemapItem = requestContext.getResolvedSiteMapItem();

        if (resolvedSitemapItem != null) {
            if (!isSiteMapItemAndComponentConfigCacheable(resolvedSitemapItem, context)) {
                return false;
            }
        } else if (!mount.isCacheable()) {
            log.debug("Request '{}' is not cacheable because mount '{}' is not cacheable.", requestURI, mount.getName());
            return false;
        }
        return true;
    }

    private boolean isSiteMapItemAndComponentConfigCacheable(final ResolvedSiteMapItem resolvedSitemapItem,
            final ValveContext context) throws ContainerException {
        if (!resolvedSitemapItem.getHstSiteMapItem().isCacheable()) {
            log.debug("Request '{}' is not cacheable because hst sitemapitem '{}' is not cacheable.", context
                    .getServletRequest().getRequestURI(), resolvedSitemapItem.getHstSiteMapItem().getId());
            return false;
        }

        // check whether component rendering is true: For component rendering, we need to check whether the specific sub
        // component (tree) is cacheable
        String componentRenderingWindowReferenceNamespace = context.getRequestContext().getBaseURL()
                .getComponentRenderingWindowReferenceNamespace();
        if (componentRenderingWindowReferenceNamespace != null) {
            HstComponentWindow window = findComponentWindow(context.getRootComponentWindow(),
                    componentRenderingWindowReferenceNamespace);
            if (window == null) {
                // incorrect request.
                return false;
            }
            if (window.getComponentInfo().isStandalone()) {
                return window.getComponentInfo().isCompositeCacheable();
            }
            // normally component rendering is standalone, however, if not standalone, than also the
            // ancestors need to be cacheable because all components will be rendered
            if (!resolvedSitemapItem.getHstComponentConfiguration().isCompositeCacheable()) {
                log.debug("Request '{}' is not cacheable because hst component '{}' is not cacheable.", context
                        .getServletRequest().getRequestURI(), resolvedSitemapItem.getHstComponentConfiguration()
                        .getId());
                return false;
            }
        } else if (!resolvedSitemapItem.getHstComponentConfiguration().isCompositeCacheable()) {
            log.debug("Request '{}' is not cacheable because hst component '{}' is not cacheable.", context
                    .getServletRequest().getRequestURI(), resolvedSitemapItem.getHstComponentConfiguration().getId());
            return false;
        }

        return true;
    }

}
