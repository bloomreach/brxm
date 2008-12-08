package org.hippoecm.frontend.plugins.yui.javascript;

import org.hippoecm.frontend.plugin.config.IPluginConfig;

public class BooleanSetting extends Setting<Boolean> {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;
    
    public BooleanSetting(String javascriptKey) {
        this(javascriptKey, false);
    }

    public BooleanSetting(String javascriptKey, Boolean defaultValue) {
        super(javascriptKey, defaultValue);
    }
    
    public Value<Boolean> newValue() {
        return new BooleanValue(defaultValue);
    }

    @Override
    protected Boolean getValueFromConfig(IPluginConfig config, Settings settings) {
        return config.getBoolean(configKey);
    }

    public void setFromString(String value, Settings settings) {
        set(Boolean.valueOf(value), settings);
    }

}
