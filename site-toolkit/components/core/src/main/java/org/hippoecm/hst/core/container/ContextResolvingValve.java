package org.hippoecm.hst.core.container;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.hippoecm.hst.configuration.HstSite;
import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapMatcher.MatchResult;
import org.hippoecm.hst.core.component.HstComponent;
import org.hippoecm.hst.core.component.HstComponentFactory;
import org.hippoecm.hst.core.domain.DomainMapping;
import org.hippoecm.hst.core.request.HstRequestContext;

public class ContextResolvingValve extends AbstractValve
{
    
    @Override
    public void invoke(HstRequestContext request, ValveContext context) throws ContainerException
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
        MatchResult matchResult = this.siteMapMatcher.match(pathInfo, hstSite);
        
        if (matchResult == null) {
            throw new ContainerException("No match for " + pathInfo);
        }
        
        HstComponentConfiguration rootCompConfig = matchResult.getCompontentConfiguration();
        
        HstComponent rootHstComponent = buildHstComponent(rootCompConfig);
        
        // continue
        context.invokeNext(request);
    }
    
    protected HstComponent buildHstComponent(HstComponentConfiguration compConfig) throws ContainerException {
        HstComponent hstComponent = null;

        try {
            String componentClassName = compConfig.getComponentClassName();
            Map<String, Object> properties = compConfig.getProperties();
            String referenceName = compConfig.getReferenceName();
            String renderPath = compConfig.getRenderPath();
            String contextRelativePath = compConfig.getContextRelativePath();
            String componentContentBasePath = compConfig.getComponentContentBasePath();
            Map<String, HstComponentConfiguration> children = compConfig.getChildren();

            Class componentClass = Class.forName(componentClassName);
            
            if (HstComponentFactory.class.isAssignableFrom(componentClass)) {
                HstComponentFactory factory = (HstComponentFactory) componentClass.newInstance();
                factory.init(properties);
                hstComponent = (HstComponent) factory.newInstance();
            } else if (HstComponent.class.isAssignableFrom(componentClass)) {
                hstComponent = (HstComponent) componentClass.newInstance();
                hstComponent.init(properties);
            }
        } catch (Exception e) {
            throw new ContainerException(e);
        }
        
        return hstComponent;
    }
    
}
