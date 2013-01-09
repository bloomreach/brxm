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


public abstract class SettingsArraySetting<K extends YuiObject> extends Setting<K[]> {

    private static final long serialVersionUID = 1L;

    public SettingsArraySetting(String javascriptKey, K[] defaultValue) {
        super(javascriptKey, defaultValue);
    }

    public K[] newValue() {
        return defaultValue.clone();
    }

    public String getScriptValue(K[] value) {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        boolean first = true;
        for(K k : value) {
            if(!k.isValid()) {
                continue;
            }
            if(first) {
                first = false;
            } else {
                sb.append(',');
            }
            sb.append(k.toScript());
        }
        sb.append(']');
        return sb.toString();
    }

}
