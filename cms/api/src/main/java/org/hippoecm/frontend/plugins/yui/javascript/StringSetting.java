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

public class StringSetting extends Setting<String> {

    private static final long serialVersionUID = 1L;

    private static final String SINGLE_QUOTE = "'";
    private static final String SINGLE_QUOTE_ESCAPED = "\\'";

    private boolean escape = true;

    public StringSetting(String javascriptKey) {
        this(javascriptKey, null);
    }

    public StringSetting(String javascriptKey, boolean escaped) {
        this(javascriptKey, null, escaped);
    }

    public StringSetting(String javascriptKey, String defaultValue) {
        super(javascriptKey, defaultValue);
    }

    public StringSetting(String javascriptKey, String defaultValue, boolean escape) {
        super(javascriptKey, defaultValue);
        this.escape = escape;
    }

    public String newValue() {
        return defaultValue;
    }

    @Override
    protected String getValueFromConfig(IPluginConfig config, YuiObject settings) {
        return config.getString(configKey, defaultValue);
    }

    public void setFromString(String value, YuiObject settings) {
        set(value, settings);
    }

    public String getScriptValue(String value) {
      if(escape) {
          return escapeString(value);
      }
      return value;
    }

    static String escapeString(String value) {
        //TODO: backslash should be escaped as well
        if (value != null) {
            value = SINGLE_QUOTE + value.replace(SINGLE_QUOTE, SINGLE_QUOTE_ESCAPED) + SINGLE_QUOTE;
        }
        return value;
    }
}
