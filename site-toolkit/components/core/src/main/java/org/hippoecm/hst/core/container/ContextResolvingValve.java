/*
 *  Copyright 2008 Hippo.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.hst.core.container;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.core.request.HstEmbeddedRequestContext;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.site.request.HstRequestContextImpl;

/**
 * ContextResolvingValve
 * 
 * @version $Id$
 */
public class ContextResolvingValve extends AbstractValve
{
    
    @Override
    public void invoke(ValveContext context) throws ContainerException
    {
        HstComponentConfiguration rootComponentConfig = null;
        
        HstRequestContextImpl requestContext = (HstRequestContextImpl) context.getServletRequest().getAttribute(ContainerConstants.HST_REQUEST_CONTEXT);
        HstContainerURL baseURL = requestContext.getBaseURL();
        HstEmbeddedRequestContext erc = requestContext.getEmbeddedRequestContext();
        
        if (erc != null) {
            // we will use the embedded request context provided ResolvedSiteMapItem and root HstComponentConfiguration
            requestContext.setResolvedSiteMapItem(erc.getResolvedSiteMapItem());
            rootComponentConfig = (HstComponentConfiguration)context.getServletRequest().getAttribute(ContainerConstants.HST_EMBEDDED_REQUEST_CONTEXT_TARGET);
            
            // build and set the embedded component reference contextName needed proper parameter encoding and resolving
            StringBuilder contextNamespaceBuilder = new StringBuilder();
            HstComponentConfiguration compConfig = rootComponentConfig;
            String referenceNameSeparator = getComponentWindowFactory().getReferenceNameSeparator();
            if (compConfig != erc.getRootComponentConfig()) {
                do {
                    contextNamespaceBuilder.insert(0, compConfig.getReferenceName());
                    compConfig = compConfig.getParent();
                    if (compConfig == erc.getRootComponentConfig()) {
                        break;
                    }
                    contextNamespaceBuilder.insert(0, referenceNameSeparator);
                } while (true);
            }
            requestContext.setContextNamespace(contextNamespaceBuilder.toString());
        }
        else {
            
            ResolvedSiteMapItem resolvedSiteMapItem = requestContext.getResolvedSiteMapItem();
            if (resolvedSiteMapItem == null) {
                throw new ContainerException("At this point the requestContext must contain a resolvedSiteMapItem");
            }

            requestContext.setResolvedSiteMapItem(resolvedSiteMapItem);

            if (!requestContext.isPortletContext()) {
                rootComponentConfig = resolvedSiteMapItem.getHstComponentConfiguration();
            } else {
                rootComponentConfig = resolvedSiteMapItem.getPortletHstComponentConfiguration();

                if (rootComponentConfig == null) {
                    rootComponentConfig = resolvedSiteMapItem.getHstComponentConfiguration();
                }
            }
         
        }
            
        if (rootComponentConfig == null) {
            throw new ContainerNotFoundException("Resolved siteMapItem does not contain a ComponentConfiguration that can be resolved." + baseURL.getPathInfo());
        }
        
        String targetComponentPath = (String) context.getServletRequest().getAttribute(ContainerConstants.HST_REQUEST_CONTEXT_TARGET_COMPONENT_PATH);
        
        if (targetComponentPath != null) {
            rootComponentConfig = findTargetComponentConfiguration(rootComponentConfig, targetComponentPath);
        }
        
        if (log.isDebugEnabled()) {
            log.debug("Matched root component config for {}: {}", baseURL.getPathInfo(), rootComponentConfig.getId());
        }
        
        try {
            HstComponentWindow rootComponentWindow = getComponentWindowFactory().create(context.getRequestContainerConfig(), requestContext, rootComponentConfig, getComponentFactory());
            context.setRootComponentWindow(rootComponentWindow);
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.warn("Failed to create component windows: {}", e.toString(), e);
            } else if (log.isWarnEnabled()) {
                log.warn("Failed to create component windows: {}", e.toString());
            }
            
            throw new ContainerException("Failed to create component window for the configuration: " + rootComponentConfig.getId(), e);
        }
        
        // continue
        context.invokeNext();
    }
    
    protected HstComponentConfiguration findTargetComponentConfiguration(HstComponentConfiguration rootComponentConfig, String targetComponentPath) {
        HstComponentConfiguration targetComponentConfig = null;
        
        try {
            String [] childComponentNames = StringUtils.splitPreserveAllTokens(StringUtils.removeEnd(StringUtils.removeStart(targetComponentPath, "/"), "/"), '/');
            
            targetComponentConfig = rootComponentConfig;
            
            for (String childComponentName : childComponentNames) {
                targetComponentConfig = targetComponentConfig.getChildByName(childComponentName);
                
                if (targetComponentConfig == null) {
                    throw new IllegalArgumentException("Invalid child component name: " + childComponentName);
                }
            }
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.warn("Failed to find child component configuration: {}", e.toString(), e);
            } else if (log.isWarnEnabled()) {
                log.warn("Failed to find child component configuration: {}", e.toString());
            }
        }
        
        if (targetComponentConfig == null) {
            if (log.isWarnEnabled()) {
                log.warn("Failed to find target component configuration by " + targetComponentPath + ". The default root component configuration will be used.");
            }
        }
        
        return (targetComponentConfig != null ? targetComponentConfig : rootComponentConfig);
    }
}
