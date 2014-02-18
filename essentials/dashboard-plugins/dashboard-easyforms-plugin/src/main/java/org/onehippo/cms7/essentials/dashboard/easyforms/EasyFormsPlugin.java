package org.onehippo.cms7.essentials.dashboard.easyforms;


import org.onehippo.cms7.essentials.dashboard.EssentialsPlugin;
import org.onehippo.cms7.essentials.dashboard.Plugin;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.easyforms.installer.EasyFormsInstaller;
import org.onehippo.cms7.essentials.dashboard.installer.InstallState;
import org.onehippo.cms7.essentials.dashboard.installer.Installer;

public class EasyFormsPlugin extends EssentialsPlugin {

    private final Installer installer;

    public EasyFormsPlugin(Plugin plugin, PluginContext context) {
        super(plugin, context);
        installer = new EasyFormsInstaller(context, "http://forge.onehippo.org/ef/1.2");
    }

    @Override
    public void install() {
        installer.install();
    }

    @Override
    public InstallState getInstallState() {
        return installer.getInstallState();
    }

    @Override
    public boolean isInstalled() {
        return installer.isInstalled();
    }

}
