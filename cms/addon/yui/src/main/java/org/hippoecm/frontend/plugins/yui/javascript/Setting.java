package org.hippoecm.frontend.plugins.yui.javascript;

import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.hippoecm.frontend.plugin.config.IPluginConfig;

public abstract class Setting<K> implements ISetting<K> {
    private static final long serialVersionUID = 1L;

    K defaultValue;
    String configKey;
    String javascriptKey;
    
    public Setting(String javascriptKey, K defaultValue) {
        this.javascriptKey = javascriptKey;
        this.configKey = toConfigKey(javascriptKey);
        this.defaultValue = defaultValue;
    }
    
    public final K get(Settings settings) {
        return getValue(settings).get();
    }

    public final void set(K value, Settings settings) {
        getValue(settings).set(value);
    }
    
    public void setFromConfig(IPluginConfig config, Settings settings) {
        if(config.containsKey(configKey)) {
            set(getValueFromConfig(config, settings), settings);
        }
    }
    
    public String getKey() {
        return javascriptKey;
    }

    abstract protected K getValueFromConfig(IPluginConfig config, Settings settings);
    
    @SuppressWarnings("unchecked")
    Value<K> getValue(Settings settings) {
        return (Value<K>) settings.get(this);
    }
    
    private String toConfigKey(String camelKey) {        
        StringBuilder b = new StringBuilder(camelKey.length()+4);
        for(char ch : camelKey.toCharArray()) {
            if(Character.isUpperCase(ch)) {
                b.append('.').append(Character.toLowerCase(ch));
            } else {
                b.append(ch);
            }
        }
        return b.toString();
    }
    
    @Override
    public boolean equals(Object o) {
        if(o != null && o instanceof Setting) {
            Setting os = (Setting)o;
            return os.getKey().equals(getKey());
        }
        return false;
    }
    
    @Override
    public String toString() {
        return new ToStringBuilder(this).append("key", javascriptKey).toString();
    }
    
    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 13).append(javascriptKey).toHashCode();
    }
}
