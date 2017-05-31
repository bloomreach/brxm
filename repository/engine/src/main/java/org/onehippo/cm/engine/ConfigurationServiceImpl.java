/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cm.engine;

import java.io.IOException;
import java.io.InputStream;

import javax.jcr.NamespaceException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang3.time.StopWatch;
import org.onehippo.cm.ConfigurationService;
import org.onehippo.cm.model.ClasspathConfigurationModelReader;
import org.onehippo.cm.model.ConfigurationModel;
import org.onehippo.cm.model.impl.ConfigurationModelImpl;
import org.onehippo.cm.model.parser.ParserException;
import org.onehippo.repository.bootstrap.util.BootstrapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.repository.api.HippoNodeType.HIPPO_LOCK;
import static org.onehippo.cm.engine.Constants.HCM_NAMESPACE;
import static org.onehippo.cm.engine.Constants.HCM_PREFIX;
import static org.onehippo.cm.engine.Constants.HCM_ROOT;
import static org.onehippo.cm.engine.Constants.HCM_ROOT_PATH;
import static org.onehippo.cm.engine.Constants.HIPPO_NAMESPACE;
import static org.onehippo.cm.engine.Constants.HIPPO_PREFIX;
import static org.onehippo.cm.engine.Constants.NT_HCM_ROOT;
import static org.onehippo.cm.model.Constants.HCM_CONFIG_FOLDER;

public class ConfigurationServiceImpl implements ConfigurationService {

    private static final Logger log = LoggerFactory.getLogger(ConfigurationServiceImpl.class);

    private final Session session;
    private final ConfigurationLockManager lockManager;
    private final ConfigurationBaselineService baselineService;
    private final ConfigurationConfigService configService;
    private final ConfigurationContentService contentService;

    /* TODO refactor after HCM-55
     * For now, storing the read result and merged model in an instance variable. This should be refactored for a
     * couple of reasons .
     *
     * The code in #initializeRepositoryConfiguration should really be in #contentBootstrap, but that is not possible
     * now, as then then the code trips on deleting the property lock that is set in
     * org.hippoecm.repository.LocalHippoRepository#initialize; line 283: initializationProcessor.lock(lockSession);
     * HCM-55 will likely introduce a mechanism so that locked property can be ignored.
     *
     * See also https://issues.onehippo.com/browse/REPO-1236
     *
     * Once the code is moved, the model will likely be loaded in #contentBootstrap and the need for these instance
     * variables will be gone.
     */
    private ConfigurationModelImpl configurationModel;

    public ConfigurationServiceImpl(final Session session) throws RepositoryException {
        this.session = session;
        lockManager = new ConfigurationLockManager(session);
        baselineService = new ConfigurationBaselineService(session, lockManager);
        configService = new ConfigurationConfigService();
        contentService = new ConfigurationContentService();
    }

    public boolean isNew() throws RepositoryException {
        return !(session.getWorkspace().getNodeTypeManager().hasNodeType(NT_HCM_ROOT) && session.nodeExists(HCM_ROOT_PATH));
    }

    /**
     * Perform initial repository configuration, including creating (claiming) a Repository initialization scope
     * lock, which only will be released through {@link #finishConfigureRepository()}
     */
    public void startConfigureRepository() throws RepositoryException {
        boolean started = false;
        ensureInitialized();
        lockManager.lock();
        try {
            // TODO when merging this code into LocalHippoRepository, use verifyOnly=false parameter
            configurationModel = new ClasspathConfigurationModelReader().read(Thread.currentThread().getContextClassLoader(), true);

            apply(configurationModel);

            if (Boolean.getBoolean("repo.yaml.verify")) {
                log.info("starting YAML verification");
                apply(configurationModel);
                log.info("YAML verification complete");
            }

            // don't fail starting up the repository storing the baseline or content
            try {
                baselineService.storeBaseline(configurationModel);
            } catch (RepositoryException|IOException|ConfigurationRuntimeException e) {
                if (log.isDebugEnabled()) {
                    log.debug("Failed to store the Configuration baseline", e);
                } else {
                    log.error("Failed to store the Configuration baseline", e.getMessage());
                }
            }
            try {
                contentService.apply(configurationModel, session);
            } catch (RepositoryException|ConfigurationRuntimeException e) {
                if (log.isDebugEnabled()) {
                    log.debug("Failed to apply all content", e);
                } else {
                    log.error("Failed to apply all content", e.getMessage());
                }
            }
            started = true;
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.debug("Bootstrap configuration failed!", e);
            } else {
                log.error("Bootstrap configuration failed!", e.getMessage());
            }
            if (e instanceof RepositoryException) {
                throw (RepositoryException)e;
            }
            if (e instanceof RuntimeException) {
                throw (RuntimeException)e;
            }
            throw new RuntimeException(e);
        } finally {
            if (!started) {
                lockManager.unlock();
            }
        }
    }

    /**
     * Execute additional tasks (if any) after the Repository has been started (virtual layer, Modules, security, etc.)
     */
    public void postStartRepository() {
        ensureInitialized();
        try {
            configService.writeWebfiles(configurationModel, session);
        } catch (IOException e) {
            if (log.isDebugEnabled()) {
                log.error("Error initializing webfiles", e);
            } else {
                log.error("Error initializing webfiles", e.getMessage());
            }
        }
        try {
            // We're completely done with the configurationModel at this point, so clean up its resources
            configurationModel.close();
        }
        catch (Exception e) {
            log.error("Error closing configuration ConfigurationModel", e);
        }
    }

    /**
     * Execute cleanup tasks (if any) after the Repository has been initialized, and at least release the lock
     * created by {@link #startConfigureRepository()}.
     */
    public void finishConfigureRepository() {
        try {
            lockManager.unlock();
        } catch (Exception e) {
            log.error("Failed to release the Bootstrap configuration lock", e);
        }
    }

    @Override
    public void apply(final ConfigurationModel model)
            throws RepositoryException, ParserException, IOException {
        ensureInitialized();
        lockManager.lock();
        try {
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();

            ConfigurationModel baseline = loadBaseline();
            if (baseline == null) {
                baseline = new ConfigurationModelImpl().build();
            }

            configService.computeAndWriteDelta(baseline, model, session, false);
            session.save();

            stopWatch.stop();
            log.info("ConfigurationModel applied in {}", stopWatch.toString());
        }
        catch (RepositoryException|ParserException|IOException e) {
            log.warn("Failed to apply configuration", e);
            throw e;
        } finally {
            lockManager.unlock();
        }
    }

    @Override
    public ConfigurationModel loadBaseline() throws RepositoryException, ParserException, IOException {
        ensureInitialized();
        return baselineService.loadBaseline();
    }

    @Override
    public boolean matchesBaselineManifest(final ConfigurationModel model) throws RepositoryException, IOException {
        ensureInitialized();
        return baselineService.matchesBaselineManifest(model);
    }

    public void shutdown() {
        if (configurationModel != null) {
            try {
                // Ensure configurationModel resources are cleaned up (if any)
                configurationModel.close();
            }
            catch (Exception e) {
                log.error("Error closing configuration ConfigurationModel", e);
            }
        }
        lockManager.shutdown();
        if (session != null && session.isLive()) {
            session.logout();
        }
    }

    private boolean isNamespaceRegistered(final String prefix) throws RepositoryException {
        try {
            session.getNamespaceURI(prefix);
            return true;
        } catch (NamespaceException e) {
            return false;
        }
    }

    private synchronized void ensureInitialized() {
        try {
            if (isNew()) {
                if (!isNamespaceRegistered(HIPPO_PREFIX)) {
                    session.getWorkspace().getNamespaceRegistry().registerNamespace(HIPPO_PREFIX, HIPPO_NAMESPACE);
                }
                if (!isNamespaceRegistered(HCM_PREFIX)) {
                    session.getWorkspace().getNamespaceRegistry().registerNamespace(HCM_PREFIX, HCM_NAMESPACE);
                }
                if (!session.getWorkspace().getNodeTypeManager().hasNodeType(HIPPO_LOCK)) {
                    try (InputStream is = getClass().getResourceAsStream("/"+HCM_CONFIG_FOLDER+"/hippo.cnd")) {
                        BootstrapUtils.initializeNodetypes(session, is, "hippo.cnd");
                    }
                }
                if (!session.getWorkspace().getNodeTypeManager().hasNodeType(NT_HCM_ROOT)) {
                    try (InputStream is = getClass().getResourceAsStream("/"+HCM_CONFIG_FOLDER+"/hcm.cnd")) {
                        BootstrapUtils.initializeNodetypes(session, is, "hcm.cnd");
                    }
                }
                Node hcmRootNode = session.getRootNode().addNode(HCM_ROOT, NT_HCM_ROOT);
                hcmRootNode.addNode(HIPPO_LOCK, HIPPO_LOCK);
                session.save();
            }
        } catch (IOException|RepositoryException e) {
            throw new ConfigurationRuntimeException(e);
        }
    }
}
