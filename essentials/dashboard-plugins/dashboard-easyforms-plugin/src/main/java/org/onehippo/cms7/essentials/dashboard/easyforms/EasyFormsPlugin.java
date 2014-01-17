package org.onehippo.cms7.essentials.dashboard.easyforms;


import org.onehippo.cms7.essentials.dashboard.Plugin;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.easyforms.installer.EasyFormsInstaller;
import org.onehippo.cms7.essentials.dashboard.installer.InstallablePlugin;

public class EasyFormsPlugin extends InstallablePlugin<EasyFormsInstaller> {

    public EasyFormsPlugin(String id, Plugin plugin, PluginContext context) {
        super(id, plugin, context);
    }


    @Override
    public EasyFormsInstaller getInstaller() {
        return new EasyFormsInstaller(getContext(), "http://forge.onehippo.org/ef/1.2");
    }
}
