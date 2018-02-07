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

        // then check if plugins can tryBoarding through the state machine
        pluginSet.getPlugins().forEach(plugin -> {
            if (plugin.getState() != InstallState.DISCOVERED) {
                tryBoarding(plugin, pluginSet, parameters, feedback, new ArrayList<>());
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
                case BOARDING:
                    plugin.setState(InstallState.ONBOARD);
                    installService.storeInstallStateToFileSystem(plugin);
                    break;
                case INSTALLING:
                    plugin.setState(InstallState.INSTALLED);
                    installService.storeInstallStateToFileSystem(plugin);
                    break;
            }
        }
    }

    /**
     * Try advancing the specified plugin through the boarding phase of the state machine, as far as possible.
     *
     * @param id         ID of the plugin to be advanced through the installation state machine
     * @param pluginSet  set of available plugins, for validation and dependency mechanism
     * @param parameters installation parameters to use
     * @param feedback   for signalling relevant messages to the user
     * @return           false if the input cannot be validated, true otherwise
     */
    public boolean tryBoarding(final String id, final PluginSet pluginSet, final Map<String, Object> parameters,
                               final UserFeedback feedback) {

        if (!validatePlugin(id, pluginSet, feedback, new LinkedHashMap<>())) {
            return false;
        }

        tryBoarding(pluginSet.getPlugin(id), pluginSet, parameters, feedback, new ArrayList<>());
        return true;
    }

    /**
     * Try advancing the specified plugin through the installation phase of the state machine, as far as possible.
     *
     * @param id         ID of the plugin to be advanced through the installation state machine
     * @param pluginSet  set of available plugins, for validation and dependency mechanism
     * @param parameters installation parameters to use
     * @param feedback   for signalling relevant messages to the user
     * @return           false if the input cannot be validated, true otherwise
     */
    public boolean tryInstallation(final String id, final PluginSet pluginSet, final Map<String, Object> parameters,
                                   final UserFeedback feedback) {

        if (!validatePlugin(id, pluginSet, feedback, new LinkedHashMap<>())) {
            return false;
        }

        tryInstallation(pluginSet.getPlugin(id), pluginSet, parameters, feedback, new ArrayList<>());

        // Above installation may have unlocked the pending boarding or installation of upstream plugins.
        // Check is anything more needs to happen by faking a restart.
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
     * Called after validation of the plugin against the pluginSet. Try to 'board' the plugin, using the supplied
     * installation parameters, collecting relevant user feedback as well as what dependent plugins limit the
     * boarding of this plugin.
     *
     * This method is part of a multi-method recursion mechanism:
     *
     *      tryBoarding -> board -> advanceDependencies -> tryBoarding
     *      tryBoarding -> tryInstallation -> install -> advanceDependencies -> tryBoarding
     */
    private void tryBoarding(final PluginDescriptor plugin, final PluginSet pluginSet, final Map<String, Object> parameters,
                             final UserFeedback feedback, final List<PluginDescriptor> limitingPlugins) {
        boolean boardingHappened = false;

        switch (plugin.getState()) {
            case DISCOVERED:
            case BOARDING_PENDING:
                board(plugin, pluginSet, parameters, feedback, limitingPlugins);
                if (plugin.getState() != InstallState.ONBOARD) {
                    return;
                }
                boardingHappened = true;

                // FALL-THROUGH!

            case ONBOARD:
                if (!installService.canAutoInstall(plugin)) {
                    if (boardingHappened) {
                        // Let the user know that the boarding phase was successful, even though the process has stopped.
                        final String message = String.format("The installation of plugin '%s' has been prepared.",
                                plugin.getName());
                        feedback.addSuccess(message);
                    }
                    return;
                }

                // FALL-THROUGH!

            case INSTALLATION_PENDING:
                tryInstallation(plugin, pluginSet, parameters, feedback, limitingPlugins);
                break;
        }
    }

    /**
     * Called after validation of the plugin against the pluginSet. Try to 'install' the plugin, using the supplied
     * installation parameters, collecting relevant user feedback as well as whatdependent plugins limit the
     * installation of this plugin.
     */
    private void tryInstallation(final PluginDescriptor plugin, final PluginSet pluginSet, final Map<String, Object> parameters,
                                 final UserFeedback feedback, final List<PluginDescriptor> limitingPlugins) {
        switch (plugin.getState()) {
            case ONBOARD:
            case INSTALLATION_PENDING:
                install(plugin, pluginSet, parameters, feedback, limitingPlugins);
                break;
        }
    }

    /**
     * Advance the plugin installation through the "boarding" phase, by (1) making sure all dependent plugins
     * allow the boarding of this plugin, (2) attempting to board this plugin and (3) updating the plugin state.
     */
    private void board(final PluginDescriptor plugin, final PluginSet pluginSet, final Map<String, Object> parameters,
                          final UserFeedback feedback, final List<PluginDescriptor> limitingPlugins) {

        if (!advanceDependencies(plugin, pluginSet, parameters, feedback, limitingPlugins, InstallState.ONBOARD)) {
            return;
        }

        if (!installService.board(plugin, feedback)) {
            return;
        }

        final boolean isPackaged = plugin.getDependencies().isEmpty() && plugin.getRepositories().isEmpty();
        if (!isPackaged) {
            final String message = String.format("The installation of plugin '%s' has been prepared, " +
                    "but requires a restart.", plugin.getName());
            feedback.addSuccess(message);
        }
        plugin.setState(isPackaged ? InstallState.ONBOARD : InstallState.BOARDING);
        installService.storeInstallStateToFileSystem(plugin);
    }

    /**
     * Advance the plugin installation through the "installation" phase, by (1) making sure all dependent plugins
     * allow the installation of this plugin, (2) attempting to install this plugin and (3) updating the plugin state.
     */
    private void install(final PluginDescriptor plugin, final PluginSet pluginSet, final Map<String, Object> parameters,
                         final UserFeedback feedback, final List<PluginDescriptor> limitingPlugins) {

        if (!advanceDependencies(plugin, pluginSet, parameters, feedback, limitingPlugins, InstallState.INSTALLED)) {
            return;
        }

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
     * Shared logic to check if all dependencies of te plugin allow the plugin to tryBoarding into the specified
     * target state. If at least one (potentially nested) dependency prevents the plugin from advancing, the plugin
     * enters a dedicated "pending" state, remembering the desire to tryBoarding for a later point in time.
     */
    private boolean advanceDependencies(final PluginDescriptor plugin, final PluginSet pluginSet,
                                        final Map<String, Object> parameters, final UserFeedback feedback,
                                        final List<PluginDescriptor> limitingPlugins, final InstallState targetState) {
        final List<PluginDescriptor.Dependency> dependencies = plugin.getPluginDependencies();
        if (dependencies != null) {
            for (PluginDescriptor.Dependency dependency : dependencies) {
                final String depId = dependency.getPluginId();
                final PluginDescriptor p = pluginSet.getPlugin(depId);
                if (mustAdvanceDependency(dependency, p, targetState)) {
                    if (p.getState() == InstallState.DISCOVERED) {
                        final String message = String.format("Installing dependent plugin '%s'...", p.getName());
                        feedback.addSuccess(message);
                    }
                    tryBoarding(p, pluginSet, parameters, feedback, limitingPlugins);
                }
                if (mustAdvanceDependency(dependency, p, targetState)) {
                    limitingPlugins.add(p); // dependency is still in a state that keeps this plugin from advancing
                }
            }
        }

        if (!limitingPlugins.isEmpty()) {
            plugin.setState(makePendingState(targetState));
            installService.storeInstallStateToFileSystem(plugin);
            addPendingFeedback(feedback, plugin, limitingPlugins, targetState);
            return false;
        }

        return true;
    }

    private boolean mustAdvanceDependency(final PluginDescriptor.Dependency dependency,
                                          final PluginDescriptor plugin, final InstallState targetState) {
        final InstallState testState = targetState == InstallState.ONBOARD
                ? dependency.getMinStateForBoarding()
                : dependency.getMinStateForInstalling();
        return testState != null && plugin.getState().compareTo(testState) < 0;
    }

    private InstallState makePendingState(final InstallState targetState) {
        return targetState == InstallState.ONBOARD ? InstallState.BOARDING_PENDING : InstallState.INSTALLATION_PENDING;
    }

    private void addPendingFeedback(final UserFeedback feedback, final PluginDescriptor plugin,
                                    final List<PluginDescriptor> limitingPlugins, final InstallState targetState) {
        final String deps = limitingPlugins.stream().map(PluginDescriptor::getName).collect(Collectors.joining("', '"));
        final String activity = targetState == InstallState.ONBOARD ? "Boarding" : "Installation";
        final String message = String.format("%s of plugin '%s' is waiting for the installation of dependent plugin(s) '%s'.",
                activity, plugin.getName(), deps);
        feedback.addSuccess(message);
    }
}
