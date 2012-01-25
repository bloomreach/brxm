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
package org.hippoecm.hst.core.sitemapitemhandler;

import org.hippoecm.hst.configuration.sitemapitemhandlers.HstSiteMapItemHandlerConfiguration;
import org.hippoecm.hst.core.container.HstContainerConfig;
import org.hippoecm.hst.core.request.SiteMapItemHandlerConfiguration;
import org.hippoecm.hst.site.request.SiteMapItemHandlerConfigurationImpl;

/**
 * HstSiteMapItemHandlerFactoryImpl
 * 
 */
public class HstSiteMapItemHandlerFactoryImpl implements HstSiteMapItemHandlerFactory {
    
    protected HstSiteMapItemHandlerRegistry siteMapItemHandlerRegistry;
    
    public HstSiteMapItemHandlerFactoryImpl(HstSiteMapItemHandlerRegistry siteMapItemHandlerRegistry) {
        this.siteMapItemHandlerRegistry = siteMapItemHandlerRegistry;
    }
    
    public HstSiteMapItemHandler getSiteMapItemHandlerInstance(HstContainerConfig requestContainerConfig, HstSiteMapItemHandlerConfiguration handlerConfig) throws HstSiteMapItemHandlerException {
        
        String handlerId = handlerConfig.getId() + handlerConfig.hashCode();
        HstSiteMapItemHandler handler = this.siteMapItemHandlerRegistry.getSiteMapItemHandler(requestContainerConfig, handlerId);
        
        if (handler == null) {
            boolean initialized = false;
            String siteMapItemHandlerClassName = handlerConfig.getSiteMapItemHandlerClassName();
            
            ClassLoader containerClassloader = requestContainerConfig.getContextClassLoader();
            ClassLoader currentClassloader = Thread.currentThread().getContextClassLoader();

            try {
                if (containerClassloader != currentClassloader) {
                    Thread.currentThread().setContextClassLoader(containerClassloader);
                }
                
                Class<?> handlerClass = containerClassloader.loadClass(siteMapItemHandlerClassName);
                if(!HstSiteMapItemHandler.class.isAssignableFrom(handlerClass)) {
                   throw new HstSiteMapItemHandlerException("Cannot instantiate HstSiteMapItemHandler: The class '"+siteMapItemHandlerClassName+"' of '" + handlerId + "' is not a subtype of '"+HstSiteMapItemHandler.class.getName()+"'. "); 
                }
                handler = (HstSiteMapItemHandler) handlerClass.newInstance();
               
                SiteMapItemHandlerConfiguration handlerConfigImpl = new SiteMapItemHandlerConfigurationImpl(handlerConfig); 
                
                handler.init(requestContainerConfig.getServletContext(), handlerConfigImpl);
                
                initialized = true;
            } catch (ClassNotFoundException e) {
                throw new HstSiteMapItemHandlerException("Cannot find the class of " + handlerId + ": " + siteMapItemHandlerClassName);
            } catch (InstantiationException e) {
                throw new HstSiteMapItemHandlerException("Cannot instantiate the class of " + handlerId + ": " + siteMapItemHandlerClassName);
            } catch (IllegalAccessException e) {
                throw new HstSiteMapItemHandlerException("Illegal access to the class of " + handlerId + ": " + siteMapItemHandlerClassName);
            } finally {
                if (containerClassloader != currentClassloader) {
                    Thread.currentThread().setContextClassLoader(currentClassloader);
                }                
            }
            
            if (initialized) {
                this.siteMapItemHandlerRegistry.registerSiteMapItemHandler(requestContainerConfig, handlerId, handler);
            }
        }
        return handler;
    }
  
}
