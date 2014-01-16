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


    public enum PluginType {

        LISTVIEWPLUGIN("org.hippoecm.frontend.service.render.ListViewPlugin"), TWOCOLUMN("org.hippoecm.frontend.editor.layout.TwoColumn"), UNKNOWN("unknown");
        String clazz;

        PluginType(String clazz) {
            this.clazz = clazz;
        }

        public static PluginType get(String clazz) {
            for (PluginType a : PluginType.values()) {
                if (a.clazz.equals(clazz)) {
                    return a;
                }
            }
            return UNKNOWN;
        }

        public String getClazz() {
            return clazz;
        }

    }


}
