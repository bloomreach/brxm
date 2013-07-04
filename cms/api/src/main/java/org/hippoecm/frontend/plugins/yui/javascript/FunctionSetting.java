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
import org.hippoecm.frontend.plugins.yui.JsFunction;

public class FunctionSetting extends Setting<JsFunction> {

    private static final long serialVersionUID = 1L;

    private static final String SINGLE_QUOTE = "'";
    private static final String SINGLE_QUOTE_ESCAPED = "\\'";

    private boolean escape = true;

    public FunctionSetting(String javascriptKey, JsFunction defaultValue, boolean escape) {
        super(javascriptKey, defaultValue);
        this.escape = escape;
    }

    public JsFunction newValue() {
        return defaultValue;
    }

    @Override
    protected JsFunction getValueFromConfig(IPluginConfig config, YuiObject settings) {
        return new JsFunction(config.getString(configKey, defaultValue.getFunction()));
    }

    public void setFromString(String value, YuiObject settings) {
        set(new JsFunction(value), settings);
    }

    @Override
    public String getScriptValue(final JsFunction value) {
        return value.getFunction();
    }

}
