package org.hippoecm.hst.plugins.frontend.editor.dialogs;

import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JavaPluginConfig;

public abstract class HstPickerConfig extends JavaPluginConfig {
    private static final long serialVersionUID = 1L;

    public HstPickerConfig(IPluginConfig config) {
        super(config);
        if (!getPluginConfig("cluster.options").containsKey("content.path")) {
            IPluginConfig cc = new JavaPluginConfig(getPluginConfig("cluster.options"));
            cc.put("content.path", getPath());
            put("cluster.options", cc);
        }
    }

    protected abstract String getPath();

}
