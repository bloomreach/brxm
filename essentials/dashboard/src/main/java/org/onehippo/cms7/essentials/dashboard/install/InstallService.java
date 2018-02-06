/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.dashboard.install;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import com.google.common.base.Strings;

import org.onehippo.cms7.essentials.plugin.PluginSet;
import org.onehippo.cms7.essentials.plugin.sdk.config.InstallerDocument;
import org.onehippo.cms7.essentials.plugin.sdk.config.PluginFileService;
import org.onehippo.cms7.essentials.plugin.sdk.packaging.DefaultInstructionPackage;
import org.onehippo.cms7.essentials.plugin.sdk.services.RebuildServiceImpl;
import org.onehippo.cms7.essentials.plugin.sdk.utils.GlobalUtils;
import org.onehippo.cms7.essentials.plugin.sdk.utils.HstUtils;
import org.onehippo.cms7.essentials.rest.model.SystemInfo;
import org.onehippo.cms7.essentials.sdk.api.model.rest.InstallState;
import org.onehippo.cms7.essentials.sdk.api.model.rest.MavenDependency;
import org.onehippo.cms7.essentials.sdk.api.model.rest.MavenRepository;
import org.onehippo.cms7.essentials.sdk.api.model.rest.PluginDescriptor;
import org.onehippo.cms7.essentials.sdk.api.model.rest.UserFeedback;
import org.onehippo.cms7.essentials.sdk.api.service.JcrService;
import org.onehippo.cms7.essentials.sdk.api.service.MavenDependencyService;
import org.onehippo.cms7.essentials.sdk.api.service.MavenRepositoryService;
import org.onehippo.cms7.essentials.sdk.api.service.SettingsService;
import org.onehippo.cms7.essentials.sdk.api.model.Module;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * InstallService provides the tooling for insyalling plugins, primarily to the InstallStateMachine, which focuses on
 * the correct sequencing of tasks.
 */
@Service
public class InstallService {
    private final JcrService jcrService;
    private final SettingsService settingsService;
    private final RebuildServiceImpl rebuildService;
    private final MavenDependencyService dependencyService;
    private final MavenRepositoryService repositoryService;
    private final PluginFileService pluginFileService;
    private final AutowireCapableBeanFactory injector;

    @Inject
    public InstallService(final JcrService jcrService, final SettingsService settingsService,
                          final RebuildServiceImpl rebuildService, final MavenDependencyService dependencyService,
                          final MavenRepositoryService repositoryService, final PluginFileService pluginFileService,
                          final AutowireCapableBeanFactory injector) {
        this.jcrService = jcrService;
        this.settingsService = settingsService;
        this.rebuildService = rebuildService;
        this.dependencyService = dependencyService;
        this.repositoryService = repositoryService;
        this.pluginFileService = pluginFileService;
        this.injector = injector;
    }

    public long countInstalledPlugins(final PluginSet pluginSet) {
        return pluginSet.getPlugins().stream().filter(p -> p.getState() != InstallState.DISCOVERED).count();
    }

    public void populateSystemInfo(final PluginSet pluginSet, final SystemInfo systemInfo) {
        for (PluginDescriptor descriptor : pluginSet.getPlugins()) {
            systemInfo.incrementPlugins();
            final InstallState installState = descriptor.getState();
            final String pluginType = descriptor.getType();
            final boolean isTool = "tool".equals(pluginType);
            if (isTool) {
                systemInfo.incrementTools();
            }
            final boolean isFeature = "feature".equals(pluginType);
            if (isFeature && installState != InstallState.DISCOVERED) {
                systemInfo.incrementInstalledFeatures();
            }
            if (!isTool) {
                if (installState == InstallState.BOARDING || installState == InstallState.INSTALLING) {
                    systemInfo.addRebuildPlugin(descriptor);
                    systemInfo.setNeedsRebuild(true);
                } else if (installState == InstallState.ONBOARD) {
                    systemInfo.incrementConfigurablePlugins();
                }
            }
        }

        // check if we have external rebuild events:
        final Set<String> pluginIds = rebuildService.getRequestingPluginIds();
        for (String pluginId : pluginIds) {
            final PluginDescriptor descriptor = pluginSet.getPlugin(pluginId);
            if (descriptor != null) {
                systemInfo.setNeedsRebuild(true);
                systemInfo.addRebuildPlugin(descriptor);
            }
        }
    }

    public boolean board(final PluginDescriptor plugin, final UserFeedback feedback) {
        return installRepositories(plugin, feedback) && installDependencies(plugin, feedback);
    }

    public boolean canAutoInstall(final PluginDescriptor plugin) {
        return plugin.getState() == InstallState.ONBOARD
                && StringUtils.hasText(plugin.getPackageFile())  // TODO: is this still needed? packageFile is the only current mechanism for triggering actions during the installation.
                && (!plugin.isInstallWithParameters() || !settingsService.getSettings().isConfirmParams());
    }

    public boolean install(final PluginDescriptor plugin, final Map<String, Object> parameters) {
        HstUtils.erasePreview(jcrService, settingsService);

        final DefaultInstructionPackage instructionPackage = makeInstructionPackageInstance(plugin);
        if (instructionPackage != null) {
            instructionPackage.execute(parameters);
        }

        return true;
    }

    public InstallState readInstallStateFromWar(final PluginDescriptor plugin) {
        final String sanitized = pluginFileService.sanitizeFileName(plugin.getId());
        if (sanitized == null) {
            return null;
        }

        final String path = "/" + sanitized;
        final InputStream stream = getClass().getResourceAsStream(path);
        if (stream == null) {
            return null;
        }
        final InstallerDocument pluginFile = GlobalUtils.unmarshalStream(stream, InstallerDocument.class);
        return pluginFile != null ? InstallState.fromString(pluginFile.getInstallationState()) : InstallState.DISCOVERED;
    }

    public void loadInstallStateFromFileSystem(final PluginDescriptor plugin) {
        InstallState state = InstallState.DISCOVERED;

        final InstallerDocument document = pluginFileService.read(plugin.getId(), InstallerDocument.class);
        if (document != null) {
            state = InstallState.fromString(document.getInstallationState());
        }

        plugin.setState(state);
    }

    public void storeInstallStateToFileSystem(final PluginDescriptor plugin) {
        final InstallState state = plugin.getState();
        if (state != InstallState.DISCOVERED) {
            final String fileName = plugin.getId();

            InstallerDocument document = pluginFileService.read(fileName, InstallerDocument.class);
            if (document == null) {
                // create a new installer document
                document = new InstallerDocument();
                document.setDateInstalled(Calendar.getInstance());
            }

            if (document.getDateAdded() == null
                    && (state == InstallState.INSTALLING || state == InstallState.INSTALLED)) {
                document.setDateAdded(Calendar.getInstance());
            }

            document.setInstallationState(state.toString());
            pluginFileService.write(fileName, document);
        }
    }

    private boolean installRepositories(final PluginDescriptor plugin, final UserFeedback feedback) {
        final List<String> failedRepositoryUrls = new ArrayList<>();

        for (MavenRepository.WithModule repository : plugin.getRepositories()) {
            if (!repositoryService.addRepository(Module.pomForName(repository.getTargetPom()), repository)) {
                failedRepositoryUrls.add(repository.getUrl());
            }
        }

        if (!failedRepositoryUrls.isEmpty()) {
            final String urls = failedRepositoryUrls.stream().collect(Collectors.joining(", "));
            final String message = String.format("Failed to install plugin '%s': These repositories failed to install: %s.",
                    plugin.getName(), urls);
            feedback.addError(message);
            return false;
        }
        return true;
    }

    private boolean installDependencies(final PluginDescriptor plugin, final UserFeedback feedback) {
        final List<String> failedDependencies = new ArrayList<>();

        for (MavenDependency.WithModule dependency : plugin.getDependencies()) {
            if (!dependencyService.addDependency(Module.pomForName(dependency.getTargetPom()), dependency)) {
                failedDependencies.add(String.format("%s:%s", dependency.getGroupId(), dependency.getArtifactId()));
            }
        }

        if (!failedDependencies.isEmpty()) {
            final String deps = failedDependencies.stream().collect(Collectors.joining(", "));
            final String message = String.format("Failed to install plugin '%s': These dependencies failed to install: %s.",
                    plugin.getName(), deps);
            feedback.addError(message);
            return false;
        }
        return true;
    }

    public DefaultInstructionPackage makeInstructionPackageInstance(final PluginDescriptor plugin) {
        final String packageFile = plugin.getPackageFile();
        if (!Strings.isNullOrEmpty(packageFile)) {
            final DefaultInstructionPackage instructionPackage = injector.createBean(DefaultInstructionPackage.class);
            instructionPackage.setInstructionPath(packageFile);
            return instructionPackage;
        }
        return null;
    }
}
