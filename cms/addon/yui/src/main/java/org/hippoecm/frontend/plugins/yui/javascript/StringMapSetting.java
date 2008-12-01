package org.hippoecm.frontend.plugins.yui.javascript;

import java.util.Map;

import org.apache.wicket.util.value.ValueMap;
import org.hippoecm.frontend.plugin.config.IPluginConfig;

public class StringMapSetting extends Setting<Map<String, String>> {
    private static final long serialVersionUID = 1L;

    public StringMapSetting(String javascriptKey) {
        this(javascriptKey, null);
    }

    public StringMapSetting(String javascriptKey, Map<String, String> defaultValue) {
        super(javascriptKey, defaultValue);
    }

    public Value<Map<String, String>> newValue() {
        return new StringMapValue(defaultValue);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Map<String, String> getValueFromConfig(IPluginConfig config, Settings settings) {
        return config.getPluginConfig(configKey);
    }

    public void setFromString(String value, Settings settings) {
        set(new ValueMap(value), settings);
    }
    
}
