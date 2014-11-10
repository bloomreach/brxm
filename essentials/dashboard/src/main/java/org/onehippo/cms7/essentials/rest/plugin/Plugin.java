package org.onehippo.cms7.essentials.rest.plugin;

import org.onehippo.cms7.essentials.dashboard.model.PluginDescriptor;

public class Plugin {
    private final PluginDescriptor descriptor;

    public Plugin(final PluginDescriptor descriptor) {
        this.descriptor = descriptor;
    }

    public PluginDescriptor getDescriptor() {
        return descriptor;
    }

    public String getId() {
        return descriptor.getId();
    }
}
