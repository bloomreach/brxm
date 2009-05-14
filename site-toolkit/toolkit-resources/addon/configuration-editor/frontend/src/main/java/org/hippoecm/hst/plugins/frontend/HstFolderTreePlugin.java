package org.hippoecm.hst.plugins.frontend;

import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.cms.browse.tree.FolderTreePlugin;

public class HstFolderTreePlugin extends FolderTreePlugin {
    private static final long serialVersionUID = 1L;

    public HstFolderTreePlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);
    }

}
