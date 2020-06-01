/*
 *  Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.platform.container;

import org.hippoecm.hst.core.internal.PlatformModelAvailableService;
import org.hippoecm.hst.core.internal.PreviewDecorator;
import org.hippoecm.hst.platform.model.HstModelRegistryImpl;
import org.hippoecm.hst.platform.services.PlatformServicesImpl;
import org.hippoecm.hst.site.container.HstContextLoaderListener;
import org.hippoecm.hst.site.request.PreviewDecoratorImpl;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.ProxiedServiceHolder;
import org.onehippo.cms7.services.ProxiedServiceTracker;
import org.onehippo.cms7.services.context.HippoWebappContext;
import org.onehippo.cms7.services.context.HippoWebappContextRegistry;
import org.onehippo.repository.RepositoryService;

/**
 * HstPlatformContextLoaderListener, the HST Platform Container Configuration/Initialization/Destroying Listener.
 *
 * <P>
 * This must be used as a servlet context listener, configured like the following in the web.xml for platform:
 * </P>
 * <PRE><CODE>
 *  &lt;listener>
 *    &lt;listener-class>org.hippoecm.hst.platform.container.HstPlatformContextLoaderListener&lt;/listener-class>
 *  &lt;/listener>
 * </CODE></PRE>
 *
 * <P>
 * This listener first {@link HippoWebappContextRegistry#register(HippoWebappContext) registers a HippoWebappContext}
 * of type {@link HippoWebappContext.Type#PLATFORM}, and then waits for the {@link RepositoryService} to be available
 * to initialize the HstPlaform. It will delegating back to the parent {@link HstContextLoaderListener} to load
 * the platform HST Context and initialize the container. Finally it will register the (internal)
 * {@link PlatformModelAvailableService} service to trigger regular {@link HippoWebappContext.Type#SITE}s to initialize
 * thereafter.
 * </P>
 */
public class HstPlatformContextLoaderListener extends HstContextLoaderListener {

    private final HstModelRegistryImpl hstModelRegistry = new HstModelRegistryImpl();
    private final PlatformServicesImpl platformServices = new PlatformServicesImpl();
    private final PreviewDecorator previewDecorator = new PreviewDecoratorImpl();
    private final PlatformModelAvailableService platformModelAvailableService = new PlatformModelAvailableService(){};
    private ProxiedServiceTracker<RepositoryService> repositoryServiceTracker;

    protected HippoWebappContext.Type getContextType() {
        return HippoWebappContext.Type.PLATFORM;
    }

    @Override
    protected void initialize() {
        repositoryServiceTracker = new ProxiedServiceTracker<RepositoryService>() {
            @Override
            public void serviceRegistered(final ProxiedServiceHolder<RepositoryService> serviceHolder) {
                initializeHstPlaform(serviceHolder.getServiceProxy());
            }

            @Override
            public void serviceUnregistered(final ProxiedServiceHolder<RepositoryService> serviceHolder) {
                destroyHstPlatform();
            }
        };
        HippoServiceRegistry.addTracker(repositoryServiceTracker, RepositoryService.class);
    }

    @Override
    protected void destroy() {
        if (repositoryServiceTracker != null) {
            HippoServiceRegistry.removeTracker(repositoryServiceTracker, RepositoryService.class);
            repositoryServiceTracker = null;
            destroyHstPlatform();
        }
    }

    protected void initializeHstPlaform(final RepositoryService repositoryService) {
        // HSTTWO-4355 TODO: we may wany to setup a separate *parent* Spring Context for platform specific Spring wiring
        //                   for now doing required wiring inline here, using the provided Repository but also temporarily
        //                   (mis?)using the hstconfigreader.delegating credentials from registering hst sites (see HstModelRegistryImpl)

        platformServices.setHstModelRegistry(hstModelRegistry);
        platformServices.setPreviewDecorator(previewDecorator);
        platformServices.init();
        hstModelRegistry.setRepository(repositoryService);
        hstModelRegistry.init();

        configureHstSite(hstModelRegistry);

        HippoServiceRegistry.register(platformModelAvailableService, PlatformModelAvailableService.class);
    }

    protected void destroyHstPlatform() {
        HippoServiceRegistry.unregister(platformModelAvailableService, PlatformModelAvailableService.class);
        destroyHstSite();
        hstModelRegistry.destroy();
        hstModelRegistry.setRepository(null);
        platformServices.destroy();

        platformServices.setPreviewDecorator(null);
        platformServices.setHstModelRegistry(null);
    }
}
