/*
 *  Copyright 2008 Hippo.
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
import java.util.Map.Entry;

import org.apache.wicket.IClusterable;
import org.hippoecm.frontend.plugin.config.IPluginConfig;

public abstract class Settings implements IClusterable {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private Map<Setting<?>, Value<?>> settings;

    public Settings() {
        settings = new HashMap<Setting<?>, Value<?>>();
        initValues();
    }

    public Settings(IPluginConfig config) {
        this();
        for (Setting<?> setting : settings.keySet()) {
            setting.setFromConfig(config, this);
        }
    }

    /**
     * Template method for settings
     */
    protected abstract void initValues();

    public void updateValues(Map<String, String> options) {
        for (Entry<Setting<?>, Value<?>> entry : settings.entrySet()) {
            Setting<?> setting = entry.getKey();
            if (options.containsKey(setting.getKey())) {
                setting.setFromString(options.get(setting.getKey()), this);
            }
        }
    }

    //initialise settings
    protected void add(Setting<?>... settings) {
        for (Setting<?> setting : settings) {
            this.settings.put(setting, setting.newValue());
        }
    }

    protected void skip(Setting<?>... settings) {
        for (Setting<?> setting : settings) {
            this.settings.get(setting).setSkip(true);
        }
    }

    Value<?> get(Setting<?> key) {
        return settings.get(key);
    }

    void set(Setting<?> key, Value<?> value) {
        settings.put(key, value);
    }

    public String toScript() {
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        boolean first = true;
        for (Entry<Setting<?>, Value<?>> entry : settings.entrySet()) {
            if(entry.getValue().isValid()) {
                if (first) {
                    first = false;
                } else {
                    sb.append(',');
                }
                sb.append(entry.getKey().getKey() + ": " + entry.getValue().getScriptValue());
            }
        }
        sb.append('}');
        return sb.toString();
    }

    public boolean isValid() {
        return true;
    }

}
