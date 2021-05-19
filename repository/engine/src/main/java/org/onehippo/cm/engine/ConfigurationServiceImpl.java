/*
 * Copyright 2017-2021 Hippo B.V. (http://www.onehippo.com)
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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.jcr.NamespaceException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletContext;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.lang3.tuple.Pair;
import org.onehippo.cm.engine.autoexport.AutoExportConfig;
import org.onehippo.cm.engine.autoexport.AutoExportConstants;
import org.onehippo.cm.engine.autoexport.AutoExportServiceImpl;
import org.onehippo.cm.engine.migrator.ConfigurationMigrator;
import org.onehippo.cm.engine.migrator.ConfigurationSiteMigrator;
import org.onehippo.cm.engine.migrator.MigrationException;
import org.onehippo.cm.engine.migrator.PostMigrator;
import org.onehippo.cm.engine.migrator.PostSiteMigrator;
import org.onehippo.cm.engine.migrator.PreMigrator;
import org.onehippo.cm.engine.migrator.PreSiteMigrator;
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
import org.onehippo.cm.model.impl.definition.WebFileBundleDefinitionImpl;
import org.onehippo.cm.model.impl.tree.ConfigurationNodeImpl;
import org.onehippo.cm.model.impl.tree.ConfigurationPropertyImpl;
import org.onehippo.cm.model.impl.tree.ValueImpl;
import org.onehippo.cm.model.parser.ClasspathConfigurationModelReader;
import org.onehippo.cm.model.parser.ContentSourceParser;
import org.onehippo.cm.model.parser.ModuleReader;
import org.onehippo.cm.model.parser.ParserException;
import org.onehippo.cm.model.path.JcrPath;
import org.onehippo.cm.model.path.JcrPaths;
import org.onehippo.cm.model.serializer.ContentSourceSerializer;
import org.onehippo.cm.model.serializer.ModuleContext;
import org.onehippo.cm.model.serializer.ModuleWriter;
import org.onehippo.cm.model.source.ResourceInputProvider;
import org.onehippo.cm.model.source.Source;
import org.onehippo.cm.model.util.ClasspathResourceAnnotationScanner;
import org.onehippo.cms7.services.ServiceHolder;
import org.onehippo.cms7.services.ServiceTracker;
import org.onehippo.cms7.services.context.HippoWebappContext;
import org.onehippo.cms7.services.context.HippoWebappContextRegistry;
import org.onehippo.repository.util.NodeTypeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import com.google.common.io.Files;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.hippoecm.repository.api.HippoNodeType.NT_DOCUMENT;
import static org.onehippo.cm.engine.Constants.HCM_NAMESPACE;
import static org.onehippo.cm.engine.Constants.HCM_PREFIX;
import static org.onehippo.cm.engine.Constants.HCM_ROOT;
import static org.onehippo.cm.engine.Constants.HCM_ROOT_PATH;
import static org.onehippo.cm.engine.Constants.HCM_SITE_DESCRIPTOR;
import static org.onehippo.cm.engine.Constants.HCM_SITE_DESCRIPTOR_LOCATION;
import static org.onehippo.cm.engine.Constants.NT_HCM_ROOT;
import static org.onehippo.cm.engine.Constants.PROJECT_BASEDIR_PROPERTY;
import static org.onehippo.cm.engine.Constants.SYSTEM_PARAMETER_REPO_BOOTSTRAP;
import static org.onehippo.cm.engine.Constants.SYSTEM_PARAMETER_USE_HCM_SITES;
import static org.onehippo.cm.engine.autoexport.AutoExportConstants.SYSTEM_PROPERTY_AUTOEXPORT_ALLOWED;
import static org.onehippo.cm.model.Constants.HCM_CONFIG_FOLDER;
import static org.onehippo.cm.model.impl.ConfigurationModelImpl.mergeWithSourceModules;
import static org.onehippo.cm.model.util.FilePathUtils.nativePath;

public class ConfigurationServiceImpl implements InternalConfigurationService, ServiceTracker<HippoWebappContext> {

    private static final Logger log = LoggerFactory.getLogger(ConfigurationServiceImpl.class);

    private static class SiteRecord {
        final String siteName;
        final JcrPath hstRoot;
        final ServletContext servletContext;

        public SiteRecord(final String siteName, final JcrPath hstRoot, final ServletContext servletContext) {
            this.siteName = siteName;
            this.hstRoot = hstRoot;
            this.servletContext = servletContext;
        }
    }

    static final boolean USE_HCM_SITES_MODE = checkHcmSitesMode();

    /**
     * Load the value of the "use.hcm.sites" property from an "hcm.properties" file in the root resource path of the
     * current classloader for this class.
     * @return default true, or the value of the "use.hcm.sites" property
     */
    public static boolean checkHcmSitesMode() {

        try (final InputStream propsStream = ConfigurationServiceImpl.class.getResourceAsStream("/hcm.properties")) {
            if (propsStream == null) {
                log.info("No hcm.properties file found for platform. Checking for system property.");
                final boolean multiSiteMode = Boolean.parseBoolean(System.getProperty(SYSTEM_PARAMETER_USE_HCM_SITES, "true"));
                log.info("Running in HCM {}-site mode.", multiSiteMode ? "multi" : "single");
                return multiSiteMode;
            }

            final Properties hcmProperties = new Properties();
            hcmProperties.load(propsStream);
            final boolean multiSiteMode = Boolean.parseBoolean(hcmProperties.getProperty(SYSTEM_PARAMETER_USE_HCM_SITES, "true"));
            log.info("hcm.properties file found for platform. Running in HCM {}-site mode.", multiSiteMode ? "multi" : "single");
            return multiSiteMode;

        } catch (IOException e) {
            log.warn("Error reading hcm.properties file for platform. Running in HCM multi-site mode.");
            return true;
        }
    }

    private Session session;
    private ConfigurationLockManager lockManager;
    private ConfigurationBaselineService baselineService;
    private ConfigurationConfigService configService;
    private ConfigurationContentService contentService;
    private AutoExportServiceImpl autoExportService;
    private Map<String, SiteRecord> hcmSiteRecords = new ConcurrentHashMap<>();
    private boolean startAutoExportService;

    private boolean verify;
    private boolean fullConfigure;
    private boolean first;
    private boolean mustConfigure;


    /**
     * Note: this will typically be null, but will store a reference copy of the baseline when autoexport is allowed
     */
    private ConfigurationModelImpl baselineModel;

    /**
     * This should be non-null on any successful startup.
     */
    private ConfigurationModelImpl runtimeConfigurationModel;

    /**
     * Wrap the HCM configuration service startup with error handling and logging, and mark the JCR session used for
     * bootstrap so that events can be filtered out from auto-export.
     * @param configurationServiceSession the JCR session to use for bootstrap
     * @param startRepositoryServicesTask a command-pattern task for follow-up work that must be triggered after bootstrap
     * @return a fully-initialized ConfigurationService
     * @throws RepositoryException
     */
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

        return this;
    }

    /**
     * Perform HCM model loading and bootstrap when a new HST site is added to the service registry.
     * @param record a param-holder object with the context data relevant for a newly-registered HST site
     * @throws ParserException
     * @throws IOException
     * @throws URISyntaxException
     * @throws RepositoryException
     */
    private void applySiteConfig(final SiteRecord record) throws ParserException, IOException, URISyntaxException, RepositoryException {

        final String siteName = record.siteName;
        if (!mustConfigure) {
            log.debug("skip applySiteConfig because bootstrap is disabled, site: {}", siteName);
            return;
        }

        log.info("New HCM site detected: {}", siteName);

        // Load the site HCM modules from the classpath and append to the runtimeConfigurationModel
        final ClasspathConfigurationModelReader modelReader = new ClasspathConfigurationModelReader();
        // This variable may or may not contain a new model instance, so logic below should not assume, but code defensively
        ConfigurationModelImpl newRuntimeConfigModel = modelReader.readSite(siteName, record.hstRoot,
                record.servletContext.getClassLoader(), runtimeConfigurationModel);

        // If auto-export is enabled and will be started, we also need to load modules from the local project
        if (startAutoExportService) {
            log.debug("loading source modules for auto-export during site bootstrap");
            final List<ModuleImpl> hcmSiteModulesFromSourceFiles = readModulesFromSourceFiles(runtimeConfigurationModel)
                    .stream().filter(m -> siteName.equals(m.getSiteName())).collect(toList());
            if (CollectionUtils.isNotEmpty(hcmSiteModulesFromSourceFiles)) {
                // This is where the runtimeConfigurationModel could be replaced with a new instance
                log.debug("merging source modules into runtime model for auto-export during site bootstrap");
                newRuntimeConfigModel = mergeWithSourceModules(hcmSiteModulesFromSourceFiles, newRuntimeConfigModel);
            }
        }

        // Reload baseline for bootstrap 2+
        final Set<String> siteNames = newRuntimeConfigModel.getSiteNames();
        log.debug("loading existing baseline during site bootstrap for sites: {}", siteNames);
        final ConfigurationModelImpl newBaselineModel = loadBaselineModel(siteNames);

        if (shouldSkipConfigBecauseOfDigestMatch(newRuntimeConfigModel, newBaselineModel)) {
            log.info("ConfigurationService: skipping site bootstrap because of matching bootstrap and baseline models: {}", siteName);
            // Note: site continues using jar-backed model for runtime use
            runtimeConfigurationModel = newRuntimeConfigModel;

            //process webfilebundle instructions from current HCM Site
            applyWebfiles(runtimeConfigurationModel, siteName);
        }
        else {
            // Run pre site migrators for this site
            log.debug("applying site pre-migrators for site: {}", siteName);
            applyPreMigrators(newRuntimeConfigModel, singleton(record), ConfigurationSiteMigrator.class);

            // apply config, but skip applying namespaces, since they are not allowed in sites
            log.debug("applying model config for sites: {}", siteNames);
            boolean success = applyConfig(newBaselineModel, newRuntimeConfigModel, false, verify, fullConfigure, !first, false);
            if (success) {
                log.debug("processing webfiles for site: {}", siteName);
                //process webfilebundle instructions from current HCM Site
                applyWebfiles(runtimeConfigurationModel, siteName);

                log.debug("applying model content for sites: {}", siteNames);
                success = applyContent(newRuntimeConfigModel);
            }

            if (success) {
                runtimeConfigurationModel = newRuntimeConfigModel;

                //store only HCM Site modules
                baselineService.storeSite(siteName, newRuntimeConfigModel, session);
                if (startAutoExportService) {
                    log.debug("reloading stored baseline during site init for site: {}, sites: {}", siteName, siteNames);
                    this.baselineModel = loadBaselineModel(siteNames);
                }

                // Run post site migrators for this site
                log.debug("applying site post-migrators for site: {}", siteName);
                applyPostMigrators(newRuntimeConfigModel, singleton(record),
                        (autoExportService != null && autoExportService.isRunning()),
                        ConfigurationSiteMigrator.class);

                log.info("HCM Site Configuration '{}' was successfuly applied", siteName);
            } else {
                log.error("HCM Site '{}' failed to be applied", siteName);
            }
        }
    }

    private List<WebFileBundleDefinitionImpl> getWebFileBundleDefsForSite(final ConfigurationModelImpl model, final String siteName) {
        return model.getModulesStream()
                .filter(m -> siteName.equals(m.getSiteName()))
                .flatMap(m -> m.getWebFileBundleDefinitions().stream()).collect(toList());
    }

    private void init(final StartRepositoryServicesTask startRepositoryServicesTask) throws RepositoryException {
        lockManager = new ConfigurationLockManager();
        baselineService = new ConfigurationBaselineService(lockManager);
        configService = new ConfigurationConfigService();
        contentService = new ConfigurationContentService(baselineService, new JcrContentProcessor());

        final Set<String> knownHcmSites = hcmSiteRecords.keySet();

        // acquire a write lock for the hcm
        lockManager.lock();
        try {

            HippoWebappContextRegistry.get().addTracker(this);

            log.debug("known sites at init: {}", knownHcmSites);
            // Ensure/force cluster synchronization in case another instance just initialized before, which changes
            // then may not yet have been synchronized automatically!
            session.refresh(true);
            // create the /hcm:hcm node, if necessary
            ensureInitialized();

            // attempt to load a baseline, which may be empty -- we will need this if (mustConfigure == false)
            log.debug("loading existing baseline during init with sites: {}", knownHcmSites);
            ConfigurationModelImpl baselineModel = loadBaselineModel(knownHcmSites);

            // check the appropriate params to determine our state and bootstrap mode
            // empty baseline means we've never applied the v12+ bootstrap model before, since we should have at
            // least the hippo-cms group defined
            first = baselineModel.getSortedGroups().isEmpty();
            fullConfigure =
                    first || "full".equalsIgnoreCase(System.getProperty(SYSTEM_PARAMETER_REPO_BOOTSTRAP, "false"));
            final boolean configure = fullConfigure || Boolean.getBoolean(SYSTEM_PARAMETER_REPO_BOOTSTRAP);
            mustConfigure = first || configure;
            verify = Boolean.getBoolean("repo.bootstrap.verify");

            // also, check params for auto-export state
            final boolean isProjectBaseDirSet = StringUtils.isNotBlank(System.getProperty(PROJECT_BASEDIR_PROPERTY));
            startAutoExportService = configure && isProjectBaseDirSet && Boolean.getBoolean(SYSTEM_PROPERTY_AUTOEXPORT_ALLOWED);

            ConfigurationModelImpl bootstrapModel = null;

            if (!mustConfigure) {
                initWithoutBootstrap(startRepositoryServicesTask, baselineModel);
            }
            else {
                log.info("ConfigurationService: start configuring {}", first ? "(first time)" : fullConfigure ? "(full)" : "");
                try {
                    log.info("ConfigurationService: load bootstrap model");
                    bootstrapModel = loadBootstrapModel();

                    // check the digest before doing real bootstrap work
                    if (shouldSkipConfigBecauseOfDigestMatch(bootstrapModel, baselineModel)) {
                        log.info("ConfigurationService: skipping config and content bootstrap because of matching bootstrap and baseline models");
                        initWithoutBootstrap(startRepositoryServicesTask, baselineModel);

                        applyWebfiles(bootstrapModel, null);
                    }
                    else {
                        //In case bootstrapping fails, we make sure there is a non-null runtimeConfigurationModel
                        //We exclude the cases where repository is bootstrapped first time or when repo.bootstrap=full (cause the baseline model is ignored then)
                        if (!fullConfigure) {
                            runtimeConfigurationModel = baselineModel;
                        }

                        // now that we have the deployment-based bootstrap model, we want to find out if the auto-export
                        // config indicates to us that we should load some modules from the filesystem
                        if (startAutoExportService) {
                            try {
                                // load modules that are specified via auto-export config
                                log.debug("loading source modules for auto-export");
                                final List<ModuleImpl> modulesFromSourceFiles = readModulesFromSourceFiles(bootstrapModel);

                                final List<ModuleImpl> bootstrapModules = bootstrapModel.getModulesStream().collect(toList());

                                //Collect only modules which exist at boostrap model
                                final List<ModuleImpl> eligibleModules = modulesFromSourceFiles.stream()
                                        .filter(bootstrapModules::contains)
                                        .peek(m -> {
                                            //Copy the module's hcm site name and hstRoot (if exist) from bootstrap module
                                            ModuleImpl source = bootstrapModules.get(bootstrapModules.indexOf(m));
                                            m.setHstRoot(source.getHstRoot());
                                        })
                                        .collect(toList());

                                // add all of the filesystem modules to a new model as "replacements" that override later additions
                                log.debug("merging source modules for auto-export");
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
                        } else {
                            // if starting auto-export was disallowed to begin with, notify devs via the log
                            log.info("Running autoexport service not allowed (requires appropriate system parameters to be set first)");
                        }

                        // we have a real difference, we're in "full" bootstrap mode, or we are setting up for auto-export
                        log.debug("applying core pre-migrators");
                        applyPreMigrators(bootstrapModel, emptySet(), ConfigurationMigrator.class);

                        if (!hcmSiteRecords.isEmpty()) {
                            // if the registered site list is not empty then run pre site migrators for the site(s)
                            log.debug("applying site pre-migrators during init for sites: {}", knownHcmSites);
                            applyPreMigrators(bootstrapModel, hcmSiteRecords.values(),
                                    ConfigurationSiteMigrator.class);
                        }

                        // If use.hcm.sites == true and no hst sites are available yet & boostrap model has /hst:hst then consider this
                        // as error, since core bootstrap model should not contain any hst specific configuration
                        if (USE_HCM_SITES_MODE && isEmpty(knownHcmSites)) {
                            final ConfigurationNodeImpl hstRootNode = bootstrapModel.getConfigurationRootNode().getNode("hst:hst");
                            if (hstRootNode != null && hstRootNode.getProperty("jcr:primaryType").getValue().getString().equals("hst:hst")) {
                                throw new IllegalArgumentException("Core bootstrap model contains hst nodes");
                            }
                        }

                        log.info("ConfigurationService: apply bootstrap config");
                        boolean configAppliedSuccessfully = applyConfig(baselineModel, bootstrapModel, false, verify, fullConfigure, !first, true);

                        if (configAppliedSuccessfully) {
                            // set runtimeConfigurationModel from bootstrapModel -- this is a reasonable default in case of exception
                            runtimeConfigurationModel = bootstrapModel;

                            log.info("ConfigurationService: store bootstrap config as baseline");
                            configAppliedSuccessfully = storeBaselineModel(bootstrapModel);
                        }

                        boolean contentAppliedSuccessfully = false;

                        if (configAppliedSuccessfully) {
                            log.info("ConfigurationService: apply bootstrap content");
                            // use bootstrap modules, because that's the only place content sources really exist
                            contentAppliedSuccessfully = applyContent(bootstrapModel);
                        }

                        if (configAppliedSuccessfully && contentAppliedSuccessfully) {
                            // if we're in a mode that allows auto-export, keep a copy of the baseline for future use
                            if (startAutoExportService) {
                                // reload the baseline after storing, so we have a JCR-backed view of our modules
                                log.debug("reloading stored baseline during init with sites: {}", knownHcmSites);
                                baselineModel = loadBaselineModel(knownHcmSites);

                                this.baselineModel = baselineModel;
                            }

                            // Unfortunately, using the JCR-backed baseline model leaves us vulnerable to problems
                            // if another cluster node stores a new baseline while we're holding onto this one.
                            // We need to keep the jar-backed model, despite the memory cost.

                            runtimeConfigurationModel = bootstrapModel;
                        }

                        log.info("ConfigurationService: start repository services");
                        startRepositoryServicesTask.execute();
                        if (configAppliedSuccessfully) {
                            log.info("ConfigurationService: start post-startup tasks");
                            // we need the bootstrap model here, not the baseline, so we can access the jar content
                            applyWebfiles(bootstrapModel, null);
                        }

                        boolean autoExportRunning = false;
                        if (startAutoExportService) {
                            if(configAppliedSuccessfully && contentAppliedSuccessfully) {
                                log.info("ConfigurationService: start autoexport service");
                                autoExportRunning = startAutoExportService();
                            } else {
                                log.warn("ConfigurationService: skipping starting autoexport service due to bootstrap errors");
                            }
                        }

                        // post migrators need to run after the auto export service has been started because the
                        // changes of the migrators might have to be exported
                        log.debug("applying core post-migrators");
                        applyPostMigrators(bootstrapModel, emptySet(), autoExportRunning, ConfigurationMigrator.class);

                        if (!hcmSiteRecords.isEmpty()) {
                            // if the registered site list is not empty then run post site migrators for the site(s)
                            log.debug("applying site post-migrators during init for sites: {}", knownHcmSites);
                            applyPostMigrators(bootstrapModel, hcmSiteRecords.values(),
                                    autoExportRunning, ConfigurationSiteMigrator.class);
                        }
                    }
                } finally {
                }
            }
        } finally {
            try {
                lockManager.unlock();
            } catch (Exception e) {
                log.error("Failed to release the configuration lock", e);
            }
        }
    }

    private boolean shouldSkipConfigBecauseOfDigestMatch(final ConfigurationModelImpl bootstrapModel,
                                                         final ConfigurationModelImpl baselineModel) {
        // NOTE: This will not notice differences within content files, since these are not fully stored
        //       in the baseline. This will only notice added or removed content files, changed actions,
        //       or changed config files.
        // TODO v13.3: do this check based on the stored JCR property, before spending time loading the baseline,
        //       once we include webfile bundle digests in the main model digest

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        // In case of setting repo.bootstrap parameter as "full" or repo.autoexport.allowed
        // parameter as "true", bootstrap should run to create the model regardless of digest check.
        boolean skip = !fullConfigure && !startAutoExportService
                && bootstrapModel.currentSitesMatchByDigests(baselineModel);

        stopWatch.stop();
        log.debug("digest comparison in {}", stopWatch.toString());

        return skip;
    }

    /**
     * Helper method when we're short-circuiting main bootstrap. Used when repo.bootstrap = false or when
     * repo.bootstrap = true and the baseline matches the incoming bootstrap model.
     * @param startRepositoryServicesTask
     * @param baselineModel
     * @throws RepositoryException
     */
    private void initWithoutBootstrap(final StartRepositoryServicesTask startRepositoryServicesTask, final ConfigurationModelImpl baselineModel) throws RepositoryException {
        // if we're not doing any bootstrap, use the baseline model as our runtime model
        runtimeConfigurationModel = baselineModel;

        log.info("ConfigurationService: start repository services");
        startRepositoryServicesTask.execute();
    }

    private <T> void applyPreMigrators(final ConfigurationModelImpl bootstrapModel, final Collection<SiteRecord> siteRecords,
            Class<T> configurationMigratorClass) {
        log.info("Loading preMigrators");
        final boolean coreMigrators = configurationMigratorClass.equals(ConfigurationMigrator.class);
        final List<T> preMigrators = loadMigrators(coreMigrators ? PreMigrator.class : PreSiteMigrator.class,
                configurationMigratorClass);
        if (!preMigrators.isEmpty()) {
            log.info("Running preMigrators: {}", preMigrators);
            if (coreMigrators) {
                runMigrators(bootstrapModel, preMigrators, null, false);
            } else {
                for (SiteRecord siteRecord : siteRecords) {
                    runMigrators(bootstrapModel, preMigrators, siteRecord, false);
                }
            }
        }
    }

    private <T> void applyPostMigrators(final ConfigurationModelImpl bootstrapModel, final Collection<SiteRecord> siteRecords,
            final boolean autoExportRunning, Class<T> configurationMigratorClass) throws RepositoryException {
        log.info("Loading postMigrators");
        final boolean coreMigrators = configurationMigratorClass.equals(ConfigurationMigrator.class);
        final List<T> postMigrators = loadMigrators(coreMigrators ? PostMigrator.class : PostSiteMigrator.class,
                configurationMigratorClass);
        if (!postMigrators.isEmpty()) {
            try {
                if (session.hasPendingChanges()) {
                    throw new IllegalStateException("Pending changes at this moment not allowed");
                }
                log.debug("ConfigurationService: Resetting ObservationManager userData before running postMigrators to enable auto-export of their changes (if any).");
                session.getWorkspace().getObservationManager().setUserData(null);
                log.info("ConfigurationService: Running postMigrators: {}", postMigrators);

                if (coreMigrators) {
                    runMigrators(bootstrapModel, postMigrators, null, autoExportRunning);
                } else {
                    for (SiteRecord sideRecord : siteRecords) {
                        runMigrators(bootstrapModel, postMigrators, sideRecord, autoExportRunning);
                    }
                }
            } finally {
                log.debug("ConfigurationService: Setting ObservationManager userData again to {} to skip further change events from this session for auto-export", Constants.HCM_ROOT);
                session.getWorkspace().getObservationManager().setUserData(Constants.HCM_ROOT);
            }
        }
    }

    public void stop() {
        log.info("ConfigurationService: stop");

        HippoWebappContextRegistry.get().removeTracker(this);

        hcmSiteRecords.clear();

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
        if (baselineModel != null) {
            try {
                // Ensure baselineModel resources are cleaned up (if any)
                baselineModel.close();
            } catch (Exception e) {
                log.error("Error closing baseline configuration", e);
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
            try (ConfigurationModelImpl configurationModelImpl = new ConfigurationModelImpl()) {
                return applyConfig(configurationModelImpl.build(), loadBootstrapModel(), true, false, true, false, true);
            }
        } finally {
            lockManager.unlock();
        }
    }

    /**
     * INTERNAL USAGE ONLY
     **/
    @Override
    public ConfigurationModel getBaselineModel() {
        return baselineModel;
    }

    /**
     * INTERNAL USAGE ONLY
     **/
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

        try (FileSystem zipFileSystem = ZipCompressor.createZipFileSystem(zipFile.getAbsolutePath(), false)) {
            final Path zipRootPath = zipFileSystem.getPath("/");
            final ModuleImpl module = new ModuleImpl("import-module", new ProjectImpl("import-project", new GroupImpl("import-group")));
            final ModuleContext moduleContext = new ImportModuleContext(module, zipRootPath);
            try {
                new ModuleReader().readModule(module, moduleContext, false);

                // todo: check for missing content source
                final ContentDefinitionImpl contentDefinition = module.getContentSources().iterator().next().getContentDefinition();
                contentService.importNode(contentDefinition.getNode(), parentNode, ActionType.RELOAD);
            } catch (Exception e) {
                throw new RepositoryException("Import failed", e);
            }
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
            throw new RepositoryException("Import failed", e);
        }
    }

    public String exportContent(final Node nodeToExport) throws RepositoryException, IOException {

        final ModuleImpl module = contentService.exportNode(nodeToExport);

        final ModuleContext moduleContext = new ConsoleExportModuleContext(module);
        final ContentSourceSerializer contentSourceSerializer = new ContentSourceSerializer(moduleContext, module.getContentSources().iterator().next(), false);

        final org.yaml.snakeyaml.nodes.Node node = contentSourceSerializer.representSource();
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        contentSourceSerializer.serializeNode(out, node);
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
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
            for (final SiteRecord record : hcmSiteRecords.values()) {
                model = modelReader.readSite(record.siteName, record.hstRoot, record.servletContext.getClassLoader(), model);
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
        final String projectDir = System.getProperty(Constants.PROJECT_BASEDIR_PROPERTY);

        // if project.basedir is defined, and auto-export config mentions a module, load it from the filesystem
        final ConfigurationPropertyImpl autoExportModulesProp =
                bootstrapModel.resolveProperty(AutoExportConstants.SERVICE_CONFIG_PATH
                        + "/" + AutoExportConstants.CONFIG_MODULES_PROPERTY_NAME);
        Map<String, Pair<String, Collection<String>>> modulesConfig = null;
        if (autoExportModulesProp != null) {
            final ArrayList<String> moduleStrings = new ArrayList<>();
            for (ValueImpl value : autoExportModulesProp.getValues()) {
                moduleStrings.add(value.getString());
            }
            // reuse the auto-export logic to tweak the defined config as necessary
            modulesConfig = AutoExportConfig.processModuleStrings(moduleStrings,false);
        }

        if (MapUtils.isEmpty(modulesConfig)) {
            return Collections.emptyList();
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

            // When loading module from disk, use hcm site info from matching module previously-loaded from jars
            final String siteName = modulesConfig.get(mvnModulePath).getLeft();
            final ModuleImpl module =
                    new ModuleReader().readReplacement(moduleDescriptorPath, bootstrapModel, siteName).getModule();

            // store mvnSourcePath on each module for later use by auto-export
            module.setMvnPath(mvnModulePath);
            modulesFromSourceFiles.add(module);
        }
        return modulesFromSourceFiles;
    }

    /**
     * @return a valid baseline without hcm site modules, if one exists, or an empty ConfigurationModel
     * @throws RepositoryException
     */
    private ConfigurationModelImpl loadBaselineModel() throws RepositoryException {
        return loadBaselineModel(null);
    }

    /**
     * @return a valid baseline, if one exists, or an empty ConfigurationModel
     * @param hcmSiteNames A set of hcm sites which should be included in the baseline
     * @throws RepositoryException only if an unexpected repository problem occurs (not if the baseline is missing)
     * TODO: specify a Set<String> of extension names here
     */
    private ConfigurationModelImpl loadBaselineModel(Set<String> hcmSiteNames) throws RepositoryException {
        try {
            ConfigurationModelImpl model = baselineService.loadBaseline(session, hcmSiteNames);
            if (model == null) {
                try (ConfigurationModelImpl configurationModelImpl = new ConfigurationModelImpl()) {
                    model = configurationModelImpl.build();
                }
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
                                final boolean verify, final boolean forceApply, final boolean mayFail, final boolean applyNamespaces)
            throws RepositoryException {
        try {
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();

            if (applyNamespaces) {
                configService.applyNamespacesAndNodeTypes(baseline, config, session);
            }
            configService.computeAndWriteDelta(baseline, config, session, forceApply);
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
     * fails during their {@link ConfigurationMigrator#migrate(Session, ConfigurationModel, boolean)} or
     * {@link ConfigurationSiteMigrator#migrate(Session, ConfigurationModel, JcrPath, boolean)}, {@code false} is
     * returned.
     */

    private <T> void runMigrators(final ConfigurationModel model, final List<T> migrators, SiteRecord siteRecord,
            final boolean autoExportRunning) {
        for (T migrator : migrators) {
            try {
                if (ConfigurationMigrator.class.isInstance(migrator)) {
                    ((ConfigurationMigrator) migrator).migrate(session, model, autoExportRunning);

                } else if (ConfigurationSiteMigrator.class.isInstance(migrator)) {
                    ((ConfigurationSiteMigrator) migrator).migrate(session, model, siteRecord.hstRoot,
                            autoExportRunning);
                }
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

    /**
     * Bootstrap webfile bundles
     *
     * @param bootstrapModel The configuration model to load webfilebundles from
     * @param siteName The site for which bundles are to be bootstrapped, or null to bootstrap
     *                 the bundles of all sites in this bootstrapModel
     */
    private void applyWebfiles(final ConfigurationModelImpl bootstrapModel, final String siteName) {
        final List<WebFileBundleDefinitionImpl> webFileBundleDefs =
                (siteName != null) ?
                        getWebFileBundleDefsForSite(bootstrapModel, siteName) :
                        bootstrapModel.getWebFileBundleDefinitions();
        try {
            boolean skip = configService.skipOrWriteWebfiles(webFileBundleDefs, baselineService, session);
            if (!skip) {
                session.save();
            }
        } catch (IOException | RepositoryException e) {
            log.error("Error initializing webfiles", e);
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

    private <T> List<T> loadMigrators(final Class<? extends Annotation> annotationClazz,
            Class<T> configurationMigratorClass) {
        Set<String> migratorClassNames = new ClasspathResourceAnnotationScanner().scanClassNamesAnnotatedBy(annotationClazz,
                "classpath*:org/hippoecm/**/*.class",
                "classpath*:org/onehippo/**/*.class",
                "classpath*:com/onehippo/**/*.class");

        List<T> migrators = new ArrayList<>();
        for (String migratorClassName : migratorClassNames) {
            try {
                Class<?> migratorClass = Class.forName(migratorClassName);

                Object object = migratorClass.newInstance();

                if (configurationMigratorClass.isInstance(object)) {
                    T migrator = (T) object;
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

    @Override
    @SuppressWarnings("unchecked")
    public void serviceRegistered(final ServiceHolder<HippoWebappContext> serviceHolder) {

        if (serviceHolder.getServiceObject().getType() == HippoWebappContext.Type.SITE && USE_HCM_SITES_MODE) {
            final ServletContext servletContext = serviceHolder.getServiceObject().getServletContext();
            final Map<String, String> hcmSiteConfig;
            try (final InputStream hcmSiteIs = servletContext.getResourceAsStream(HCM_SITE_DESCRIPTOR_LOCATION)) {
                if (hcmSiteIs == null) {
                    throw new FileNotFoundException(HCM_SITE_DESCRIPTOR_LOCATION);
                }
                final Yaml yamlReader = new Yaml();
                hcmSiteConfig = (Map<String, String>) yamlReader.load(hcmSiteIs);
            } catch (IOException e) {
                log.error(String.format("Failed to read %s", HCM_SITE_DESCRIPTOR), e);
                return;
            }
            final String hcmSiteName = hcmSiteConfig.get("name");
            final JcrPath hstRoot = JcrPaths.getPath(hcmSiteConfig.get("hstRoot"));
            try {
                lockManager.lock();
                try {
                    if (hcmSiteRecords.containsKey(hcmSiteName)) {
                        log.error("HCM Site: " + hcmSiteName + " already added");
                        return;
                    }
                    final SiteRecord record = new SiteRecord(hcmSiteName, hstRoot, servletContext);

                    // If no model is set yet, we are still processing the core in init(), so we should let the sites
                    // get processed along with the core and not process them separately.
                    if (runtimeConfigurationModel != null) {
                        // already initialized: apply hcm site
                        applySiteConfig(record);
                    }
                    hcmSiteRecords.put(hcmSiteName, record);
                } finally {
                    lockManager.unlock();
                }
            } catch (Exception e) {
                final String message = "Failed to add hcm site: " + hcmSiteName + " for context path: " + servletContext.getContextPath();
                log.error(message, e);
                throw new RuntimeException(message, e);
            }
        }
    }

    @Override
    public void serviceUnregistered(final ServiceHolder<HippoWebappContext> serviceHolder) {
        if (serviceHolder.getServiceObject().getType() == HippoWebappContext.Type.SITE && USE_HCM_SITES_MODE) {
            final String contextPath = serviceHolder.getServiceObject().getServletContext().getContextPath();
            try {
                lockManager.lock();
                try {
                    for (SiteRecord record : hcmSiteRecords.values()) {
                        if (record.servletContext.getContextPath().equals(contextPath)) {
                            // TODO: autoexport handling to be adjusted for this?
                            hcmSiteRecords.remove(record.siteName);
                            return;
                        }
                    }
                } finally {
                    lockManager.unlock();
                }
            } catch (RepositoryException e) {
                log.error("Failed to remove HCM Site for context path: "+contextPath, e);
            }
        }
    }
}
