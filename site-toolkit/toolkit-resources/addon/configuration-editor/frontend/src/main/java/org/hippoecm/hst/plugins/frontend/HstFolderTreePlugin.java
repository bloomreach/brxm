package org.hippoecm.hst.plugins.frontend;

import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JavaPluginConfig;
import org.hippoecm.frontend.plugins.cms.browse.tree.FolderTreePlugin;

public class HstFolderTreePlugin extends FolderTreePlugin {
    private static final long serialVersionUID = 1L;

    public HstFolderTreePlugin(IPluginContext context, IPluginConfig config) {
        super(context, new HstFolderTreeConfig(config));
    }

    static class HstFolderTreeConfig extends JavaPluginConfig {
        private static final long serialVersionUID = 1L;

        public HstFolderTreeConfig(IPluginConfig upstream) {
            super(upstream);
            upstream.put("path", "apelul!");
        }

    }
}
