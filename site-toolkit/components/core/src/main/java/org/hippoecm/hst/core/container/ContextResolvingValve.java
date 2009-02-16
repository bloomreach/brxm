package org.hippoecm.hst.core.container;

import javax.servlet.http.HttpServletRequest;

import org.hippoecm.hst.configuration.HstSite;
import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapMatcher.MatchResult;
import org.hippoecm.hst.core.component.HstComponentWindow;
import org.hippoecm.hst.core.domain.DomainMapping;

public class ContextResolvingValve extends AbstractValve
{
    
    @Override
    public void invoke(ValveContext context) throws ContainerException
    {
        HttpServletRequest servletRequest = (HttpServletRequest) context.getServletRequest();
        
        String domainName = servletRequest.getServerName();
        DomainMapping domainMapping = this.domainMappings.findDomainMapping(domainName);
        
        if (domainMapping == null) {
            throw new ContainerException("No domain mapping for " + domainName);
        }

        String siteName = domainMapping.getSiteName();
        HstSite hstSite = this.hstSites.getSite(siteName);
        
        if (hstSite == null) {
            throw new ContainerException("No site found for " + siteName);
        }
        
        String pathInfo = servletRequest.getPathInfo();
        
        MatchResult matchResult = null;
        
        try {
            matchResult = this.siteMapMatcher.match(pathInfo, hstSite);
        } catch (Exception e) {
            throw new ContainerException("No match for " + pathInfo, e);
        }
        
        if (matchResult == null) {
            throw new ContainerException("No match for " + pathInfo);
        }
        
        HstComponentConfiguration rootComponentConfig = matchResult.getCompontentConfiguration();
        
        try {
            HstComponentWindow rootComponentWindow = getComponentWindowFactory().create(rootComponentConfig, getComponentFactory());
            context.setRootComponentWindow(rootComponentWindow);
        } catch (Exception e) {
            throw new ContainerException("Failed to create component window for the configuration, " + rootComponentConfig.getId(), e);
        }
        
        // continue
        context.invokeNext();
    }
    
}
