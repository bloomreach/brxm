package org.onehippo.cms7.essentials.dashboard.contentblocks;

import org.onehippo.cms7.essentials.dashboard.EssentialsPlugin;
import org.onehippo.cms7.essentials.dashboard.Plugin;
import org.onehippo.cms7.essentials.dashboard.contentblocks.installer.ContentBlocksInstaller;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.installer.InstallState;
import org.onehippo.cms7.essentials.dashboard.installer.Installer;


/**
 * @author wbarthet
 */
public class ContentBlocksPlugin extends EssentialsPlugin {

    private final Installer installer;

    public ContentBlocksPlugin(final Plugin descriptor, final PluginContext context) {
        super(descriptor, context);
        installer = new ContentBlocksInstaller();
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
