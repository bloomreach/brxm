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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;

import javax.jcr.NamespaceException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.onehippo.cm.ResourceInputProvider;
import org.onehippo.cm.engine.autoexport.AutoExportServiceImpl;
import org.onehippo.cm.model.ActionType;
import org.onehippo.cm.model.ClasspathConfigurationModelReader;
import org.onehippo.cm.model.ConfigurationModel;
import org.onehippo.cm.model.ContentDefinition;
import org.onehippo.cm.model.ExportModuleContext;
import org.onehippo.cm.model.ImportModuleContext;
import org.onehippo.cm.model.ModuleContext;
import org.onehippo.cm.model.ModuleWriter;
import org.onehippo.cm.model.PathConfigurationReader;
import org.onehippo.cm.model.Source;
import org.onehippo.cm.model.impl.ConfigurationModelImpl;
import org.onehippo.cm.model.impl.ContentDefinitionImpl;
import org.onehippo.cm.model.impl.GroupImpl;
import org.onehippo.cm.model.impl.ModuleImpl;
import org.onehippo.cm.model.impl.ProjectImpl;
import org.onehippo.cm.model.parser.ContentSourceParser;
import org.onehippo.cm.model.parser.ParserException;
import org.onehippo.cm.model.serializer.ContentSourceSerializer;
import org.onehippo.repository.bootstrap.util.BootstrapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.Files;

import static org.hippoecm.repository.api.HippoNodeType.HIPPO_LOCK;
import static org.onehippo.cm.engine.Constants.HCM_BASELINE_PATH;
import static org.onehippo.cm.engine.Constants.HCM_NAMESPACE;
import static org.onehippo.cm.engine.Constants.HCM_PREFIX;
import static org.onehippo.cm.engine.Constants.HCM_ROOT;
import static org.onehippo.cm.engine.Constants.HCM_ROOT_PATH;
import static org.onehippo.cm.engine.Constants.HIPPO_NAMESPACE;
import static org.onehippo.cm.engine.Constants.HIPPO_PREFIX;
import static org.onehippo.cm.engine.Constants.NT_HCM_ROOT;
import static org.onehippo.cm.engine.Constants.SYSTEM_PARAMETER_REPO_BOOTSTRAP;
import static org.onehippo.cm.engine.autoexport.Constants.SYSTEM_PROPERTY_AUTOEXPORT_ALLOWED;
import static org.onehippo.cm.model.Constants.HCM_CONFIG_FOLDER;
import static org.onehippo.cm.model.Constants.PROJECT_BASEDIR_PROPERTY;
import static org.onehippo.cm.model.impl.ConfigurationModelImpl.mergeWithSourceModules;

public class ConfigurationServiceImpl implements InternalConfigurationService {

    private static final Logger log = LoggerFactory.getLogger(ConfigurationServiceImpl.class);

    private Session session;
    private ConfigurationLockManager lockManager;
    private ConfigurationBaselineService baselineService;
    private ConfigurationConfigService configService;
    private ConfigurationContentService contentService;
    private AutoExportServiceImpl autoExportService;

    private ConfigurationModelImpl baselineModel;
    private ConfigurationModelImpl runtimeConfigurationModel;

    public ConfigurationServiceImpl start(final Session configurationServiceSession, final StartRepositoryServicesTask startRepositoryServicesTask)
            throws RepositoryException {
        session = configurationServiceSession;
        session.getWorkspace().getObservationManager().setUserData(Constants.HCM_ROOT);
        log.info("ConfigurationService: start");
        try {
            init(startRepositoryServicesTask);
            log.info("ConfigurationService: started");
        } catch (RepositoryException e) {
            log.error("Failed to start the ConfigurationService", e);
            stop();
            throw e;
        }
        return this;
    }

    private void init(final StartRepositoryServicesTask startRepositoryServicesTask) throws RepositoryException {
        lockManager = new ConfigurationLockManager(session);
        baselineService = new ConfigurationBaselineService(session, lockManager);
        configService = new ConfigurationConfigService();
        contentService = new ConfigurationContentService(baselineService, new JcrContentProcessor());

        ensureInitialized();
        lockManager.lock();
        try {
            final boolean first = isNew();
            final boolean fullConfigure =
                    first || "full".equalsIgnoreCase(System.getProperty(SYSTEM_PARAMETER_REPO_BOOTSTRAP, "false"));
            final boolean configure = fullConfigure || Boolean.getBoolean(SYSTEM_PARAMETER_REPO_BOOTSTRAP);
            final boolean mustConfigure = first || configure;
            final boolean verify = Boolean.getBoolean("repo.bootstrap.verify");
            final boolean isPojectBaseDirSet = !StringUtils.isBlank(System.getProperty(PROJECT_BASEDIR_PROPERTY));
            final boolean autoExportAllowed = isPojectBaseDirSet && Boolean.getBoolean(SYSTEM_PROPERTY_AUTOEXPORT_ALLOWED);

            baselineModel = loadBaselineModel();
            ConfigurationModelImpl bootstrapModel = null;
            boolean success;
            if (mustConfigure) {
                log.info("ConfigurationService: start configuring {}", first ? "(first time)" : fullConfigure ? "(full)" : "");
                try {
                    log.info("ConfigurationService: load bootstrap model");
                    bootstrapModel = loadBootstrapModel();
                    final boolean startAutoExportService = configure && autoExportAllowed; // TODO: && hasSourceModules(baselineModel);
                    log.info("ConfigurationService: apply bootstrap config");
                    success = applyConfig(baselineModel, bootstrapModel, false, verify, fullConfigure, !first);
                    if (success) {
                        // set the runtime model to bootstrap here just in case storing the baseline fails
                        runtimeConfigurationModel = bootstrapModel;
                        log.info("ConfigurationService: store bootstrap config");
                        success = storeBaselineModel(bootstrapModel);
                    }
                    if (success) {
                        log.info("ConfigurationService: apply bootstrap content");
                        // use bootstrap modules, because that's the only place content sources really exist
                        success = applyContent(bootstrapModel);
                    }
                    if (success) {
                        // reload the baseline after storing, so we have a JCR-backed view of our modules
                        // we want to avoid using bootstrap modules directly, because of awkward ZipFileSystems
                        baselineModel = loadBaselineModel();

                        // also, we prefer using source modules over baseline modules
                        runtimeConfigurationModel = mergeWithSourceModules(bootstrapModel, baselineModel);
                    }
                    log.info("ConfigurationService: start repository services");
                    startRepositoryServicesTask.execute();
                    if (success) {
                        log.info("ConfigurationService: start post-startup tasks");
                        // we need the bootstrap model here, not the baseline, so we can access the jar content
                        postStartupTasks(bootstrapModel);
                    }
                    if (startAutoExportService) {
                        log.info("ConfigurationService: start autoexport service");
                        startAutoExportService();
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
    }

    public void stop() {
        log.info("ConfigurationService: stop");
        if (autoExportService != null) {
            autoExportService.close();
            autoExportService = null;
        }
        boolean locked = false;
        if (lockManager != null) {
            try {
                lockManager.lock();
                locked = true;
            } catch (Exception e) {
                log.error("Failed to claim the configuration lock", e);
            }
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
        contentService = null;
        configService = null;
        baselineService = null;
        if (lockManager != null) {
            lockManager.stop();
            lockManager = null;
        }
        if (session != null) {
            if (session.isLive()) {
                session.logout();
            }
            session = null;
        }
        log.info("ConfigurationService: stopped");
    }

    @Override
    public ConfigurationModelImpl getRuntimeConfigurationModel() {
        return runtimeConfigurationModel;
    }

    @Override
    public boolean isAutoExportAvailable() {
        return autoExportService != null;
    }

    /** INTERNAL USAGE ONLY **/
    @Override
    public boolean verifyConfigurationModel() throws RepositoryException {
        lockManager.lock();
        try {
            log.info("ConfigurationService: verify config");
            return applyConfig(new ConfigurationModelImpl().build(), loadBootstrapModel(), true, false, true, false);
        } finally {
            lockManager.unlock();
        }
    }

    /**
     * Store the new baseline model as computed by auto-export, and make this the new runtimeConfigurationModel.
     * @param updatedModules modules that have been changed by auto-export and need to be stored in the baseline
     * @return
     */
    // TODO: confirm that this is the appropriate scope (public, but not exposed on interface)
    public boolean updateBaselineForAutoExport(final Collection<ModuleImpl> updatedModules) {
        try {
            baselineModel = baselineService.updateBaselineModules(updatedModules, baselineModel, session);
            runtimeConfigurationModel = mergeWithSourceModules(updatedModules, baselineModel);
            return true;
        }
        catch (Exception e) {
            log.error("Failed to update the Configuration baseline after auto-export", e);
            return false;
        }
    }

    public File exportZippedContent(final Node nodeToExport) throws RepositoryException, IOException {

        final ModuleImpl module = contentService.exportNode(nodeToExport);

        final File dirToZip = Files.createTempDir();

        final Path modulePath = Paths.get(dirToZip.getPath());

        final ModuleContext moduleContext = new ExportModuleContext(module, modulePath);
        try {
            new ModuleWriter().writeModule(module, moduleContext);
            File file = File.createTempFile("export", "zip");
            final ZipCompressor zipCompressor = new ZipCompressor();
            zipCompressor.zipDirectory(dirToZip.toPath(), file.getAbsolutePath());
            return file;
        }
        finally {
            FileUtils.deleteQuietly(dirToZip);
        }
    }

    public void importZippedContent(final File zipFile, final Node parentNode) throws RepositoryException, IOException {

        final FileSystem zipFileSystem = ZipCompressor.createZipFileSystem(zipFile.getAbsolutePath(), false);
        final Path zipRootPath = zipFileSystem.getPath("/");

        final ModuleImpl module = new ModuleImpl("import-module", new ProjectImpl("import-project", new GroupImpl("import-group")));
        final ModuleContext moduleContext = new ImportModuleContext(module, zipRootPath);
        try {
            new PathConfigurationReader().readModule(module, moduleContext, false);
            final ContentDefinitionImpl contentDefinition = (ContentDefinitionImpl)module.getContentSources().iterator().next().getDefinitions().get(0);
            contentService.importNode(contentDefinition.getNode(), parentNode, ActionType.RELOAD);
        } catch (ParserException e) {
            throw new RuntimeException("Import failed", e);
        }

    }

    public void importPlainYaml(final InputStream inputStream, final Node parentNode) throws RepositoryException {

        try {
            final ResourceInputProvider resourceInputProvider = new ResourceInputProvider() {
                @Override
                public boolean hasResource(final Source source, final String resourcePath) {
                    return false;
                }
                @Override
                public InputStream getResourceInputStream(final Source source, final String resourcePath) throws IOException {
                    throw new IOException("Plain YAML import does not support links to resources");
                }

            };

            final ModuleImpl module = new ModuleImpl("import-module", new ProjectImpl("import-project", new GroupImpl("import-group")));
            final ContentSourceParser sourceParser = new ContentSourceParser(resourceInputProvider);
            sourceParser.parse(inputStream, "/import", "console.yaml", module);
            final ContentDefinition contentDefinition = (ContentDefinition) module.getContentSources().iterator().next().getDefinitions().get(0);
            contentService.importNode(((ContentDefinitionImpl)contentDefinition).getNode(), parentNode, ActionType.RELOAD);
        } catch (Exception e) {
            throw new RuntimeException("Import failed", e);
        }
    }

    public String exportContent(final Node nodeToExport) throws RepositoryException, IOException {

        final ModuleImpl module = contentService.exportNode(nodeToExport);

        final ModuleContext moduleContext = new ExportModuleContext(module);
        final ContentSourceSerializer contentSourceSerializer = new ContentSourceSerializer(moduleContext, module.getContentSources().iterator().next(), false);

        final org.yaml.snakeyaml.nodes.Node node = contentSourceSerializer.representSource(postProcessItem -> {
        });
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        contentSourceSerializer.serializeNode(out, node);
        return new String(out.toByteArray());
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
            return new ClasspathConfigurationModelReader().read(Thread.currentThread().getContextClassLoader());
        } catch (Exception e) {
            if (e instanceof RuntimeException) {
                throw (RuntimeException)e;
            }
            throw new RepositoryException(e);
        }
    }

    private boolean hasSourceModules(final ConfigurationModelImpl model) {
        for (ModuleImpl m : model.getModules()) {
            if (m.getMvnPath() != null) {
                return true;
            }
        }
        return false;
    }

    private ConfigurationModelImpl loadBaselineModel() throws RepositoryException {
        try {
            ConfigurationModelImpl model = baselineService.loadBaseline(session);
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

    private boolean applyConfig(final ConfigurationModel baseline, final ConfigurationModelImpl config, final boolean verifyOnly,
                                final boolean verify, final boolean forceApply, final boolean mayFail)
            throws RepositoryException {
        try {
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();

            configService.computeAndWriteDelta(baseline, config, session, forceApply);
            if (verify) {
                configService.computeAndWriteDelta(baseline, config, session, forceApply);
            }
            if (!verifyOnly) {
                session.save();
            }

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
            baselineService.storeBaseline(model, session);
            // session.save() isn't necessary here, because storeBaseline() already does it
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

    private void postStartupTasks(final ConfigurationModel bootstrapModel) {
        try {
            // webfiles
            try {
                configService.writeWebfiles(bootstrapModel, session);
                session.save();
            } catch (IOException e) {
                log.error("Error initializing webfiles", e);
            }
        } catch (Exception e) {
            log.error("Failed to complete post-startup tasks", e);
        }
    }
}
