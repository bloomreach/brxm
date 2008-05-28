package org.hippoecm.frontend.yui.dragdrop;

import java.util.List;

import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.PluginDescriptor;

public abstract class PluginDragDropBehavior extends AbstractDragDropBehavior {
    private static final long serialVersionUID = 1L;
    private Plugin plugin;

    public PluginDragDropBehavior(String... groups) {
        super(groups);
    }

    @Override
    protected void onBind() {
        super.onBind();
        PluginDescriptor descriptor = getPlugin().getDescriptor();
        List<String> groups = descriptor.getParameter("dd-groups").getStrings();
        if (groups.size() > 0) {
            if (descriptor.getParameter("dd-groups-overwrite").getBoolean()) {
                clearGroups();
            }
            for (String group : groups) {
                addGroup(group);
            }
        }
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
