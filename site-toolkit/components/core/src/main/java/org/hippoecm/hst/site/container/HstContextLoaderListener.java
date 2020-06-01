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

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.hippoecm.hst.core.container.ContainerException;
import org.hippoecm.hst.core.internal.PlatformModelAvailableService;
import org.hippoecm.hst.platform.model.HstModelRegistry;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.ProxiedServiceHolder;
import org.onehippo.cms7.services.ProxiedServiceTracker;
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
 * of type {@link HippoWebappContext.Type#SITE}, and then waits for the {@link PlatformModelAvailableService} to be
 * available before using a {@link DefaultHstSiteConfigurer} to load HST Context and initialize the container.
 * Please refer to {@link DefaultHstSiteConfigurer} to see how it finds and loads configurations in detail.
 * </P>
 */
public class HstContextLoaderListener implements ServletContextListener {

    private static Logger log = LoggerFactory.getLogger(HstContextLoaderListener.class);

    private HippoWebappContext webappContext;
    private ProxiedServiceTracker<PlatformModelAvailableService> hstPlatformModelAvailableServiceTracker;
    private HstSiteConfigurer siteConfigurer;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        webappContext = new HippoWebappContext(getContextType(), sce.getServletContext());
        HippoWebappContextRegistry.get().register(webappContext);
        initialize();
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        destroy();
        HippoWebappContextRegistry.get().unregister(webappContext);
        webappContext = null;
    }

    protected HippoWebappContext.Type getContextType() {
        return HippoWebappContext.Type.SITE;
    }

    protected void initialize() {
        hstPlatformModelAvailableServiceTracker = new ProxiedServiceTracker<PlatformModelAvailableService>() {
            @Override
            public void serviceRegistered(final ProxiedServiceHolder<PlatformModelAvailableService> serviceHolder) {
                final HstModelRegistry modelRegistry = HippoServiceRegistry.getService(HstModelRegistry.class);
                configureHstSite(modelRegistry);
            }

            @Override
            public void serviceUnregistered(final ProxiedServiceHolder<PlatformModelAvailableService> serviceHolder) {
                destroyHstSite();
            }
        };
        HippoServiceRegistry.addTracker(hstPlatformModelAvailableServiceTracker, PlatformModelAvailableService.class);
    }

    protected void destroy() {
        if (hstPlatformModelAvailableServiceTracker != null) {
            HippoServiceRegistry.removeTracker(hstPlatformModelAvailableServiceTracker, PlatformModelAvailableService.class);
            hstPlatformModelAvailableServiceTracker = null;
            destroyHstSite();
        }
    }

    protected void configureHstSite(final HstModelRegistry hstModelRegistry) {
        siteConfigurer = new DefaultHstSiteConfigurer();
        ((DefaultHstSiteConfigurer) siteConfigurer).setServletContext(webappContext.getServletContext());
        try {
            siteConfigurer.initialize(hstModelRegistry);
        } catch (ContainerException e) {
            log.error("Error occurred while initializing HstSiteConfigurer.", e);
        }
    }

    protected void destroyHstSite() {
        if (siteConfigurer != null) {
            try {
                siteConfigurer.destroy();
            } catch (Exception e) {
                log.error("Error occurred while destroying HstSiteConfigurer.", e);
            }
            siteConfigurer = null;
        }
    }
}
