package org.hippoecm.hst.core.container;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.configuration.HstSitesManager;
import org.hippoecm.hst.configuration.components.DelegatingHstComponentInfo;
import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.configuration.components.HstComponentInfo;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.component.HstURLFactory;
import org.hippoecm.hst.core.hosting.VirtualHostsManager;
import org.hippoecm.hst.core.linking.HstLinkCreator;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.HstRequestContextComponent;
import org.hippoecm.hst.core.request.HstSiteMapMatcher;
import org.hippoecm.hst.core.search.HstQueryManagerFactory;
import org.hippoecm.hst.core.sitemenu.HstSiteMenusManager;
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
    protected Map<String, HstSitesManager> sitesManagers;
    protected VirtualHostsManager virtualHostsManager;
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
    
    protected String traceToolComponentName = "hstTraceToolComponent";
    protected String traceToolComponentClassName = "org.hippoecm.hst.component.support.tool.HstTraceToolComponent";
        
    public ContainerConfiguration getContainerConfiguration() {
        return this.containerConfiguration;
    }
    
    public void setContainerConfiguration(ContainerConfiguration containerConfiguration) {
        this.containerConfiguration = containerConfiguration;
    }
    
    public HstSitesManager getSitesManager(String name) {
        return sitesManagers.get(name);
    }

    public void setSitesManagers(Map<String, HstSitesManager> sitesManagers) {
        this.sitesManagers = sitesManagers;
    }

    public VirtualHostsManager getVirtualHostsManager() {
        return virtualHostsManager;
    }

    public void setVirtualHostsManager(VirtualHostsManager virtualHostsManager) {
        this.virtualHostsManager = virtualHostsManager;
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
    
    public abstract void invoke(ValveContext context) throws ContainerException;

    public void initialize() throws ContainerException {
    }
    
    public void destroy() {
    }
    
    public void setTraceToolComponentName(String traceToolComponentName) {
        this.traceToolComponentName = StringUtils.trim(traceToolComponentName);
    }
    
    public String getTraceToolComponentName() {
        return this.traceToolComponentName;
    }

    public void setTraceToolComponentClassName(String traceToolComponentClassName) {
        this.traceToolComponentClassName = StringUtils.trim(traceToolComponentClassName);
    }
    
    public String getTraceToolComponentClassName() {
        return this.traceToolComponentClassName;
    }
    
    protected HstComponentWindow createTraceToolComponent(ValveContext context, HstRequestContext requestContext, HstComponentWindow parentWindow) {
        HstComponentWindow traceToolComponentWindow = null;
        final String traceCompName = getTraceToolComponentName();
        final String traceToolCompClassName = getTraceToolComponentClassName();

        if (traceToolCompClassName != null && !"".equals(traceToolCompClassName)) {
            try {
                HstComponentConfiguration compConfig = new HstComponentConfiguration() {
                    public SortedMap<String, HstComponentConfiguration> getChildren() { return null; }
                    public HstComponentConfiguration getChildByName(String name) { return null; }
                    public String getComponentClassName() { return traceToolCompClassName; }
                    public String getId() { return traceCompName; }
                    public String getName() { return traceCompName; }
                    public String getParameter(String name) { return null; }
                    public Map<String, String> getParameters() { return null; }
                    public String getReferenceName() { return traceCompName; }
                    public String getRenderPath() { return null; }
                    public String getServeResourcePath() { return null; }
                    public HstComponentConfiguration getParent() { return null; }
					public String getLocalParameter(String name) {return null;}
					public Map<String, String> getLocalParameters() {return null;}
                };
                
                traceToolComponentWindow = getComponentWindowFactory().create(context.getRequestContainerConfig(), requestContext, compConfig, getComponentFactory(), parentWindow);
            } catch (Exception e) {
                if (log.isDebugEnabled()) {
                    log.warn("Failed to create hstTraceTool component windows.", e);
                } else if (log.isWarnEnabled()) {
                    log.warn("Failed to create hstTraceTool component windows: {}", e.toString());
                }
            }
        }
        
        return traceToolComponentWindow;
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
                
                HstComponentInfo componentInfo = new DelegatingHstComponentInfo(window.getComponentInfo());
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
        PageErrorHandler pageErrorHandler = (PageErrorHandler) hstRequest.getAttribute(ContainerConstants.CUSTOM_ERROR_HANDLER_PARAM_NAME);
        
        if (pageErrorHandler == null) {
            String pageErrorHandlerClassName = (String) window.getParameter(ContainerConstants.CUSTOM_ERROR_HANDLER_PARAM_NAME);
            
            while (pageErrorHandlerClassName == null && window.getParentWindow() != null) {
                window = window.getParentWindow();
                pageErrorHandlerClassName = (String) window.getParameter(ContainerConstants.CUSTOM_ERROR_HANDLER_PARAM_NAME);
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
