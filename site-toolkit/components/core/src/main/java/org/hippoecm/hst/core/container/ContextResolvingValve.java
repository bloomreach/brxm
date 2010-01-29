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

import java.io.IOException;

import org.hippoecm.hst.configuration.HstSite;
import org.hippoecm.hst.configuration.HstSitesManager;
import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.core.request.HstEmbeddedRequestContext;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.site.request.HstRequestContextImpl;

public class ContextResolvingValve extends AbstractValve
{
    
    @Override
    public void invoke(ValveContext context) throws ContainerException
    {
        HstComponentConfiguration rootComponentConfig = null;
        
        HstRequestContextImpl requestContext = (HstRequestContextImpl) context.getServletRequest().getAttribute(ContainerConstants.HST_REQUEST_CONTEXT);
        HstContainerURL baseURL = requestContext.getBaseURL();
        HstEmbeddedRequestContext erc = requestContext.getEmbeddedRequestContext();
        
        String siteName = requestContext.getMatchedMapping().getSiteName();
        
        if (erc != null) {
            // DEBUG check we actually have a matching site...
            if (!siteName.equals(erc.getSiteName())) {
                throw new ContainerException("Site doesn't match up with Embedded HST request context site");
            }
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
            
            HstSitesManager mngr = getSitesManager(context.getServletRequest().getServletPath());
            
            if(mngr == null) {
                throw new ContainerException("No site manager found for " + context.getServletRequest().getServletPath() + ". Make sure you sitesManagers in your Spring " +
                		"configuration contains an entry for '"+context.getServletRequest().getServletPath()+"'");
            }
            
            HstSite hstSite = mngr.getSites().getSite(siteName);
            
            if (hstSite == null) {
                throw new ContainerException("No site found for " + siteName);
            }
            
            String pathInfo = baseURL.getPathInfo();
            
            ResolvedSiteMapItem resolvedSiteMapItem = null;
            
            try {
                resolvedSiteMapItem = this.siteMapMatcher.match(pathInfo, hstSite);
            } catch (Exception e) {
                throw new ContainerNotFoundException("No match for " + pathInfo, e);
            }
            
            if (resolvedSiteMapItem == null) {
                throw new ContainerNotFoundException("No match for " + pathInfo);
            }
            if (resolvedSiteMapItem.getErrorCode() > 0) {
                
                try {
                    if (log.isDebugEnabled()) {
                        log.debug("The resolved sitemap item for {} has error status: {}", pathInfo, Integer.valueOf(resolvedSiteMapItem.getErrorCode()));
                    }           
                    context.getServletResponse().sendError(resolvedSiteMapItem.getErrorCode());
                    
                } catch (IOException e) {
                    if (log.isDebugEnabled()) {
                        log.warn("Exception invocation on sendError().", e);
                    } else if (log.isWarnEnabled()) {
                        log.warn("Exception invocation on sendError().");
                    }
                }
                // we're done: jump out pipeline processing
                return;
                
            } else {
                
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
        }
            
        if(rootComponentConfig == null) {
            throw new ContainerNotFoundException("Resolved siteMapItem does not contain a ComponentConfiguration that can be resolved." + baseURL.getPathInfo());
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
}
