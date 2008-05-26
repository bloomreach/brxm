package org.hippoecm.frontend.yui.dragdrop;

import org.hippoecm.frontend.plugin.Plugin;

public abstract class PluginDragDropBehavior extends AbstractDragDropBehavior {
    private static final long serialVersionUID = 1L;
    private Plugin plugin;

    public PluginDragDropBehavior(String... groups) {
        super(groups);
    }

    protected Plugin getPlugin() {
        if (plugin == null) {
            if (getComponent() instanceof Plugin) {
                plugin = (Plugin) getComponent();
            } else {
                plugin = (Plugin) getComponent().findParent(Plugin.class);
            }
        }
        return plugin;
    }

}
