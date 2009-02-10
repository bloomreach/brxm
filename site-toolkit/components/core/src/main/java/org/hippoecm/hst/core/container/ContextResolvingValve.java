package org.hippoecm.hst.core.container;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.hippoecm.hst.configuration.HstSite;
import org.hippoecm.hst.configuration.HstSites;
import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.configuration.sitemap.HstSiteMap;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.core.component.DefaultHstComponentImpl;
import org.hippoecm.hst.core.component.HstComponent;
import org.hippoecm.hst.core.component.HstComponentFactory;
import org.hippoecm.hst.core.request.HstRequestContext;

public class ContextResolvingValve extends AbstractValve
{
    protected HstSites hstSites;
    
    public ContextResolvingValve(HstSites hstSites) {
        super();
        
        this.hstSites = hstSites;
    }
    
    @Override
    public void invoke(HstRequestContext request, ValveContext context) throws ContainerException
    {
        String siteName = null;
        String componentPath = null;
        HstSite hstSite = null;
        
        HttpServletRequest servletRequest = (HttpServletRequest) context.getServletRequest();
        String pathInfo = servletRequest.getPathInfo();
        
        if (pathInfo != null) {
            int offset = pathInfo.indexOf("/", 1);
            
            if (offset != -1) {
                siteName = pathInfo.substring(1, offset);
                componentPath = pathInfo.substring(offset);
            } else {
                siteName = pathInfo.substring(1);
            }
            
            hstSite = this.hstSites.getSite(siteName);
        }
        
        if (siteName == null) {
            throw new ContainerException("No site found for " + pathInfo);
        }
        
        if (componentPath == null) {
            throw new ContainerException("No component found for " + pathInfo);
        }
        
        HstSiteMap siteMap = hstSite.getSiteMap();
        HstSiteMapItem siteMapItem = siteMap.getSiteMapItem(componentPath);
        String componentConfigurationId = siteMapItem.getComponentConfigurationId();
        HstComponentConfiguration componentConfiguration = hstSite.getComponentsConfiguration().getComponentConfiguration(componentConfigurationId);
        HstComponent hstComponent = buildHstComponent(componentConfiguration);
        
        // continue
        context.invokeNext(request);
    }
    
    protected HstComponent buildHstComponent(HstComponentConfiguration componentConfiguration) throws ContainerException {
        HstComponent hstComponent = null;

        try {
            String componentClassName = componentConfiguration.getComponentClassName();
            Map<String, Object> properties = componentConfiguration.getProperties();
            String referenceName = componentConfiguration.getReferenceName();
            String renderPath = componentConfiguration.getRenderPath();
            String contextRelativePath = componentConfiguration.getContextRelativePath();
            String componentContentBasePath = componentConfiguration.getComponentContentBasePath();
            Map<String, HstComponentConfiguration> children = componentConfiguration.getChildren();

            Class componentClass = Class.forName(componentClassName);
            
            if (HstComponentFactory.class.isAssignableFrom(componentClass)) {
                HstComponentFactory factory = (HstComponentFactory) componentClass.newInstance();
                factory.init(properties);
                HstComponent delegated = (HstComponent) factory.newInstance();
            } else if (HstComponent.class.isAssignableFrom(componentClass)) {
                HstComponent delegated = (HstComponent) componentClass.newInstance();
                hstComponent = new DefaultHstComponentImpl(delegated);
                hstComponent.init(properties);
            }
        } catch (Exception e) {
            throw new ContainerException(e);
        }
        
        return hstComponent;
    }
    
}
