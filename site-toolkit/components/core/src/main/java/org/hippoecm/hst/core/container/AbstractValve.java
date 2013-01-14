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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.hippoecm.hst.configuration.components.DelegatingHstComponentInfo;
import org.hippoecm.hst.configuration.components.HstComponentInfo;
import org.hippoecm.hst.configuration.model.HstManager;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.component.HstURLFactory;
import org.hippoecm.hst.core.internal.HstRequestContextComponent;
import org.hippoecm.hst.core.linking.HstLinkCreator;
import org.hippoecm.hst.core.request.HstSiteMapMatcher;
import org.hippoecm.hst.core.search.HstQueryManagerFactory;
import org.hippoecm.hst.core.sitemenu.HstSiteMenusManager;
import org.hippoecm.hst.resourcebundle.ResourceBundleRegistry;
import org.hippoecm.hst.util.DefaultKeyValue;
import org.hippoecm.hst.util.KeyValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AbstractValve
 * 
 * @version $Id$
 */
public abstract class AbstractValve implements Valve {
    
    protected final static Logger log = LoggerFactory.getLogger(AbstractValve.class);
    
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
    
    protected boolean alwaysRedirectLocationToAbsoluteUrl = true;
    
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
    
}
