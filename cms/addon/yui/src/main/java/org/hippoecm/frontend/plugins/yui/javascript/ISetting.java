package org.hippoecm.frontend.plugins.yui.javascript;

import org.apache.wicket.IClusterable;
import org.hippoecm.frontend.plugin.config.IPluginConfig;

public interface ISetting<K> extends IClusterable {
    
    void set(K value, Settings settings);
    K get(Settings settings);

    void setFromString(String value, Settings settings);
    void setFromConfig(IPluginConfig config, Settings settings);

    Value<K> newValue();
    
    String getKey();

}
