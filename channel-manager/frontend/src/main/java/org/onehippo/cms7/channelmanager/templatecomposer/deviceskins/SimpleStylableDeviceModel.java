package org.onehippo.cms7.channelmanager.templatecomposer.deviceskins;

import java.io.Serializable;

import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id$"
 */
public class SimpleStylableDeviceModel implements StyleableDevice, Serializable {

    private static Logger log = LoggerFactory.getLogger(SimpleStylableDeviceModel.class);

    protected final IPluginConfig config;
    private String name;

    public SimpleStylableDeviceModel(final IPluginConfig config) {
        this.config = config;
    }

    public String getStyle() {
        return config.containsKey("style") ? config.getString("style") : null;
    }

    @Override
    public String getWrapStyle() {
        return config.containsKey("wrapstyle") ? config.getString("wrapstyle") : null;
    }

    public String getName() {
        final String configName = config.getName();
        if (name == null) {
            name = configName.substring(configName.lastIndexOf('.') + 1);
        }
        return name;
    }

    @Override
    public String getId() {
        final String configName = config.getName();
        return configName.substring(configName.lastIndexOf('.') + 1);
    }

    public void setName(final String name) {
        this.name = name;
    }


}
