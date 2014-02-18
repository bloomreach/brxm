package org.onehippo.cms7.essentials.dashboard.relateddocs;

import org.onehippo.cms7.essentials.dashboard.EssentialsPlugin;
import org.onehippo.cms7.essentials.dashboard.Plugin;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.installer.InstallState;
import org.onehippo.cms7.essentials.dashboard.installer.Installer;
import org.onehippo.cms7.essentials.dashboard.relateddocs.installer.RelatedDocsInstaller;

/**
 * @version "$Id$"
 */
public class RelatedDocsPlugin extends EssentialsPlugin {


    private final Installer installer;

    public RelatedDocsPlugin(final Plugin descriptor, final PluginContext context) {
        super(descriptor, context);
        installer = new RelatedDocsInstaller(getContext(), "http://forge.onehippo.org/relateddocs/nt/1.1");
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
