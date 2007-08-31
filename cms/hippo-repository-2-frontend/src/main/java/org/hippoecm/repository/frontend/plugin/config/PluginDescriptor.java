package org.hippoecm.repository.frontend.plugin.config;

import org.apache.wicket.IClusterable;
import org.hippoecm.repository.frontend.plugin.Plugin;

public class PluginDescriptor implements IClusterable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String className;
    private String path;

    public PluginDescriptor(String path, String className) {
        this.id = path.substring(path.lastIndexOf(":") + 1);
        this.className = className;
        this.path = path;
    }

    public PluginDescriptor(Plugin plugin) {
        this.id = plugin.getId();
        this.className = plugin.getClass().getName();
        this.path = plugin.getPath();
    }

    public String getId() {
        return id;
    }

    public String getClassName() {
        return className;
    }

    public String getPath() {
        return path;
    }

}
