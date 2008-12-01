package org.hippoecm.frontend.plugins.yui.javascript;

import org.hippoecm.frontend.plugin.config.IPluginConfig;

public class StringSetting extends Setting<String> {
    private static final long serialVersionUID = 1L;
    
    private boolean escape = true;

    public StringSetting(String javascriptKey) {
        this(javascriptKey, null);
    }

    public StringSetting(String javascriptKey, boolean escaped) {
        this(javascriptKey, null, escaped);
    }

    public StringSetting(String javascriptKey, String defaultValue) {
        super(javascriptKey, defaultValue);
    }

    public StringSetting(String javascriptKey, String defaultValue, boolean escape) {
        super(javascriptKey, defaultValue);
        this.escape = escape;
    }

    public Value<String> newValue() {
        return new StringValue(defaultValue, escape);
    }

    @Override
    protected String getValueFromConfig(IPluginConfig config, Settings settings) {
        return config.getString(configKey, defaultValue);
    }

    public void setFromString(String value, Settings settings) {
        set(value, settings);
    }
}
