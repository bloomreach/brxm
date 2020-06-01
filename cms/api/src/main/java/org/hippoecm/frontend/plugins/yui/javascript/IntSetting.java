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

import org.hippoecm.frontend.plugin.config.IPluginConfig;

public class IntSetting extends Setting<Integer> {

    private static final long serialVersionUID = 1L;

    public IntSetting(String javascriptKey) {
        this(javascriptKey, null);
    }

    public IntSetting(String javascriptKey, Integer defaultValue) {
        super(javascriptKey, defaultValue);
    }

    public Integer newValue() {
        return Integer.valueOf(defaultValue != null ? defaultValue : 0);
    }

    @Override
    protected Integer getValueFromConfig(IPluginConfig config, YuiObject settings) {
        return config.getInt(configKey);
    }

    public void setFromString(String value, YuiObject settings) {
        set(Integer.valueOf(value), settings);
    }

    public String getScriptValue(Integer value) {
        return value.toString();
    }

}
