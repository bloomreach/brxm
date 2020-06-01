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

import java.util.HashMap;
import java.util.Map;

import org.apache.wicket.util.io.IClusterable;
import org.apache.wicket.util.value.IValueMap;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.util.PluginConfigMapper;

/**
 * Base class for passing configuration to a YUI component on the client.
 * <p>
 * This class has been deprecated.  Create a java bean instead and use the
 * {@link PluginConfigMapper} and JavascriptMapper helpers to do the serialization. 
 */
@Deprecated
public class YuiObject implements IClusterable {

    private static final long serialVersionUID = 1L;

    private YuiType type;
    private Map<Setting<?>, Object> settings;

    public YuiObject(YuiType type) {
        this.type = type;
        settings = new HashMap<Setting<?>, Object>();
        for (Setting<?> setting : type.getProperties()) {
            this.settings.put(setting, setting.newValue());
        }
    }

    public YuiObject(YuiType type, IPluginConfig config) {
        this(type);
        if(config != null) {
            for (Setting<?> setting : settings.keySet()) {
                setting.setFromConfig(config, this);
            }
        }
    }

    public YuiType getType() {
        return type;
    }

    public void updateValues(IValueMap options) {
        for (Setting<?> setting : settings.keySet()) {
            if (options.containsKey(setting.getKey())) {
                // FIXME: remove the fromString(.. toString) construction
                setting.setFromString(options.get(setting.getKey()).toString(), this);
            }
        }
    }

    <T> T get(Setting<T> key) {
        return (T) settings.get(key);
    }

    void set(Setting<?> key, Object value) {
        settings.put(key, value);
    }

    public String toScript() {
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        boolean first = true;
        for (Setting setting : type.getProperties()) {
            Object value = setting.get(this);
            if (setting.isValid(value)) {
                if (first) {
                    first = false;
                } else {
                    sb.append(',');
                }
                sb.append(setting.getKey());
                sb.append(": ");
                sb.append(setting.getScriptValue(value));
            }
        }
        sb.append('}');
        return sb.toString();
    }

    @Override
    /**
     * {@inheritDoc}
     * 
     * This method is not part of the YuiObject API.  Use #toScript instead.
     */
    public String toString() {
        return toScript();
    }
    
    public boolean isValid() {
        return true;
    }

}
