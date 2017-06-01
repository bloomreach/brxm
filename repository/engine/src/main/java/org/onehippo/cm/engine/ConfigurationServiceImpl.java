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
import org.onehippo.cm.engine.autoexport.AutoExportServiceImpl;
import org.onehippo.cm.model.ClasspathConfigurationModelReader;
import org.onehippo.cm.model.ConfigurationModel;
import org.onehippo.cm.model.impl.ConfigurationModelImpl;
import org.onehippo.cm.model.impl.GroupImpl;
import org.onehippo.cm.model.impl.ModuleImpl;
import org.onehippo.cm.model.impl.ProjectImpl;
import org.onehippo.repository.bootstrap.util.BootstrapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.repository.api.HippoNodeType.HIPPO_LOCK;
import static org.onehippo.cm.engine.Constants.HCM_BASELINE_PATH;
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

    private Session session;
    private ConfigurationLockManager lockManager;
    private ConfigurationBaselineService baselineService;
    private ConfigurationConfigService configService;
    private ConfigurationContentService contentService;
    private AutoExportServiceImpl autoExportService;

    private ConfigurationModelImpl runtimeConfigurationModel;

    public ConfigurationServiceImpl start(final Session session, final StartRepositoryServicesTask startRepositoryServicesTask)
            throws RepositoryException {
        this.session = session;
        lockManager = new ConfigurationLockManager(session);
        baselineService = new ConfigurationBaselineService(session, lockManager);
        configService = new ConfigurationConfigService();
        contentService = new ConfigurationContentService();

        log.info("ConfigurationService: start");
        ensureInitialized();
        lockManager.lock();
        try {
            final boolean fullConfigure = "full".equalsIgnoreCase(System.getProperty("repo.bootstrap", "false"));
            final boolean configure = fullConfigure || Boolean.getBoolean("repo.bootstrap");
            final boolean first = isNew();
            final boolean mustConfigure = first || configure;
            final boolean verify = Boolean.getBoolean("repo.bootstrap.verify");
            final boolean autoExportAllowed = Boolean.getBoolean(org.onehippo.cm.engine.autoexport.Constants.SYSTEM_ALLOWED_PROPERTY_NAME);

            ConfigurationModelImpl baselineModel = loadBaselineModel();
            ConfigurationModelImpl bootstrapModel = null;
            boolean success;
            if (mustConfigure) {
                log.info("ConfigurationService: start configuring {}", first ? "(first time)" : fullConfigure ? "(full)" : "");
                try {
                    log.info("ConfigurationService: load bootstrap model");
                    bootstrapModel = loadBootstrapModel();
                    final boolean startAutoExportService = configure && autoExportAllowed; // TODO: && hasSourceModules(baselineModel);
                    log.info("ConfigurationService: apply bootstrap config");
                    success = applyConfig(baselineModel, bootstrapModel, verify, fullConfigure, !first);
                    if (success) {
                        // TODO: applyConfig should (probably) update/replace baselineModel, which then can/must
                        //       serve as runtimeConfigurationModel
                        runtimeConfigurationModel = bootstrapModel;
                        log.info("ConfigurationService: store bootstrap config");
                        success = storeBaselineModel(bootstrapModel);
                    }
                    if (success) {
                        // TODO: probably also should pass in baselineModel (or runtimeConfigurationModel, if updated, see above)
                        //       applied changes then should also be applied to/updated in runtimeConfigurationModel
                        //       furthermore, ContentService should use BaselineService to do the updates, not directly to JCR
                        log.info("ConfigurationService: apply bootstrap content");
                        success = applyContent(bootstrapModel);
                    }
                    if (startAutoExportService) {
                        log.info("ConfigurationService: start autoexport service");
                        startAutoExportService();
                    }
                    log.info("ConfigurationService: start repository services");
                    startRepositoryServicesTask.execute();
                    if (success) {
                        log.info("ConfigurationService: start post-startup tasks");
                        postStartupTasks();
                    }
                } finally {
                    if (bootstrapModel != null) {
                        try {
                            bootstrapModel.close();
                        } catch (Exception e) {
                            log.error("Error closing bootstrap configuration", e);
                        }
                    }
                }
            }
            else {
                log.info("ConfigurationService: start repository services");
                startRepositoryServicesTask.execute();
            }
        } finally {
            try {
                lockManager.unlock();
            } catch (Exception e) {
                log.error("Failed to release the configuration lock", e);
            }
        }
        log.info("ConfigurationService: started");
        return this;
    }

    public void stop() {
        if (autoExportService != null) {
            autoExportService.close();
            autoExportService = null;
        }
        if (lockManager == null) {
            return;
        }
        log.info("ConfigurationService: stop");
        boolean locked = false;
        try {
            lockManager.lock();
            locked = true;
        } catch (Exception e) {
            log.error("Failed to claim the configuration lock", e);
        }
        try {
            if (runtimeConfigurationModel != null) {
                try {
                    // Ensure configurationModel resources are cleaned up (if any)
                    runtimeConfigurationModel.close();
                }
                catch (Exception e) {
                    log.error("Error closing runtime configuration", e);
                }
            }
            runtimeConfigurationModel = null;
        } finally {
            if (locked) {
                try {
                    lockManager.unlock();
                } catch (Exception e) {
                    log.error("Failed to release the configuration lock", e);
                }
            }
        }
        lockManager.stop();
        lockManager = null;
        contentService = null;
        configService = null;
        baselineService = null;
        session = null;
        log.info("ConfigurationService: stopped");
    }

    @Override
    public ConfigurationModel getRuntimeConfigurationModel() {
        return runtimeConfigurationModel;
    }

    private boolean isNew() throws RepositoryException {
        return !(session.getWorkspace().getNodeTypeManager().hasNodeType(NT_HCM_ROOT)
                && session.nodeExists(HCM_ROOT_PATH) && session.nodeExists(HCM_BASELINE_PATH));
    }

    private boolean isNamespaceRegistered(final String prefix) throws RepositoryException {
        try {
            session.getNamespaceURI(prefix);
            return true;
        } catch (NamespaceException e) {
            return false;
        }
    }

    private void ensureInitialized() throws RepositoryException {
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
        } catch (RepositoryException|RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private ConfigurationModelImpl loadBootstrapModel() throws RepositoryException {
        try {
            return new ClasspathConfigurationModelReader().read(Thread.currentThread().getContextClassLoader(), true);
        } catch (Exception e) {
            if (e instanceof RuntimeException) {
                throw (RuntimeException)e;
            }
            throw new RepositoryException(e);
        }
    }

    private boolean hasSourceModules(final ConfigurationModelImpl model) {
        for (GroupImpl g : model.getSortedGroups()) {
            for (ProjectImpl p : g.getProjects()) {
                for (ModuleImpl m : p.getModules()) {
                    if (m.getMvnPath() != null) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private ConfigurationModelImpl loadBaselineModel() throws RepositoryException {
        try {
            ConfigurationModelImpl model = baselineService.loadBaseline();
            if (model == null) {
                model = new ConfigurationModelImpl().build();
            }
            return model;
        } catch (Exception e) {
            if (e instanceof RuntimeException) {
                throw (RuntimeException)e;
            }
            if (e instanceof RepositoryException) {
                throw (RepositoryException)e;
            }
            throw new RepositoryException(e);
        }
    }

    private boolean applyConfig(final ConfigurationModel baseline, final ConfigurationModelImpl config,
                             final boolean verify, final boolean forceApply, final boolean mayFail)
            throws RepositoryException {
        try {
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();

            configService.computeAndWriteDelta(baseline, config, session, forceApply);
            if (verify) {
                configService.computeAndWriteDelta(baseline, config, session, forceApply);
            }
            session.save();

            stopWatch.stop();
            log.info("ConfigurationModel {}applied {}in {}",
                    forceApply ? "fully " : "",
                    verify ? "and verified " : "",
                    stopWatch.toString());
            return true;
        }
        catch (Exception e) {
            log.error("Failed to apply config", e);
            if (mayFail) {
                return false;
            }
            if (e instanceof ConfigurationRuntimeException) {
                throw (ConfigurationRuntimeException)e;
            }
            if (e instanceof RepositoryException) {
                throw (RepositoryException)e;
            }
            throw new RepositoryException(e);
        }
    }

    private boolean storeBaselineModel(final ConfigurationModelImpl model) {
        try {
            baselineService.storeBaseline(model);
            session.save();
            return true;
        } catch (Exception e) {
            log.error("Failed to store the Configuration baseline", e);
            return false;
        }
    }

    private boolean applyContent(final ConfigurationModelImpl model) {
        try {
            contentService.apply(model, session);
            return true;
        } catch (Exception e) {
            log.error("Failed to apply all content", e);
            return false;
        }
    }

    private void startAutoExportService() {
        try {
            autoExportService = new AutoExportServiceImpl(session, this);
        } catch (Exception e) {
            log.error("Faileed to start autoexport service");
        }
    }

    private void postStartupTasks() {
        try {
            // webfiles
            try {
                configService.writeWebfiles(runtimeConfigurationModel, session);
            } catch (IOException e) {
                log.error("Error initializing webfiles", e);
            }
        } catch (Exception e) {
            log.error("Failed to complete post-startup tasks", e);
        }
    }
}
