/*
 *  Copyright 2009 Hippo.
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

public class YuiIdSetting extends Setting<YuiId> {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    public YuiIdSetting(String javascriptKey) {
        super(javascriptKey, null);
    }

    public YuiIdSetting(String javascriptKey, YuiId defaultValue) {
        super(javascriptKey, defaultValue);
    }

    public YuiId newValue() {
        if (defaultValue != null) {
            YuiId id =  new YuiId(defaultValue.id);
            if (defaultValue.parentId != null) {
                id.setParentId(defaultValue.parentId);
            }
            return id;
        } else {
            return null;
        }
    }

    @Override
    protected YuiId getValueFromConfig(IPluginConfig config, YuiObject settings) {
        return new YuiId(config.getString(configKey));
    }

    public void setFromString(String value, YuiObject settings) {
        set(new YuiId(value), settings);
    }

    public String getScriptValue(YuiId value) {
        return StringSetting.escapeString(value.getElementId());
    }

}
