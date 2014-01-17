package org.onehippo.cms7.essentials.dashboard.contentblocks;

import org.onehippo.cms7.essentials.dashboard.Plugin;
import org.onehippo.cms7.essentials.dashboard.contentblocks.installer.ContentBlocksInstaller;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.installer.InstallablePlugin;


/**
 * @author wbarthet
 */
public class ContentBlocksPlugin extends InstallablePlugin<ContentBlocksInstaller> {

    public ContentBlocksPlugin(final String id, final Plugin descriptor, final PluginContext context) {
        super(id, descriptor, context);
    }

    @Override
    public ContentBlocksInstaller getInstaller() {
        return new ContentBlocksInstaller();
    }
}
