package org.hippoecm.frontend.plugins.yui.javascript;

import org.hippoecm.frontend.plugin.config.IPluginConfig;

public class IntSetting extends Setting<Integer> {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    public IntSetting(String javascriptKey) {
        this(javascriptKey, null);
    }

    public IntSetting(String javascriptKey, Integer defaultValue) {
        super(javascriptKey, defaultValue);
    }

    public Value<Integer> newValue() {
        return new IntValue(defaultValue);
    }

    @Override
    protected Integer getValueFromConfig(IPluginConfig config, Settings settings) {
        return config.getInt(configKey);
    }

    public void setFromString(String value, Settings settings) {
        set(Integer.valueOf(value), settings);
    }
    
}
