package org.hippoecm.hst.core.container;

import org.hippoecm.hst.configuration.HstSites;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapMatcher;
import org.hippoecm.hst.core.container.Valve;
import org.hippoecm.hst.core.container.ValveContext;
import org.hippoecm.hst.core.domain.DomainMappings;
import org.hippoecm.hst.core.request.HstRequestContext;

public abstract class AbstractValve implements Valve
{
    protected HstSites hstSites;
    protected DomainMappings domainMappings;
    protected HstSiteMapMatcher siteMapMatcher;
    protected HstRequestProcessor requestProcessor;
    
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

    public HstRequestProcessor getRequestProcessor() {
        return requestProcessor;
    }

    public void setRequestProcessor(HstRequestProcessor requestProcessor) {
        this.requestProcessor = requestProcessor;
    }

    public abstract void invoke(HstRequestContext request, ValveContext context) throws ContainerException;

    public void initialize() throws ContainerException {
    }
    
    protected boolean isActionRequest() {
        return false;
    }
    
    protected boolean isResourceRequest() {
        return false;
    }
    
}
