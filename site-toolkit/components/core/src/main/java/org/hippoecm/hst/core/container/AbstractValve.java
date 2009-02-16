package org.hippoecm.hst.core.container;

import org.hippoecm.hst.configuration.HstSites;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapMatcher;
import org.hippoecm.hst.core.component.HstComponentFactory;
import org.hippoecm.hst.core.component.HstComponentWindowFactory;
import org.hippoecm.hst.core.domain.DomainMappings;
import org.hippoecm.hst.core.request.HstRequestContextComponent;

public abstract class AbstractValve implements Valve
{
    protected HstSites hstSites;
    protected DomainMappings domainMappings;
    protected HstSiteMapMatcher siteMapMatcher;
    protected HstRequestContextComponent requestContextComponent;
    protected HstComponentFactory componentFactory;
    protected HstComponentWindowFactory componentWindowFactory;
    protected HstComponentInvokerProvider componentInvokerProvider;
    
    public HstSites getHstSites() {
        return hstSites;
    }

    public void setHstSites(HstSites hstSites) {
        this.hstSites = hstSites;
    }

    public DomainMappings getDomainMappings() {
        return domainMappings;
    }

    public void setDomainMappings(DomainMappings domainMappings) {
        this.domainMappings = domainMappings;
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
    
    public HstComponentInvokerProvider getComponentInvokerProvider() {
        return this.componentInvokerProvider;
    }

    public void setComponentInvokerProvider(HstComponentInvokerProvider componentInvokerProvider) {
        this.componentInvokerProvider = componentInvokerProvider;
    }
    
    public HstComponentInvoker getComponentInvoker(String contextName) {
        HstComponentInvoker invoker = null;
        
        if (this.componentInvokerProvider != null) {
            invoker = this.componentInvokerProvider.getComponentInvoker(contextName);
        }
        
        return invoker;
    }

    public abstract void invoke(ValveContext context) throws ContainerException;

    public void initialize() throws ContainerException {
    }
    
    protected boolean isActionRequest() {
        return false;
    }
    
    protected boolean isResourceRequest() {
        return false;
    }
    
}
