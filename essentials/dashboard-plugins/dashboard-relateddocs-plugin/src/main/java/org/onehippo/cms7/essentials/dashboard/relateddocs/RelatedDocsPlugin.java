package org.onehippo.cms7.essentials.dashboard.relateddocs;

import org.onehippo.cms7.essentials.dashboard.Plugin;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.installer.InstallablePlugin;
import org.onehippo.cms7.essentials.dashboard.relateddocs.installer.RelatedDocsInstaller;

/**
 * @version "$Id$"
 */
public class RelatedDocsPlugin extends InstallablePlugin<RelatedDocsInstaller> {



    public RelatedDocsPlugin(final String id, final Plugin descriptor, final PluginContext context) {
        super(id, descriptor, context);
    }


    @Override
    public RelatedDocsInstaller getInstaller() {
        return new RelatedDocsInstaller(getContext(), "http://forge.onehippo.org/relateddocs/nt/1.1");
    }


}
