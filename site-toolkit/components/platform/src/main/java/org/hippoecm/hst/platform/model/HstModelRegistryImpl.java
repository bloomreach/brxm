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
package org.hippoecm.hst.platform.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import org.hippoecm.hst.core.container.ComponentManager;
import org.hippoecm.hst.core.linking.HstLinkCreator;
import org.hippoecm.hst.core.linking.HstLinkProcessor;
import org.hippoecm.hst.core.linking.LocationResolver;
import org.hippoecm.hst.core.linking.ResourceContainer;
import org.hippoecm.hst.core.linking.RewriteContextResolver;
import org.hippoecm.hst.platform.configuration.cache.HstConfigurationLoadingCache;
import org.hippoecm.hst.platform.configuration.cache.HstNodeLoadingCache;
import org.hippoecm.hst.platform.configuration.model.ModelLoadingException;
import org.hippoecm.hst.platform.linking.DefaultHstLinkCreator;
import org.hippoecm.hst.platform.linking.DefaultRewriteContextResolver;
import org.hippoecm.hst.platform.linking.HstLinkProcessorChain;
import org.hippoecm.hst.platform.linking.containers.DefaultResourceContainer;
import org.hippoecm.hst.platform.linking.containers.HippoGalleryAssetSet;
import org.hippoecm.hst.platform.linking.containers.HippoGalleryImageSetContainer;
import org.hippoecm.hst.platform.linking.resolvers.HippoResourceLocationResolver;
import org.hippoecm.repository.util.NodeIterable;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.hst.configuration.HstNodeTypes.HST_HST_PROPERTY_CONTEXT_PATH;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODENAME_HST_CONFIGURATIONS;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODETYPE_HST_HST;

public class HstModelRegistryImpl implements HstModelRegistry {

    private static final Logger log = LoggerFactory.getLogger(HstModelRegistryImpl.class);

    private volatile Map<String, Supplier<HstModelImpl>> modelSuppliers = new HashMap<>();

    private Repository repository;
    private Credentials credentials;

    public HstModelRegistryImpl() {
        HippoServiceRegistry.registerService(this, HstModelRegistry.class);
    }


    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    public void setCredentials(final Credentials credentials) {
        this.credentials = credentials;
    }

    private void stop() {
        HippoServiceRegistry.unregisterService(this, HstModelRegistry.class);
    }

    // TODO register listeners for jcr events!!
    // TODO if a root hst:hst node gets deleted, remove the listener and remove from virtualHostsSuppliers
    public void load() {
        Session session =null;
        try {
            session = repository.login(credentials);
            for (Node child : new NodeIterable(session.getRootNode().getNodes())) {
                try {
                    if (!child.isNodeType(NODETYPE_HST_HST)) {
                        continue;
                    }
                    if (!child.hasProperty("hst:contextpath")) {
                        log.error("Cannot load hst config '{}' since no contextpath configured on '{}'",
                                child.getPath(), child.getPath());
                        continue;
                    }
                    // TODO replace with correct constant below
                    final String contextPath = child.getProperty(HST_HST_PROPERTY_CONTEXT_PATH).getString();
                    final HstNodeLoadingCache hstNodeLoadingCache = new HstNodeLoadingCache(repository, credentials, child.getPath());

                    final HstConfigurationLoadingCache hstConfigurationLoadingCache = new HstConfigurationLoadingCache(hstNodeLoadingCache,
                            hstNodeLoadingCache.getRootPath() + "/" + NODENAME_HST_CONFIGURATIONS + "/");

                    modelSuppliers.put(contextPath, () -> new HstModelImpl(contextPath, hstNodeLoadingCache, hstConfigurationLoadingCache));

                } catch (RepositoryException e) {
                    log.error("Could not load '{}'", child.getPath(), e);
                }
            }
        } catch (LoginException e) {
            throw new ModelLoadingException("Cannot login JCR Session", e);
        } catch (RepositoryException e) {
            throw new ModelLoadingException("Cannot create HstModelRegistryImpl", e);
        } finally {
            if (session != null) {
                session.logout();
            }
        }
    }

    @Override
    public HstModel getHstModel(final String contextPath, final ComponentManager websiteComponentManager) {

        final Supplier<HstModelImpl> hstModelSupplier = modelSuppliers.get(contextPath);

        if (hstModelSupplier == null) {
            throw new ModelLoadingException(String.format("Cannot load an HST model for contextPath '%s'", contextPath));
        }

        // make sure that the Thread class loader during model loading is the platform classloader!
        ClassLoader platformClassloader = hstModelSupplier.getClass().getClassLoader();
        ClassLoader currentClassloader = Thread.currentThread().getContextClassLoader();
        try {
            if (platformClassloader != currentClassloader) {
                Thread.currentThread().setContextClassLoader(platformClassloader);
            }
            hstModelSupplier.getClass().getClassLoader();
            // TODO support retries in case of model loading failure and synchronization and caching
            final HstModelImpl hstModel = hstModelSupplier.get();

            // Set the custom website webapp specific configurations for for example linkrewriting
            configureSiteMapMatcher(websiteComponentManager, hstModel);
            configureHstLinkCreator(websiteComponentManager, hstModel);

            // TODO
            // TODO configureChannelManager(websiteComponentManager, hstModel);

            return hstModel;
        } catch (Exception e) {
           log.error("Exception loading model", e);
           throw e;
        } finally {
            if (platformClassloader != currentClassloader) {
                Thread.currentThread().setContextClassLoader(currentClassloader);
            }
        }
    }

    private void configureSiteMapMatcher(final ComponentManager websiteComponentManager, final HstModelImpl hstModel) {
        hstModel.getHstSiteMapMatcherImpl().setLinkProcessor(getHstLinkProcessor(websiteComponentManager));
    }

    private HstLinkProcessor getHstLinkProcessor(final ComponentManager websiteComponentManager) {
        HstLinkProcessor customLinkProcessor = websiteComponentManager.getComponent(HstLinkProcessor.class.getName());
        if (customLinkProcessor != null) {
            return customLinkProcessor;
        }
        return new HstLinkProcessorChain();
    }

    private RewriteContextResolver getRewriteContextResolver(final ComponentManager websiteComponentManager) {
        final RewriteContextResolver customRewriteContextResolver = websiteComponentManager.getComponent(RewriteContextResolver.class.getName());
        if (customRewriteContextResolver != null) {
            return customRewriteContextResolver;
        }
        return new DefaultRewriteContextResolver();
    }

    private void configureHstLinkCreator(final ComponentManager websiteComponentManager, final HstModelImpl hstModel) {
        final DefaultHstLinkCreator hstLinkCreator = hstModel.getHstLinkCreatorImpl();
        final List<String> binaryLocations = websiteComponentManager.getComponent(HstLinkCreator.class.getName() + ".binaryLocations");
        hstLinkCreator.setBinaryLocations(binaryLocations.toArray(new String[binaryLocations.size()]));
        hstLinkCreator.setPageNotFoundPath(websiteComponentManager.getComponent(HstLinkCreator.class.getName() + ".pageNotFoundPath"));

        hstLinkCreator.setRewriteContextResolver(getRewriteContextResolver(websiteComponentManager));

        hstLinkCreator.setLinkProcessor(getHstLinkProcessor(websiteComponentManager));

        final List<ResourceContainer> immutableResourceContainers = getImmutableResoureceContainers(websiteComponentManager);

        final List<LocationResolver> immutableLocationResolvers =
                getImmutableResoureceResolvers(websiteComponentManager, immutableResourceContainers, binaryLocations.toArray(new String[binaryLocations.size()]));

        hstLinkCreator.setLocationResolvers(immutableLocationResolvers);
    }

    private List<LocationResolver> getImmutableResoureceResolvers(final ComponentManager websiteComponentManager,
                                                                  final List<ResourceContainer> resourceContainers,
                                                                  final String[] binaryLocations) {

        List<LocationResolver> locationsResolvers = new ArrayList<>();
        // the spring config id is customResourceResolvers instead of customLocationResolvers
        List<LocationResolver> customLocationResolvers = websiteComponentManager.getComponent("customResourceResolvers");

        locationsResolvers.addAll(customLocationResolvers);

        final HippoResourceLocationResolver hippoResourceLocationResolver = new HippoResourceLocationResolver();
        hippoResourceLocationResolver.setBinaryLocations(binaryLocations);
        hippoResourceLocationResolver.setResourceContainers(resourceContainers);

        locationsResolvers.add(hippoResourceLocationResolver);

        return locationsResolvers;
    }

    private List<ResourceContainer> getImmutableResoureceContainers(final ComponentManager websiteComponentManager) {

        List<ResourceContainer> resourceContainers = new ArrayList<>();
        // first add the custom resourceContainers, after that the fallback built in resource containers
        List<ResourceContainer> customResourceContainers = websiteComponentManager.getComponent("customResourceContainers");
        resourceContainers.addAll(customResourceContainers);

        final HippoGalleryImageSetContainer hippoGalleryImageSetContainer = new HippoGalleryImageSetContainer();
        hippoGalleryImageSetContainer.setPrimaryItem("hippogallery:original");
        hippoGalleryImageSetContainer.setMappings(ImmutableMap.of("hippogallery:thumbnail", "thumbnail"));

        resourceContainers.add(hippoGalleryImageSetContainer);

        final HippoGalleryAssetSet hippoGalleryAssetSet = new HippoGalleryAssetSet();
        hippoGalleryAssetSet.setPrimaryItem("hippogallery:asset");
        hippoGalleryAssetSet.setMappings(ImmutableMap.of());

        resourceContainers.add(hippoGalleryAssetSet);
        resourceContainers.add(new DefaultResourceContainer());
        return ImmutableList.copyOf(resourceContainers);
    }

}
