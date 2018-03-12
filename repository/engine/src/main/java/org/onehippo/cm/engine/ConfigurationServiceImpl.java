/*
 * Copyright 2017-2018 Hippo B.V. (http://www.onehippo.com)
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
import java.lang.annotation.Annotation;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.jcr.NamespaceException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.onehippo.cm.engine.autoexport.AutoExportConfig;
import org.onehippo.cm.engine.autoexport.AutoExportConstants;
import org.onehippo.cm.engine.autoexport.AutoExportServiceImpl;
import org.onehippo.cm.engine.migrator.ConfigurationMigrator;
import org.onehippo.cm.engine.migrator.MigrationException;
import org.onehippo.cm.engine.migrator.PostMigrator;
import org.onehippo.cm.engine.migrator.PreMigrator;
import org.onehippo.cm.model.ConfigurationModel;
import org.onehippo.cm.model.ConsoleExportModuleContext;
import org.onehippo.cm.model.ExportModuleContext;
import org.onehippo.cm.model.ImportModuleContext;
import org.onehippo.cm.model.definition.ActionType;
import org.onehippo.cm.model.impl.ConfigurationModelImpl;
import org.onehippo.cm.model.impl.GroupImpl;
import org.onehippo.cm.model.impl.ModuleImpl;
import org.onehippo.cm.model.impl.ProjectImpl;
import org.onehippo.cm.model.impl.definition.ContentDefinitionImpl;
import org.onehippo.cm.model.impl.tree.ConfigurationPropertyImpl;
import org.onehippo.cm.model.impl.tree.ValueImpl;
import org.onehippo.cm.model.parser.ClasspathConfigurationModelReader;
import org.onehippo.cm.model.parser.ContentSourceParser;
import org.onehippo.cm.model.parser.ParserException;
import org.onehippo.cm.model.parser.PathConfigurationReader;
import org.onehippo.cm.model.serializer.ContentSourceSerializer;
import org.onehippo.cm.model.serializer.ModuleContext;
import org.onehippo.cm.model.serializer.ModuleWriter;
import org.onehippo.cm.model.source.ResourceInputProvider;
import org.onehippo.cm.model.source.Source;
import org.onehippo.cm.model.util.ClasspathResourceAnnotationScanner;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.autoreload.AutoReloadService;
import org.onehippo.repository.util.NodeTypeUtils;
import org.onehippo.cms7.services.eventbus.HippoEventBus;
import org.onehippo.cms7.services.eventbus.Subscribe;
import org.onehippo.cms7.services.extension.ExtensionEvent;
import org.onehippo.cms7.services.extension.ExtensionRegistry;
import org.onehippo.repository.bootstrap.util.BootstrapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.Files;

import static org.hippoecm.repository.api.HippoNodeType.NT_DOCUMENT;
import static java.util.stream.Collectors.toList;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_LOCK;
import static org.onehippo.cm.engine.Constants.HCM_NAMESPACE;
import static org.onehippo.cm.engine.Constants.HCM_PREFIX;
import static org.onehippo.cm.engine.Constants.HCM_ROOT;
import static org.onehippo.cm.engine.Constants.HCM_ROOT_PATH;
import static org.onehippo.cm.engine.Constants.NT_HCM_ROOT;
import static org.onehippo.cm.engine.Constants.SYSTEM_PARAMETER_REPO_BOOTSTRAP;
import static org.onehippo.cm.engine.autoexport.AutoExportConstants.SYSTEM_PROPERTY_AUTOEXPORT_ALLOWED;
import static org.onehippo.cm.model.Constants.HCM_CONFIG_FOLDER;
import static org.onehippo.cm.model.Constants.PROJECT_BASEDIR_PROPERTY;
import static org.onehippo.cm.model.impl.ConfigurationModelImpl.mergeWithSourceModules;
import static org.onehippo.cm.model.util.FilePathUtils.nativePath;

public class ConfigurationServiceImpl implements InternalConfigurationService {

    private static final Logger log = LoggerFactory.getLogger(ConfigurationServiceImpl.class);

    private Session session;
    private ConfigurationLockManager lockManager;
    private ConfigurationBaselineService baselineService;
    private ConfigurationConfigService configService;
    private ConfigurationContentService contentService;
    private AutoExportServiceImpl autoExportService;

    /**
     * Note: this will typically be null, but will store a reference copy of the baseline when autoexport is allowed
     */
    private ConfigurationModelImpl baselineModel;

    /**
     * This should be non-null on any successful startup.
     */
    private ConfigurationModelImpl runtimeConfigurationModel;

    public ConfigurationServiceImpl start(final Session configurationServiceSession, final StartRepositoryServicesTask startRepositoryServicesTask)
            throws RepositoryException {

        session = configurationServiceSession;

        // set event userData to identify events coming from this HCM session
        log.debug("ConfigurationService: Setting ObservationManager userData to {} to skip change events from this session for auto-export", Constants.HCM_ROOT);
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

        HippoServiceRegistry.registerService(this, HippoEventBus.class);
        return this;
    }

    @Subscribe
    public void onNewSiteEvent(final ExtensionEvent event) throws ParserException, IOException, URISyntaxException, RepositoryException {
        final String extensionName = event.getExtensionName();
        log.info("New site extension detected: {}", extensionName);
        final ClasspathConfigurationModelReader modelReader = new ClasspathConfigurationModelReader();

        final Collection<ModuleImpl> modules = modelReader.collectExtensionModules(event.getClassLoader()).stream()
                .filter(m -> Objects.equals(extensionName, m.getExtension())).collect(Collectors.toList());

        modules.forEach(runtimeConfigurationModel::addModule);
        ConfigurationModelImpl newModel = runtimeConfigurationModel.build();

        final List<ModuleImpl> extensionModulesFromSourceFiles =
                readModulesFromSourceFiles(runtimeConfigurationModel).stream()
                        .filter(m -> extensionName.equals(m.getExtension())).collect(toList());
        if (CollectionUtils.isNotEmpty(extensionModulesFromSourceFiles)) {
            newModel = mergeWithSourceModules(extensionModulesFromSourceFiles, newModel);
        }
        applyConfig(baselineModel, newModel,false,false, false,false);
        applyContent(newModel);
        runtimeConfigurationModel = newModel;

        updateBaselineForAutoExport(modules);
    }

    private void init(final StartRepositoryServicesTask startRepositoryServicesTask) throws RepositoryException {
        lockManager = new ConfigurationLockManager();
        baselineService = new ConfigurationBaselineService(lockManager);
        configService = new ConfigurationConfigService();
        contentService = new ConfigurationContentService(baselineService, new JcrContentProcessor());

        // acquire a write lock for the hcm
        lockManager.lock();
        try {
            // Ensure/force cluster synchronization in case another instance just initialized before, which changes
            // then may not yet have been synchronized automatically!
            session.refresh(true);
            // create the /hcm:hcm node, if necessary
            ensureInitialized();

            // attempt to load a baseline, which may be empty -- we will need this if (mustConfigure == false)
            ConfigurationModelImpl baselineModel = loadBaselineModel();

            // check the appropriate params to determine our state and bootstrap mode
            // empty baseline means we've never applied the v12+ bootstrap model before, since we should have at
            // least the hippo-cms group defined
            final boolean first = baselineModel.getSortedGroups().isEmpty();
            final boolean fullConfigure =
                    first || "full".equalsIgnoreCase(System.getProperty(SYSTEM_PARAMETER_REPO_BOOTSTRAP, "false"));
            final boolean configure = fullConfigure || Boolean.getBoolean(SYSTEM_PARAMETER_REPO_BOOTSTRAP);
            final boolean mustConfigure = first || configure;
            final boolean verify = Boolean.getBoolean("repo.bootstrap.verify");

            // also, check params for auto-export state
            final boolean isProjectBaseDirSet = StringUtils.isNotBlank(System.getProperty(PROJECT_BASEDIR_PROPERTY));
            boolean startAutoExportService = configure && isProjectBaseDirSet && Boolean.getBoolean(SYSTEM_PROPERTY_AUTOEXPORT_ALLOWED);
            ConfigurationModelImpl bootstrapModel = null;
            boolean success;
            if (mustConfigure) {
                log.info("ConfigurationService: start configuring {}", first ? "(first time)" : fullConfigure ? "(full)" : "");
                try {
                    log.info("ConfigurationService: load bootstrap model");
                    bootstrapModel = loadBootstrapModel();

                    // now that we have the deployment-based bootstrap model, we want to find out if the auto-export
                    // config indicates to us that we should load some modules from the filesystem
                    if (startAutoExportService) {
                        try {
                            // load modules that are specified via auto-export config
                            final List<ModuleImpl> modulesFromSourceFiles = readModulesFromSourceFiles(bootstrapModel);
                            // add all of the filesystem modules to a new model as "replacements" that override later additions

                            final List<String> knownExtensions = ExtensionRegistry.getContexts().values().stream().map(ExtensionEvent::getExtensionName).collect(toList());

                            final List<ModuleImpl> eligibleModules = modulesFromSourceFiles.stream()
                                    .filter(m -> m.getExtension() == null || knownExtensions.contains(m.getExtension())).collect(toList());
                            bootstrapModel = mergeWithSourceModules(eligibleModules, bootstrapModel);
                        } catch (Exception e) {
                            final String errorMsg = "Failed to load modules from filesystem for autoexport: autoexport not available.";
                            if (e instanceof ConfigurationRuntimeException) {
                                // no stacktrace needed, the exception message should be informative enough
                                log.error(errorMsg + "\n" + e.getMessage());
                            } else {
                                log.error(errorMsg, e);
                            }
                            startAutoExportService = false;
                            log.error("autoexport service disallowed");
                        }
                    }
                    else {
                        // if starting auto-export was disallowed to begin with, notify devs via the log
                        log.info("Running autoexport service not allowed (requires appropriate system parameters to be set first)");
                    }

                    log.info("Loading preMigrators");
                    final List<ConfigurationMigrator> preMigrators = loadMigrators(PreMigrator.class);
                    if (!preMigrators.isEmpty()) {
                        log.info("Running preMigrators: {}", preMigrators);
                        runMigrators(bootstrapModel, preMigrators, false);
                    }

                    log.info("ConfigurationService: apply bootstrap config");
                    success = applyConfig(baselineModel, bootstrapModel, false, verify, fullConfigure, !first);

                    if (success) {
                        // set runtimeConfigurationModel from bootstrapModel -- this is a reasonable default in case of exception
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

                        // if we're in a mode that allows auto-export, keep a copy of the baseline for future use
                        if (startAutoExportService) {
                            this.baselineModel = baselineModel;
                        }

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

                    boolean autoExportRunning = false;
                    if (startAutoExportService) {
                        log.info("ConfigurationService: start autoexport service");
                        autoExportRunning = startAutoExportService();
                    }

                    // post migrators need to run after the auto export service has been started because the
                    // changes of the migrators might have to be exported
                    log.info("Loading postMigrators");
                    final List<ConfigurationMigrator> postMigrators = loadMigrators(PostMigrator.class);
                    if (!postMigrators.isEmpty()) {
                        try {
                            if (session.hasPendingChanges()) {
                                throw new IllegalStateException("Pending changes at this moment not allowed");
                            }
                            log.debug("ConfigurationService: Resetting ObservationManager userData before running postMigrators to enable auto-export of their changes (if any).");
                            session.getWorkspace().getObservationManager().setUserData(null);
                            log.info("ConfigurationService: Running postMigrators: {}", postMigrators);
                            runMigrators(bootstrapModel, postMigrators, autoExportRunning);
                        } finally {
                            log.debug("ConfigurationService: Setting ObservationManager userData again to {} to skip further change events from this session for auto-export", Constants.HCM_ROOT);
                            session.getWorkspace().getObservationManager().setUserData(Constants.HCM_ROOT);
                        }
                    }

                } finally {
                    if (bootstrapModel != null) {
                        try {
                            // we need to close the bootstrap model because it's backed by ZipFileSystem(s)
                            bootstrapModel.close();
                        } catch (Exception e) {
                            log.error("Error closing bootstrap configuration", e);
                        }
                    }
                }
            } else {
                // if we're not doing any bootstrap, use the baseline model as our runtime model
                runtimeConfigurationModel = baselineModel;

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
        if (runtimeConfigurationModel != null) {
            try {
                // Ensure configurationModel resources are cleaned up (if any)
                runtimeConfigurationModel.close();
            } catch (Exception e) {
                log.error("Error closing runtime configuration", e);
            }
        }
        runtimeConfigurationModel = null;
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

    /**
     * INTERNAL USAGE ONLY
     **/
    @Override
    public boolean verifyConfigurationModel() throws RepositoryException {
        lockManager.lock();
        try {
            log.info("ConfigurationService: verify config");
            // Ensure/force cluster synchronization in case another instance just modified the baseline
            session.refresh(true);
            return applyConfig(new ConfigurationModelImpl().build(), loadBootstrapModel(), true, false, true, false);
        } finally {
            lockManager.unlock();
        }
    }

    @Override
    public void runSingleAutoExportCycle() throws RepositoryException {
        autoExportService.runOnce();
    }

    /**
     * Store the new baseline model as computed by auto-export, and make this the new runtimeConfigurationModel.
     *
     * @param updatedModules modules that have been changed by auto-export and need to be stored in the baseline
     * @return true if and only if the baseline update was stored successfully
     */
    public boolean updateBaselineForAutoExport(final Collection<ModuleImpl> updatedModules) {
        try {
            if (baselineModel == null) {
                baselineModel = loadBaselineModel();
            }

            baselineModel = baselineService.updateBaselineModules(updatedModules, baselineModel, session);
            runtimeConfigurationModel = mergeWithSourceModules(updatedModules, baselineModel);
            return true;
        } catch (Exception e) {
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
            File file = File.createTempFile("export", ".zip");
            final ZipCompressor zipCompressor = new ZipCompressor();
            zipCompressor.zipDirectory(dirToZip.toPath(), file.getAbsolutePath());
            return file;
        } finally {
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

            // todo: check for missing content source
            final ContentDefinitionImpl contentDefinition = module.getContentSources().iterator().next().getContentDefinition();
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
            final ContentDefinitionImpl contentDefinition = module.getContentSources().iterator().next().getContentDefinition();
            contentService.importNode(contentDefinition.getNode(), parentNode, ActionType.RELOAD);
        } catch (Exception e) {
            throw new RuntimeException("Import failed", e);
        }
    }

    public String exportContent(final Node nodeToExport) throws RepositoryException, IOException {

        final ModuleImpl module = contentService.exportNode(nodeToExport);

        final ModuleContext moduleContext = new ConsoleExportModuleContext(module);
        final ContentSourceSerializer contentSourceSerializer = new ContentSourceSerializer(moduleContext, module.getContentSources().iterator().next(), false);

        final org.yaml.snakeyaml.nodes.Node node = contentSourceSerializer.representSource();
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        contentSourceSerializer.serializeNode(out, node);
        return new String(out.toByteArray());
    }

    /**
     * if no /hcm:hcm node exists, we can assume this workspace hasn't run a v12+ HCM-style bootstrap yet
     */
    private boolean isFirstHcmStartup() throws RepositoryException {
        return !(session.getWorkspace().getNodeTypeManager().hasNodeType(NT_HCM_ROOT) && session.nodeExists(HCM_ROOT_PATH));
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
            if (isFirstHcmStartup()) {
                if (!isNamespaceRegistered(HCM_PREFIX)) {
                    session.getWorkspace().getNamespaceRegistry().registerNamespace(HCM_PREFIX, HCM_NAMESPACE);
                }
                if (!session.getWorkspace().getNodeTypeManager().hasNodeType(NT_DOCUMENT)) {
                    try (InputStream is = getClass().getResourceAsStream("/" + HCM_CONFIG_FOLDER + "/hippo.cnd")) {
                        NodeTypeUtils.initializeNodeTypes(session, is, "hippo.cnd");
                    }
                }
                if (!session.getWorkspace().getNodeTypeManager().hasNodeType(NT_HCM_ROOT)) {
                    try (InputStream is = getClass().getResourceAsStream("/" + HCM_CONFIG_FOLDER + "/hcm.cnd")) {
                        NodeTypeUtils.initializeNodeTypes(session, is, "hcm.cnd");
                    }
                }
                session.getRootNode().addNode(HCM_ROOT, NT_HCM_ROOT);
                session.save();
            }
        } catch (RepositoryException | RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private ConfigurationModelImpl loadBootstrapModel() throws RepositoryException {
        try {
            final ClasspathConfigurationModelReader modelReader = new ClasspathConfigurationModelReader();
            ConfigurationModelImpl model = modelReader.read(Thread.currentThread().getContextClassLoader());
            final Map<String, ExtensionEvent> contexts = ExtensionRegistry.getContexts();
            for (ExtensionEvent event : contexts.values()) {
                model = modelReader.readExtension(event.getClassLoader(), model);
            }
            return model;
        } catch (Exception e) {
            if (e instanceof RuntimeException) {
                throw (RuntimeException)e;
            }
            throw new RepositoryException(e);
        }
    }

    /**
     * Read modules that were specified using the auto-export config as source files on the native filesystem.
     *
     * @return a List of newly-loaded filesystem-backed Modules
     */
    private List<ModuleImpl> readModulesFromSourceFiles(final ConfigurationModelImpl bootstrapModel) throws IOException, ParserException {
        final String projectDir = System.getProperty(org.onehippo.cm.model.Constants.PROJECT_BASEDIR_PROPERTY);

        // if project.basedir is defined, and auto-export config mentions a module, load it from the filesystem
        final ConfigurationPropertyImpl autoExportModulesProp =
                bootstrapModel.resolveProperty(AutoExportConstants.SERVICE_CONFIG_PATH
                        + "/" + AutoExportConstants.CONFIG_MODULES_PROPERTY_NAME);
        final LinkedHashMap<String, Collection<String>> modulesConfig = new LinkedHashMap<>();
        if (autoExportModulesProp != null) {
            final ArrayList<String> moduleStrings = new ArrayList<>();
            for (ValueImpl value : autoExportModulesProp.getValues()) {
                moduleStrings.add(value.getString());
            }
            // reuse the auto-export logic to tweak the defined config as necessary
            AutoExportConfig.processModuleStrings(moduleStrings, modulesConfig, false);
        }

        // convert the project basedir to a Path, so we can resolve modules against it
        final Path projectPath = Paths.get(projectDir);

        // for each module in autoexport:modules
        final List<ModuleImpl> modulesFromSourceFiles = new ArrayList<>();
        for (String mvnModulePath : modulesConfig.keySet()) {
            // first check module path exists:
            final Path modulePath = projectPath.resolve(nativePath(mvnModulePath));
            final File moduleDir = modulePath.toFile();
            if (!moduleDir.exists() || !moduleDir.isDirectory()) {
                throw new ConfigurationRuntimeException("Cannot find module source path for module: '" + mvnModulePath + "' in "
                        + AutoExportConstants.CONFIG_MODULES_PROPERTY_NAME + ", expected directory: " + modulePath);
            }
            // use maven conventions to find a module descriptor, then parse it
            final Path moduleDescriptorPath = projectPath.resolve(nativePath(mvnModulePath + org.onehippo.cm.engine.Constants.MAVEN_MODULE_DESCRIPTOR));

            if (!moduleDescriptorPath.toFile().exists()) {
                throw new ConfigurationRuntimeException("Cannot find module descriptor for module: '" + mvnModulePath + "' in "
                        + AutoExportConstants.CONFIG_MODULES_PROPERTY_NAME + ", expected: " + moduleDescriptorPath);
            }

            log.debug("Loading module descriptor from filesystem here: {}", moduleDescriptorPath);

            final PathConfigurationReader.ReadResult result =
                    new PathConfigurationReader().read(moduleDescriptorPath, true);

            // store mvnSourcePath on each module for later use by auto-export
            final ModuleImpl module = result.getModuleContext().getModule();
            module.setMvnPath(mvnModulePath);
            modulesFromSourceFiles.add(module);
        }
        return modulesFromSourceFiles;
    }

    /**
     * @return a valid baseline, if one exists, or an empty ConfigurationModel
     * @throws RepositoryException only if an unexpected repository problem occurs (not if the baseline is missing)
     */
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
        } catch (Exception e) {
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

    /**
     * Run all migrators. The session is expected to be clean (no pending changes) when this method is called
     * and after it returns. Failure of one migrator is not expected to prevent any other from running unless the
     * migrator throws a {@link MigrationException} which causes the repository failing to start up.
     * @return {@code true} if all migrators ran without Exception. If one migrator
     * fails during their {@link ConfigurationMigrator#migrate(Session, ConfigurationModel, boolean)}, {@code false} is
     * returned.
     */
    private void runMigrators(final ConfigurationModelImpl model, final List<ConfigurationMigrator> migrators,
                              final boolean autoExportRunning) {
        for (ConfigurationMigrator migrator : migrators) {
            try {
                migrator.migrate(session, model, autoExportRunning);
            } catch (MigrationException e) {
                log.error("Short-circuiting repository startup due to MigrationException in " +
                        "migrator {}", migrator);
                throw e;
            } catch (Exception e) {
                log.error("Failed to migrate '{}'", migrator , e);
            }
            finally {
                // clean up any changes that haven't been saved by the migrator
                try {
                    if (session.hasPendingChanges()) {
                        log.warn("Migrator {} had unsaved changes which will be discarded now.", migrator);
                        session.refresh(false);
                    }
                }
                catch (RepositoryException e2) {
                    log.error("Exception attempting to refresh session after failed migration", e2);
                }
            }
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
        } finally {
            try {
                // make sure to flush remaining pending changes as we need to assure a 'clean' session state at the end
                if (session.hasPendingChanges()) {
                    session.refresh(false);
                }
            } catch (RepositoryException ignore) {
            }
        }
    }

    private boolean startAutoExportService() {
        try {
            autoExportService = new AutoExportServiceImpl(session, this);
            return autoExportService.isRunning();
        } catch (Exception e) {
            log.error("ConfigurationService: Failed to start autoexport service");
            return false;
        }
    }

    private void postStartupTasks(final ConfigurationModel bootstrapModel) {
        try {
            // webfiles
            try {
                configService.writeWebfiles(bootstrapModel, baselineService, session);
                session.save();
            } catch (IOException e) {
                log.error("Error initializing webfiles", e);
            }
        } catch (Exception e) {
            log.error("Failed to complete post-startup tasks", e);
        }
    }

    /**
     * <p>
     *  Load all migrators by classpath scanning. Of course, on purpose, this will only scan the webapp in which the
     *  repository lives. Since we do not want to provide a generic pattern for third-party / end projects to kick in,
     *  we only load {@link ConfigurationMigrator}s that are below one of these packages:
     *  <ul>
     *      <li>org/hippoecm</li>
     *      <li>org/onehippo</li>
     *      <li>com/onehippo</li>
     *  </ul>
     *  If at some point we want to make the mechanism more generally available, we can relax the classpath scanning.
     * </p>
     *
     * @param annotationClazz
     */
    private List<ConfigurationMigrator> loadMigrators(final Class<? extends Annotation> annotationClazz) {
        Set<String> migratorClassNames = new ClasspathResourceAnnotationScanner().scanClassNamesAnnotatedBy(annotationClazz,
                "classpath*:org/hippoecm/**/*.class",
                "classpath*:org/onehippo/**/*.class",
                "classpath*:com/onehippo/**/*.class");

        List<ConfigurationMigrator> migrators = new ArrayList<>();
        for (String migratorClassName : migratorClassNames) {
            try {
                Class<?> migratorClass = Class.forName(migratorClassName);

                Object object = migratorClass.newInstance();

                if (object instanceof ConfigurationMigrator) {
                    ConfigurationMigrator migrator = (ConfigurationMigrator)object;
                    log.info("Adding migrator '{}'", migrator);
                    migrators.add(migrator);
                } else {
                    log.error("Skipping incorrect annotated class '{}' as migrator. Only subclasses of '{}' are allowed to " +
                            "be annotation with '{}'.", migratorClassName, ConfigurationMigrator.class.getName(), annotationClazz.getName());
                }
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
                log.error("Could not instantiate migrator '{}'. Migrator will not run.", migratorClassName, e);
            }
        }
        return migrators;
    }
}
