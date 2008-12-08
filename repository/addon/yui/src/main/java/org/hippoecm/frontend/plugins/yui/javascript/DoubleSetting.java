package org.hippoecm.frontend.plugins.yui.javascript;

import org.hippoecm.frontend.plugin.config.IPluginConfig;


public class DoubleSetting extends Setting<Double> {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;
    
    public DoubleSetting(String javascriptKey) {
        this(javascriptKey, null);
    }
    
    public DoubleSetting(String javascriptKey, Double defaultValue) {
        super(javascriptKey, defaultValue);
    }

    @Override
    protected Double getValueFromConfig(IPluginConfig config, Settings settings) {
        return config.getDouble(configKey);
    }

    public  Value<Double> newValue() {
        return new DoubleValue(defaultValue);
    }

    public void setFromString(String value, Settings settings) {
        set(Double.valueOf(value), settings);
    }

}
