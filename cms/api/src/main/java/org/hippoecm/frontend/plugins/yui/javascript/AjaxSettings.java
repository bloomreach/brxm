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
import org.hippoecm.frontend.plugins.yui.IAjaxSettings;
import org.hippoecm.frontend.plugins.yui.JsFunction;

import java.util.Map;

public class AjaxSettings extends YuiObject implements IAjaxSettings {

    private static final long serialVersionUID = 1L;

    protected static final StringSetting CALLBACK_URL = new StringSetting("callbackUrl", "");
    protected static final StringSetting CALLBACK_FUNCTION = new StringSetting("callbackFunction", "", false);
    protected static final StringMapSetting CALLBACK_PARAMETERS = new StringMapSetting("callbackParameters", null);

    protected static final YuiType TYPE = new YuiType(CALLBACK_URL, CALLBACK_FUNCTION, CALLBACK_PARAMETERS);

    public AjaxSettings(YuiType type) {
        super(type);
    }

    public AjaxSettings(YuiType type, IPluginConfig config) {
        super(type, config);
    }

    public void setCallbackUrl(String url) {
        CALLBACK_URL.set(url, this);
    }

    public void setCallbackFunction(JsFunction function) {
        CALLBACK_FUNCTION.set(function.getFunction(), this);
    }

    public void setCallbackParameters(Map<String, Object> map) {
        CALLBACK_PARAMETERS.set(map, this);
    }

}
