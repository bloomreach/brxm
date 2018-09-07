/*
 * Copyright 2014-2018 Hippo B.V. (http://www.onehippo.com)
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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.onehippo.cms7.essentials.plugin.PluginSet;
import org.onehippo.cms7.essentials.plugin.sdk.utils.EssentialConst;
import org.onehippo.cms7.essentials.sdk.api.model.rest.InstallState;
import org.onehippo.cms7.essentials.sdk.api.model.rest.PluginDescriptor;
import org.onehippo.cms7.essentials.sdk.api.model.rest.UserFeedback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Implements the state transitions and persistance of the per plugin installation state.
 */
@Component
public class InstallStateMachine {

    private static final Logger log = LoggerFactory.getLogger(InstallStateMachine.class);

    private final InstallService installService;

    @Inject
    public InstallStateMachine(final InstallService installService) {
        this.installService = installService;
    }

    /**
     * Call this method after a restart to see if to-be-installed plugin can be "advanced" through the plugin
     * installation state machine.
     *
     * @param pluginSet  set of all plugins to be considered
     * @param parameters installation parameters to apply
     * @param feedback   for signalling relevant messages to the user
     */
    public void signalRestart(final PluginSet pluginSet, final Map<String, Object> parameters, final UserFeedback feedback) {
        // first promote all transient install states
        pluginSet.getPlugins().forEach(this::promote);

        // then check if plugins can install through the state machine
        pluginSet.getPlugins().forEach(plugin -> {
            if (plugin.getState() == InstallState.INSTALLATION_PENDING) {
                parameters.put(EssentialConst.PROP_PLUGIN_DESCRIPTOR, plugin);
                installWithDependencies(plugin, pluginSet, parameters, feedback, new ArrayList<>());
            }
        });
    }

    /**
     * If a plugin is no longer waiting for a rebuild + restart, promote its state.
     */
    private void promote(final PluginDescriptor plugin) {
        final InstallState state = plugin.getState();
        if (state == installService.readInstallStateFromWar(plugin)) {
            switch (state) {
                case INSTALLING:
                    plugin.setState(InstallState.INSTALLED);
                    installService.storeInstallStateToFileSystem(plugin);
                    break;
            }
        }
    }

    /**
     * Try advancing the specified plugin through the installation state machine, as far as possible.
     *
     * @param id         ID of the plugin to be advanced through the installation state machine
     * @param pluginSet  set of available plugins, for validation and dependency mechanism
     * @param parameters installation parameters to use
     * @param feedback   for signalling relevant messages to the user
     * @return           false if the input cannot be validated, true otherwise
     */
    public boolean installWithDependencies(final String id, final PluginSet pluginSet,
                                           final Map<String, Object> parameters, final UserFeedback feedback) {

        if (!validatePlugin(id, pluginSet, feedback, new LinkedHashMap<>())) {
            return false;
        }

        installWithDependencies(pluginSet.getPlugin(id), pluginSet, parameters, feedback, new ArrayList<>());

        // Above installation may have unlocked the pending installation of upstream plugins.
        // Check if anything more needs to happen by faking a restart.
        signalRestart(pluginSet, parameters, feedback);
        return true;
    }

    /**
     * Try advancing the specified plugin through the installation state machine, as far as possible.
     *
     * @param id         ID of the plugin to be advanced through the installation state machine
     * @param pluginSet  set of available plugins, for validation and dependency mechanism
     * @param parameters installation parameters to use
     * @param feedback   for signalling relevant messages to the user
     * @return           false if the input cannot be validated, true otherwise
     */
    public boolean installWithParameters(final String id, final PluginSet pluginSet,
                                         final Map<String, Object> parameters, final UserFeedback feedback) {

        if (!validatePlugin(id, pluginSet, feedback, new LinkedHashMap<>())) {
            return false;
        }

        final PluginDescriptor plugin = pluginSet.getPlugin(id);
        if (plugin.getState() != InstallState.AWAITING_USER_INPUT) {
            feedback.addError("Failed to install plugin '" + plugin.getName() + "': Plugin is in unexpected state '" + plugin.getState() + "'.");
            return false;
        }

        installWithParameters(plugin, parameters, feedback);

        // Above installation may have unlocked the pending installation of upstream plugins.
        // Check if anything more needs to happen by faking a restart.
        signalRestart(pluginSet, parameters, feedback);
        return true;
    }

    /**
     * Depth-first search through the plugin's dependency tree,
     * look for missing plugins or cyclic dependencies,
     * accumulate errors.
     */
    private boolean validatePlugin(final String id, final PluginSet pluginSet, final UserFeedback feedback,
                               final LinkedHashMap<String, PluginDescriptor> parentPlugins) {
        final PluginDescriptor plugin = pluginSet.getPlugin(id);

        // plugin must exist
        if (plugin == null) {
            feedback.addError("Failed to locate plugin with ID '" + id + "'.");
            return false;
        }

        // plugin must not exist in parentPlugins set, or we have a circular dependency
        if (parentPlugins.containsKey(id)) {
            final String depChain = parentPlugins.values().stream()
                    .map(PluginDescriptor::getName)
                    .collect(Collectors.joining("' -> '"));
            log.warn("Dependency chain for cyclic plugin dependency is: '{}' -> '{}'.", depChain, plugin.getName());

            final PluginDescriptor rootPlugin = parentPlugins.entrySet().iterator().next().getValue();
            feedback.addError("Plugin '" + rootPlugin.getName() + "' has cyclic dependency, unable to install. Check back-end logs.");
            return false;
        }

        // recurse over all dependencies
        boolean allDepencendiesValid = true;
        final List<PluginDescriptor.Dependency> dependencies = plugin.getPluginDependencies();
        if (dependencies != null) {
            for (PluginDescriptor.Dependency dependency : dependencies) {
                if (allDepencendiesValid) {
                    parentPlugins.put(id, plugin);
                    allDepencendiesValid = validatePlugin(dependency.getPluginId(), pluginSet, feedback, parentPlugins);
                    parentPlugins.remove(id);
                }
            }
        }
        return allDepencendiesValid;
    }

    /**
     * Called after validation of the plugin against the pluginSet. Try to install the plugin, using the supplied
     * default installation parameters, collecting relevant user feedback as well as what dependent plugins limit
     * the installation of this plugin.
     *
     * This method is part of a multi-method recursion mechanism:
     *
     *      installWithDependencies -> advanceDependencies -> installWithDependencies
     */
    private void installWithDependencies(final PluginDescriptor plugin, final PluginSet pluginSet,
                                         final Map<String, Object> parameters, final UserFeedback feedback,
                                         final List<PluginDescriptor> limitingPlugins) {
        switch (plugin.getState()) {
            case DISCOVERED:
            case INSTALLATION_PENDING:
                if (!advanceDependencies(plugin, pluginSet, parameters, feedback, limitingPlugins)) {
                    break;
                }

                if (!installService.canAutoInstall(plugin)) {
                    plugin.setState(InstallState.AWAITING_USER_INPUT);
                    installService.storeInstallStateToFileSystem(plugin);
                    final String message = String.format("Plugin '%s' requires user input for installation.", plugin.getName());
                    feedback.addSuccess(message);
                    break;
                }

                installWithParameters(plugin, parameters, feedback);
                break;
        }
    }

    private void installWithParameters(final PluginDescriptor plugin, final Map<String, Object> parameters,
                                       final UserFeedback feedback) {

        if (!installService.install(plugin, parameters)) {
            return;
        }

        if (plugin.isRebuildAfterInstallation()) {
            plugin.setState(InstallState.INSTALLING);
            final String message = String.format("Plugin '%s' has been installed successfully, " +
                    "but requires a restart.", plugin.getName());
            feedback.addSuccess(message);
        } else {
            plugin.setState(InstallState.INSTALLED);
            final String message = String.format("Plugin '%s' has been installed successfully.", plugin.getName());
            feedback.addSuccess(message);
        }
        installService.storeInstallStateToFileSystem(plugin);
    }

    /**
     * Check if all dependencies of the plugin allow the plugin to be installed.
     * If at least one (potentially nested) dependency prevents the plugin from advancing, the plugin
     * enters a dedicated "pending" state, remembering the desire to be installed for a later point in time.
     */
    private boolean advanceDependencies(final PluginDescriptor plugin, final PluginSet pluginSet,
                                        final Map<String, Object> parameters, final UserFeedback feedback,
                                        final List<PluginDescriptor> limitingPlugins) {
        final List<PluginDescriptor.Dependency> dependencies = plugin.getPluginDependencies();
        if (dependencies != null) {
            for (PluginDescriptor.Dependency dependency : dependencies) {
                final String depId = dependency.getPluginId();
                final PluginDescriptor p = pluginSet.getPlugin(depId);
                if (mustAdvanceDependency(dependency, p)) {
                    if (p.getState() == InstallState.DISCOVERED) {
                        final String message = String.format("Installing dependent plugin '%s'...", p.getName());
                        feedback.addSuccess(message);
                    }
                    installWithDependencies(p, pluginSet, parameters, feedback, limitingPlugins);
                }
                if (mustAdvanceDependency(dependency, p)) {
                    limitingPlugins.add(p); // dependency is still in a state that keeps this plugin from advancing
                }
            }
        }

        if (!limitingPlugins.isEmpty()) {
            if (plugin.getState() != InstallState.INSTALLATION_PENDING) {
                plugin.setState(InstallState.INSTALLATION_PENDING);
                installService.storeInstallStateToFileSystem(plugin);
                addPendingFeedback(feedback, plugin, limitingPlugins);
            }
            return false;
        }

        return true;
    }

    private boolean mustAdvanceDependency(final PluginDescriptor.Dependency dependency,
                                          final PluginDescriptor plugin) {
        final InstallState testState = dependency.getMinStateForInstalling();
        return testState != null && plugin.getState().compareTo(testState) < 0;
    }

    private void addPendingFeedback(final UserFeedback feedback, final PluginDescriptor plugin,
                                    final List<PluginDescriptor> limitingPlugins) {
        final String deps = limitingPlugins.stream().map(PluginDescriptor::getName).collect(Collectors.joining("', '"));
        final String message = String.format("Installation of plugin '%s' is waiting for the installation of dependent plugin(s) '%s'.",
                plugin.getName(), deps);
        feedback.addSuccess(message);
    }
}
