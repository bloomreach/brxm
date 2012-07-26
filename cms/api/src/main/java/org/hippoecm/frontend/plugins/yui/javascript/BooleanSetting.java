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

import org.hippoecm.frontend.plugin.config.IPluginConfig;

public class BooleanSetting extends Setting<Boolean> {

    private static final long serialVersionUID = 1L;

    public BooleanSetting(String javascriptKey) {
        this(javascriptKey, false);
    }

    public BooleanSetting(String javascriptKey, Boolean defaultValue) {
        super(javascriptKey, defaultValue);
    }

    public Boolean newValue() {
        return new Boolean(defaultValue);
    }

    @Override
    protected Boolean getValueFromConfig(IPluginConfig config, YuiObject settings) {
        return config.getBoolean(configKey);
    }

    public void setFromString(String value, YuiObject settings) {
        set(Boolean.valueOf(value), settings);
    }

    public String getScriptValue(Boolean value) {
        return value.toString();
    }
    
}
