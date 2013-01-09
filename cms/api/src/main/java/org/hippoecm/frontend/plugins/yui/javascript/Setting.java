/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.frontend.plugins.yui.javascript;

import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.wicket.IClusterable;
import org.hippoecm.frontend.plugin.config.IPluginConfig;

public abstract class Setting<K> implements IClusterable {

    private static final long serialVersionUID = 1L;

    K defaultValue;
    String configKey;
    String javascriptKey;
    boolean allowNull = false;

    public Setting(String javascriptKey, K defaultValue) {
        this.javascriptKey = javascriptKey;
        this.configKey = toConfigKey(javascriptKey);
        this.defaultValue = defaultValue;
    }

    public final K get(YuiObject settings) {
        return settings.get(this);
    }

    public final void set(K value, YuiObject settings) {
        settings.set(this, value);
    }

    public void setFromConfig(IPluginConfig config, YuiObject settings) {
        if (config.containsKey(configKey)) {
            set(getValueFromConfig(config, settings), settings);
        }
    }

    public String getKey() {
        return javascriptKey;
    }

    public boolean isValid(K value) {
        if (value == null && !allowNull) {
            return false;
        }
        return true;
    }

    public void setAllowNull(boolean allowNull) {
        this.allowNull = allowNull;
    }

    abstract public String getScriptValue(K value);

    abstract public K newValue();

    abstract public void setFromString(String value, YuiObject settings);

    abstract protected K getValueFromConfig(IPluginConfig config, YuiObject settings);

    private String toConfigKey(String camelKey) {
        StringBuilder b = new StringBuilder(camelKey.length() + 4);
        for (char ch : camelKey.toCharArray()) {
            if (Character.isUpperCase(ch)) {
                b.append('.').append(Character.toLowerCase(ch));
            } else {
                b.append(ch);
            }
        }
        return b.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (o != null && o instanceof Setting) {
            Setting os = (Setting) o;
            return os.getKey().equals(getKey());
        }
        return false;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE).append("key", javascriptKey).toString();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 13).append(javascriptKey).toHashCode();
    }

}
