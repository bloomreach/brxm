package org.onehippo.cms7.essentials.rest.plugin;

import org.onehippo.cms7.essentials.dashboard.config.*;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContextFactory;
import org.onehippo.cms7.essentials.dashboard.model.PluginDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;

/**
 * Created by tjeger on 05/11/14.
 */
public class InstallStateMachine {

    private static Logger log = LoggerFactory.getLogger(InstallStateMachine.class);

    public enum State {
        DISCOVERED("discovered"),
        BOARDING("boarding"),
        ONBOARD("onBoard"),
        INSTALLING("installing"),
        INSTALLED("installed");

        private final String name;

        State(String name) {
            this.name = name;
        }

        public String toString() {
            return name;
        }

        private static State fromString(final String name) {
            for (State s : State.values()) {
                if (s.name.equals(name)) {
                    return s;
                }
            }
            return null;
        }
    }

    public static class StateException extends Exception {
        public StateException(final String msg) {
            super(msg);
        }
    }

    public static State getState(final PluginDescriptor plugin) {
        State state = State.fromString(plugin.getInstallState());
        if (state == null) {
            state = loadStateFromFileSystem(plugin);
            setState(plugin, state);
        }
        return state;
    }

    public static State install(final PluginDescriptor plugin) throws StateException {
        State state = getState(plugin);
        if (state != State.DISCOVERED) {
            throw new StateException("Can't install already installed plugin.");
        }

        final boolean isPackaged = plugin.getDependencies().size() == 0 && plugin.getRepositories().size() == 0;
        if (isPackaged) {
            // Plugin was packaged with Essentials WAR, no rebuild needed.
            final PluginParameterService params = PluginParameterServiceFactory.getParameterService(plugin);
            if (params.hasSetup()) {
                state = State.ONBOARD;
            } else {
                state = State.INSTALLED;
            }
        } else {
            state = State.BOARDING;
        }

        persistState(plugin, state);
        return state;
    }

    public static State setup(final PluginDescriptor plugin) throws StateException {
        State state = getState(plugin);
        if (state != State.ONBOARD) {
            throw new StateException("Incorrect state to run setup.");
        }

        final PluginParameterService params = PluginParameterServiceFactory.getParameterService(plugin);
        if (params.doesSetupRequireRebuild()) {
            state = State.INSTALLING;
        } else {
            state = State.INSTALLED;
        }

        persistState(plugin, state);
        return state;
    }

    public static State promote(final PluginDescriptor plugin) {
        State state = getState(plugin);

        if (state == loadStateFromWar(plugin)) {
            if (state == State.BOARDING) {
                state = State.ONBOARD;
            }
            if (state == State.INSTALLING) {
                state = State.INSTALLED;
            }
        }

        persistState(plugin, state);
        return state;
    }

    private static State loadStateFromFileSystem(final PluginDescriptor plugin) {
        return loadState(plugin, false);
    }

    private static State loadStateFromWar(final PluginDescriptor plugin) {
        return loadState(plugin, true);
    }

    private static State loadState(final PluginDescriptor plugin, final boolean fromWar) {
        final PluginContext context = PluginContextFactory.getContext(plugin);
        State state = State.DISCOVERED;

        try (PluginConfigService resourceService = new ResourcePluginService(context);
             PluginConfigService fileService = new FilePluginService(context)) {
            PluginConfigService service = fromWar ? resourceService : fileService;

            final InstallerDocument document = service.read(plugin.getId(), InstallerDocument.class);
            if (document != null) {
                state = State.fromString(document.getInstallationState());
            }
        } catch (Exception e) {
            log.error("Error loading install-state for plugin {}", plugin.getId(), e);
        }

        return state;
    }

    private static void persistState(final PluginDescriptor plugin, final State state) {
        final PluginContext context = PluginContextFactory.getContext(plugin);

        try (PluginConfigService service = new FilePluginService(context)) {
            InstallerDocument document = service.read(plugin.getId(), InstallerDocument.class);
            if (document == null) {
                // create a new installer document
                document = new InstallerDocument();

                document.setName(plugin.getId());
                document.setDateInstalled(Calendar.getInstance());
            }

            if (document.getDateAdded() == null
                && (state == State.INSTALLING || state == State.INSTALLED)) {
                document.setDateAdded(Calendar.getInstance());
            }

            document.setInstallationState(state.toString());
            service.write(document);
        } catch (Exception e) {
            log.error("Error updating install-state for plugin {}", plugin.getId(), e);
        }

        setState(plugin, state);
    }

    private static void setState(final PluginDescriptor plugin, final State state) {
        plugin.setInstallState(state.toString());
    }
}
