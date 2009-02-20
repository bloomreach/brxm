package org.hippoecm.hst.core.container;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hippoecm.hst.configuration.HstSiteMapMatcher;
import org.hippoecm.hst.configuration.HstSites;
import org.hippoecm.hst.core.component.HstURLFactory;
import org.hippoecm.hst.core.domain.DomainMappings;
import org.hippoecm.hst.core.linking.HstLinkCreator;
import org.hippoecm.hst.core.request.HstRequestContextComponent;

public abstract class AbstractValve implements Valve
{
    protected static Log log = LogFactory.getLog(AbstractValve.class);
    
    protected HstSites hstSites;
    protected DomainMappings domainMappings;
    protected HstSiteMapMatcher siteMapMatcher;
    protected HstRequestContextComponent requestContextComponent;
    protected HstComponentFactory componentFactory;
    protected HstComponentWindowFactory componentWindowFactory;
    protected HstComponentInvoker componentInvoker;
    protected HstURLFactory urlFactory;
    protected HstLinkCreator linkCreator;
    protected HstContainerURLParser containerURLParser;
    
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
    
    public HstContainerURLParser getContainerURLParser() {
        return this.containerURLParser;
    }
    
    public void setContainerURLParser(HstContainerURLParser containerURLParser) {
        this.containerURLParser = containerURLParser;
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
