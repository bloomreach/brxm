/*
 *  Copyright 2008-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.site.container;

import java.io.Serializable;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.onehippo.cms7.services.context.HippoWebappContext;
import org.onehippo.cms7.services.context.HippoWebappContextRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HstContextLoaderListener, the default HST Site Container Configuration/Initialization/Destroying Listener.
 *
 * <P>
 * This can be used as a servlet context listener, configured like the following in web.xml:
 * </P>
 * <PRE><CODE>
 *  &lt;listener>
 *    &lt;listener-class>org.hippoecm.hst.site.container.HstContextLoaderListener&lt;/listener-class>
 *  &lt;/listener>
 * </CODE></PRE>
 *
 * <P>
 * This listener first {@link HippoWebappContextRegistry#register(HippoWebappContext) registers a HippoWebappContext}
 * of type {@link HippoWebappContext.Type#SITE}, and then invokes {@link DefaultHstSiteConfigurer} to load HST Context
 * and initialize the container.
 * Please be referred to {@link DefaultHstSiteConfigurer} to see how it finds and loads configurations in detail.
 * </P>
 */
public class HstContextLoaderListener implements ServletContextListener, Serializable {

    private static final long serialVersionUID = 1L;

    private static Logger log = LoggerFactory.getLogger(HstContextLoaderListener.class);

    private HippoWebappContext webappContext;
    private HstSiteConfigurer siteConfigurer;

    public HstContextLoaderListener() {
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        try {
            webappContext = new HippoWebappContext(HippoWebappContext.Type.SITE, sce.getServletContext());
            HippoWebappContextRegistry.get().register(webappContext);
            siteConfigurer = new DefaultHstSiteConfigurer();
            ((DefaultHstSiteConfigurer) siteConfigurer).setServletContext(sce.getServletContext());
            siteConfigurer.initialize();
        } catch (Exception e) {
            log.error("Error occurred while initializing HstSiteConfigurer.", e);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        HippoWebappContextRegistry.get().unregister(webappContext);
        if (siteConfigurer != null) {
            try {
                siteConfigurer.destroy();
            } catch (Exception e) {
                log.error("Error occurred while destroying HstSiteConfigurer.", e);
            }
        }
    }

}
