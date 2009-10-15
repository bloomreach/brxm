package org.hippoecm.hst.core.container;

import java.util.Map;
import java.util.SortedMap;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.configuration.HstSitesManager;
import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.core.component.HstURLFactory;
import org.hippoecm.hst.core.hosting.VirtualHostsManager;
import org.hippoecm.hst.core.linking.HstLinkCreator;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.HstRequestContextComponent;
import org.hippoecm.hst.core.request.HstSiteMapMatcher;
import org.hippoecm.hst.core.search.HstQueryManagerFactory;
import org.hippoecm.hst.core.sitemenu.HstSiteMenusManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractValve implements Valve
{
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

}
