package org.onehippo.cms7.essentials.plugin;

import org.onehippo.cms7.essentials.dashboard.config.*;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;

/**
 * Implements the state transitions and persistance of the per plugin installation state.
 */
class InstallStateMachine {

    private static Logger log = LoggerFactory.getLogger(InstallStateMachine.class);
    private InstallState state;
    private Plugin plugin;

    InstallStateMachine(final Plugin plugin) {
        this.plugin = plugin;
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

        if (PluginParameterServiceFactory.getParameterService(plugin).doesSetupRequireRebuild()) {
            log.info("Setting to Installing for plugin " + plugin);
            state = InstallState.INSTALLING;
        } else {
            log.info("Setting to Installed for plugin " + plugin);
            state = InstallState.INSTALLED;
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
        if (PluginParameterServiceFactory.getParameterService(plugin).hasSetup()) {
            log.info("Promoting/Short-circuiting to onBoard for plugin " + plugin);
            state = InstallState.ONBOARD;
        } else {
            log.info("Short-circuiting to Installed for plugin " + plugin);
            state = InstallState.INSTALLED;
        }
    }

    private InstallState loadStateFromFileSystem() {
        return loadState(false);
    }

    private InstallState loadStateFromWar() {
        return loadState(true);
    }

    private InstallState loadState(final boolean fromWar) {
        final PluginContext context = PluginContextFactory.getContext();
        InstallState state = InstallState.DISCOVERED;

        try (PluginConfigService resourceService = new ResourcePluginService(context);
             PluginConfigService fileService = new FilePluginService(context)) {
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
            final PluginContext context = PluginContextFactory.getContext();

            try (PluginConfigService service = new FilePluginService(context)) {
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
