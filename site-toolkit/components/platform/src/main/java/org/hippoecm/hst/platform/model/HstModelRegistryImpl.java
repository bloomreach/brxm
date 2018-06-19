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

import java.util.HashMap;
import java.util.Map;

import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.hst.configuration.model.HstNode;
import org.hippoecm.hst.core.container.ComponentManager;
import org.hippoecm.hst.core.container.ContainerConfiguration;
import org.hippoecm.hst.platform.configuration.cache.HstConfigurationLoadingCache;
import org.hippoecm.hst.platform.configuration.cache.HstNodeLoadingCache;
import org.hippoecm.hst.platform.configuration.hosting.VirtualHostsService;
import org.hippoecm.hst.platform.configuration.model.ConfigurationNodesLoadingException;
import org.hippoecm.hst.site.HstServices;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.hst.configuration.HstNodeTypes.NODENAME_HST_CONFIGURATIONS;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODETYPE_HST_HST;

public class HstModelRegistryImpl implements HstModelRegistry {

    private static final Logger log = LoggerFactory.getLogger(HstModelRegistryImpl.class);

    private volatile Map<String, HstModel> models = new HashMap<>();

    private Repository repository;
    private Credentials credentials;

    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    public void setCredentials(final Credentials credentials) {
        this.credentials = credentials;
    }

    private void init() {
        HippoServiceRegistry.registerService(this, HstModelRegistry.class);
    }

    private void stop() {
        HippoServiceRegistry.unregisterService(this, HstModelRegistry.class);
        // TODO HSTTWO-4355 should we unregister all hst models as well?
    }

    // TODO HSTTWO-4355 register listeners for jcr events!!
    // TODO HSTTWO-4355 if a root hst:hst node gets deleted, remove the listener and remove from virtualHostsSuppliers
    @Override
    public HstModel registerHstModel(final String contextPath, final ClassLoader websiteClassLoader,
                                     final ComponentManager websiteComponentManager, final boolean loadHstConfigNodes) throws ModelRegistrationException {
        Session session = null;
        if (models.containsKey(contextPath)) {
            throw new IllegalStateException(String.format("There is already an HstModel registered for contextPath '%s'", contextPath));
        }
        try {
            session = repository.login(credentials);
            final ContainerConfiguration websiteContainerConfiguration = websiteComponentManager.getComponent("containerConfiguration");
            final String rootPath = websiteContainerConfiguration.getString("hst.configuration.rootPath", null);
            if (rootPath == null) {
                throw new ModelRegistrationException(String.format("Cannot register model for context '{}' since missing 'hst.configuration.rootPath' property", contextPath));
            }
            if (!session.nodeExists(rootPath)) {
                throw new ModelRegistrationException(String.format("Cannot register model for context '{}' since 'hst.configuration.rootPath' points to nonexisting " +
                        "jcr node '{}'", contextPath, rootPath));
            }

            final Node hstHstNode = session.getNode(rootPath);
            if (!hstHstNode.isNodeType(NODETYPE_HST_HST)) {
                throw new ModelRegistrationException(String.format("Cannot register model for context '{}' since 'hst.configuration.rootPath' points to " +
                        "jcr node that is not of type '{}'", contextPath, NODETYPE_HST_HST));
            }

            final HstNodeLoadingCache hstNodeLoadingCache = new HstNodeLoadingCache(repository, credentials, hstHstNode.getPath());

            final HstConfigurationLoadingCache hstConfigurationLoadingCache = new HstConfigurationLoadingCache(hstNodeLoadingCache,
                    hstNodeLoadingCache.getRootPath() + "/" + NODENAME_HST_CONFIGURATIONS + "/");

            if (loadHstConfigNodes) {
                loadHstConfigNodes(websiteClassLoader, hstNodeLoadingCache);
            }

            final HstModelImpl model = new HstModelImpl(contextPath, websiteClassLoader, websiteComponentManager, hstNodeLoadingCache, hstConfigurationLoadingCache);
            models.put(contextPath, model);

            log.info("Registered HstModel for '{}'", contextPath);
            return model;
        } catch (LoginException e) {
            throw new ModelRegistrationException("Cannot login JCR Session", e);
        } catch (RepositoryException e) {
            throw new ModelRegistrationException("Cannot create HstModelRegistryImpl", e);
        } catch (Exception e) {
            throw new ModelRegistrationException("Cannot create HstModelRegistryImpl", e);
        } finally {
            if (session != null) {
                session.logout();
            }
        }
    }

    @Override
    public void unregisterHstModel(final String contextPath) {
        // TODO HSTTWO-4355 should this do more? Potentially stop the website component manager as well if needed?
        final HstModel remove = models.remove(contextPath);
        if (remove == null) {
            throw new ModelRegistrationException(String.format("Could not remove HstModel for '{}' since no such model present", contextPath));
        } else {
            log.info("Unregistered HstModel for '{}'", contextPath);
        }
    }

    @Override
    public HstModel getHstModel(final String contextPath) {

        final HstModel model = models.get(contextPath);
        if (model == null) {
            throw new IllegalArgumentException(String.format("No HstModel present for context '{}'", contextPath));
        }
        return model;

    }

    private void loadHstConfigNodes(final ClassLoader websiteClassLoader, final HstNodeLoadingCache hstNodeLoadingCache) throws InterruptedException {
        final long start = System.currentTimeMillis();
        // triggers the loading of all the hst configuration nodes
        HstNode root = null;
        while (root == null) {
            try {
                root = hstNodeLoadingCache.getNode(hstNodeLoadingCache.getRootPath());
                // don't sweat to much, sleep for 250 ms
                Thread.sleep(250);
            } catch (ConfigurationNodesLoadingException e) {
                if (log.isDebugEnabled()) {
                    log.info("Exception while trying to load the HST configuration nodes. Try again.", e);
                } else {
                    log.info("Exception while trying to load the HST configuration nodes. Try again. Reason: {}", e.getMessage());
                }
            }
        }
        log.info("Loaded all HST Configuraion JCR nodes in {} ms.", (System.currentTimeMillis() - start));
        // use the right class loader
        ClassLoader currentClassloader = Thread.currentThread().getContextClassLoader();
        try {
            if (websiteClassLoader != currentClassloader) {
                Thread.currentThread().setContextClassLoader(websiteClassLoader);
            }
            HstServices.setHstConfigurationNodesLoaded(true);
        } catch (Exception e) {
            log.error("Exception loading model", e);
            throw e;
        } finally {
            if (websiteClassLoader != currentClassloader) {
                Thread.currentThread().setContextClassLoader(currentClassloader);
            }
        }
    }
}
