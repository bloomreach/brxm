package org.onehippo.cms7.essentials.dashboard.installer;

import org.onehippo.cms7.essentials.dashboard.DashboardPlugin;
import org.onehippo.cms7.essentials.dashboard.Plugin;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;


/**
 * Extend this class if your plugin needs to be installed. This plugin offers a nice default interface for essentials
 * which need to be installed. It also provides creating your own plugin interface etc. etc.
 *
 * @version "$Id: InstallablePlugin.java 174582 2013-08-21 16:56:23Z mmilicevic $"
 */
public abstract class InstallablePlugin<T extends Installer> extends DashboardPlugin {


    public InstallablePlugin(final String id, final Plugin plugin, final PluginContext context) {
        super(id, plugin, context);
    }

    public InstallState getInstallState() {
        return getInstaller().getInstallState();
    }

    /**
     * overwrite if you need to check for namespace uri CNDUtils etc...
     *
     * @return
     */
    public boolean isInstalled() {
        return getInstaller().getInstallState().equals(InstallState.INSTALLED);
    }


    public abstract T getInstaller();
}
