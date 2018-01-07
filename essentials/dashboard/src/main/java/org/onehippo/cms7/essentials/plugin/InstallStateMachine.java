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

package org.onehippo.cms7.essentials.plugin;

import java.util.Calendar;

import org.onehippo.cms7.essentials.dashboard.config.FilePluginService;
import org.onehippo.cms7.essentials.dashboard.config.InstallerDocument;
import org.onehippo.cms7.essentials.dashboard.config.PluginConfigService;
import org.onehippo.cms7.essentials.dashboard.config.ResourcePluginService;
import org.onehippo.cms7.essentials.dashboard.service.ProjectService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements the state transitions and persistance of the per plugin installation state.
 */
class InstallStateMachine {

    private static Logger log = LoggerFactory.getLogger(InstallStateMachine.class);
    private final Plugin plugin;
    private final ProjectService projectService;
    private InstallState state;

    InstallStateMachine(final Plugin plugin, final ProjectService projectService) {
        this.plugin = plugin;
        this.projectService = projectService;
        this.state = loadStateFromFileSystem();
    }

    InstallState getState() {
        return state;
    }

    InstallState install() throws PluginException {
        if (state != InstallState.DISCOVERED) {
            throw new PluginException("Can't install already installed plugin.");
        }

        final boolean isPackaged = plugin.getDescriptor().getDependencies().size() == 0
                                && plugin.getDescriptor().getRepositories().size() == 0;
        if (isPackaged) {
            shortCircuitOnBoardState();
        } else {
            log.info("Setting to Boarding for plugin " + plugin);
            state = InstallState.BOARDING;
        }

        persistState();
        return state;
    }

    InstallState setup() throws PluginException {
        // TODO: the blog plugin abuses a set-up call to effectuate configuration changes.
        // It should be using different means!
        // when it does, the 3 lines of code below should be removed.
        if (state == InstallState.INSTALLED) {
            return state;
        }

        if (state != InstallState.ONBOARD) {
            throw new PluginException("Incorrect state to run setup.");
        }

        if (plugin.getDescriptor().isNoRebuildAfterSetup()) {
            log.info("Setting to Installed for plugin " + plugin);
            state = InstallState.INSTALLED;
        } else {
            log.info("Setting to Installing for plugin " + plugin);
            state = InstallState.INSTALLING;
        }

        persistState();
        return state;
    }

    InstallState promote() {
        if (state == loadStateFromWar()) {
            if (state == InstallState.BOARDING) {
                shortCircuitOnBoardState();
            }
            if (state == InstallState.INSTALLING) {
                log.info("Promoting to Installed for plugin " + plugin);
                state = InstallState.INSTALLED;
            }
        }

        persistState();
        return state;
    }

    private void shortCircuitOnBoardState() {
        // All features go through a setup phase.
        log.info("Promoting/Short-circuiting to onBoard for plugin " + plugin);
        state = InstallState.ONBOARD;
    }

    private InstallState loadStateFromFileSystem() {
        return loadState(false);
    }

    private InstallState loadStateFromWar() {
        return loadState(true);
    }

    private InstallState loadState(final boolean fromWar) {
        InstallState state = InstallState.DISCOVERED;

        try (PluginConfigService resourceService = new ResourcePluginService(projectService);
             PluginConfigService fileService = new FilePluginService(projectService)) {
            PluginConfigService service = fromWar ? resourceService : fileService;

            final InstallerDocument document = service.read(plugin.getId(), InstallerDocument.class);
            if (document != null) {
                state = InstallState.fromString(document.getInstallationState());
            }
        } catch (Exception e) {
            log.error("Error loading install-state for plugin {}", plugin.getId(), e);
        }

        return state;
    }

    private void persistState() {
        if (state != InstallState.DISCOVERED) {
            try (PluginConfigService service = new FilePluginService(projectService)) {
                InstallerDocument document = service.read(plugin.getId(), InstallerDocument.class);
                if (document == null) {
                    // create a new installer document
                    document = new InstallerDocument();

                    document.setName(plugin.getId());
                    document.setDateInstalled(Calendar.getInstance());
                }

                if (document.getDateAdded() == null
                        && (state == InstallState.INSTALLING || state == InstallState.INSTALLED)) {
                    document.setDateAdded(Calendar.getInstance());
                }

                document.setInstallationState(state.toString());
                service.write(document);
            } catch (Exception e) {
                log.error("Error updating install-state for plugin {}", plugin.getId(), e);
            }
        }
    }
}
