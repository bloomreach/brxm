package org.hippoecm.hst.core.container;

import java.util.Map;

import org.hippoecm.hst.configuration.HstSitesManager;
import org.hippoecm.hst.core.component.HstURLFactory;
import org.hippoecm.hst.core.domain.DomainMappings;
import org.hippoecm.hst.core.linking.HstLinkCreator;
import org.hippoecm.hst.core.request.HstRequestContextComponent;
import org.hippoecm.hst.core.request.HstSiteMapMatcher;
import org.hippoecm.hst.core.search.HstCtxWhereClauseComputer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractValve implements Valve
{
    protected final static Logger log = LoggerFactory.getLogger(AbstractValve.class);
    protected Map<String, HstSitesManager> sitesManagers;
    protected DomainMappings domainMappings;
    protected HstSiteMapMatcher siteMapMatcher;
    protected HstRequestContextComponent requestContextComponent;
    protected HstComponentFactory componentFactory;
    protected HstComponentWindowFactory componentWindowFactory;
    protected HstComponentInvoker componentInvoker;
    protected HstURLFactory urlFactory;
    protected HstLinkCreator linkCreator;
    protected HstCtxWhereClauseComputer ctxWhereClauseComputer;
    
    public HstSitesManager getSitesManager(String name) {
        return sitesManagers.get(name);
    }

    public void setSitesManagers(Map<String, HstSitesManager> sitesManagers) {
        this.sitesManagers = sitesManagers;
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
    
    public HstCtxWhereClauseComputer getCtxWhereClauseComputer(){
        return this.ctxWhereClauseComputer;
    }
    
    public void setCtxWhereClauseComputer(HstCtxWhereClauseComputer ctxWhereClauseComputer){
        this.ctxWhereClauseComputer = ctxWhereClauseComputer;
    }
    
    public abstract void invoke(ValveContext context) throws ContainerException;

    public void initialize() throws ContainerException {
    }
    
}
