/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.platform.container.sitemapitemhandler;

import javax.servlet.ServletContext;

import org.hippoecm.hst.configuration.sitemapitemhandlers.HstSiteMapItemHandlerConfiguration;
import org.hippoecm.hst.core.request.SiteMapItemHandlerConfiguration;
import org.hippoecm.hst.core.sitemapitemhandler.HstSiteMapItemHandler;
import org.hippoecm.hst.core.sitemapitemhandler.HstSiteMapItemHandlerException;
import org.hippoecm.hst.site.request.SiteMapItemHandlerConfigurationImpl;

/**
 * HstSiteMapItemHandlerFactoryImpl
 * 
 */
public class HstSiteMapItemHandlerFactoryImpl implements HstSiteMapItemHandlerFactory {

    protected HstSiteMapItemHandlerRegistryImpl siteMapItemHandlerRegistry;
    private ServletContext websiteServletContext;

    public HstSiteMapItemHandlerFactoryImpl(final HstSiteMapItemHandlerRegistryImpl siteMapItemHandlerRegistry,
                                            final ServletContext websiteServletContext) {
        this.siteMapItemHandlerRegistry = siteMapItemHandlerRegistry;
        this.websiteServletContext = websiteServletContext;
    }
    
    public HstSiteMapItemHandler getSiteMapItemHandlerInstance(HstSiteMapItemHandlerConfiguration handlerConfig) throws HstSiteMapItemHandlerException {
        
        String handlerId = handlerConfig.getId() + handlerConfig.hashCode();
        HstSiteMapItemHandler handler = this.siteMapItemHandlerRegistry.getSiteMapItemHandler(handlerId);
        
        if (handler == null) {
            String siteMapItemHandlerClassName = handlerConfig.getSiteMapItemHandlerClassName();

            final ClassLoader websiteClassloader = websiteServletContext.getClassLoader();
            final ClassLoader currentClassloader = Thread.currentThread().getContextClassLoader();

            try {
                if (websiteClassloader != currentClassloader) {
                    Thread.currentThread().setContextClassLoader(websiteClassloader);
                }
                
                Class<?> handlerClass = websiteClassloader.loadClass(siteMapItemHandlerClassName);
                if(!HstSiteMapItemHandler.class.isAssignableFrom(handlerClass)) {
                   throw new HstSiteMapItemHandlerException("Cannot instantiate HstSiteMapItemHandler: The class '"+siteMapItemHandlerClassName+"' of '" + handlerId + "' is not a subtype of '"+HstSiteMapItemHandler.class.getName()+"'. "); 
                }
                handler = (HstSiteMapItemHandler) handlerClass.newInstance();
               
                SiteMapItemHandlerConfiguration handlerConfigImpl = new SiteMapItemHandlerConfigurationImpl(handlerConfig); 
                
                handler.init(websiteServletContext, handlerConfigImpl);

            } catch (ClassNotFoundException e) {
                throw new HstSiteMapItemHandlerException(String.format("Cannot find the class of  %s: %s", handlerId, siteMapItemHandlerClassName));
            } catch (InstantiationException e) {
                throw new HstSiteMapItemHandlerException(String.format("Cannot instantiate the class of %s: %s", handlerId, siteMapItemHandlerClassName));
            } catch (IllegalAccessException e) {
                throw new HstSiteMapItemHandlerException(String.format("Illegal access to the class of %s: %s", handlerId, siteMapItemHandlerClassName));
            } finally {
                if (websiteClassloader != currentClassloader) {
                    Thread.currentThread().setContextClassLoader(currentClassloader);
                }                
            }

            this.siteMapItemHandlerRegistry.registerSiteMapItemHandler(handlerId, handler);

        }
        return handler;
    }
  
}
