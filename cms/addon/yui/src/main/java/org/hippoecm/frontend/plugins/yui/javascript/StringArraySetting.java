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

import java.util.StringTokenizer;

import org.hippoecm.frontend.plugin.config.IPluginConfig;

public class StringArraySetting extends Setting<String[]> {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private boolean escaped;

    public StringArraySetting(String javascriptKey, String... defaultValue) {
        this(javascriptKey, true, defaultValue);
    }

    public StringArraySetting(String javascriptKey, boolean escaped, String... defaultValue) {
        super(javascriptKey, defaultValue);
        this.escaped = escaped;
    }

    public Value<String[]> newValue() {
        return new StringArrayValue(defaultValue, escaped);
    }

    @Override
    protected String[] getValueFromConfig(IPluginConfig config, Settings settings) {
        return config.getStringArray(configKey);
    }

    public void setFromString(String value, Settings settings) {
        //TODO: check if I need to strip [] chars
        set(value.split(","), settings);
    }

}
