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
import org.hippoecm.frontend.plugins.yui.IAjaxSettings;
import org.hippoecm.frontend.plugins.yui.JsFunction;

public class AjaxSettings extends YuiObject implements IAjaxSettings {

    private static final long serialVersionUID = 1L;

    protected static final FunctionSetting CALLBACK_FUNCTION = new FunctionSetting("callbackFunction", new JsFunction("function(){}"), false);

    protected static final YuiType TYPE = new YuiType(CALLBACK_FUNCTION);

    public AjaxSettings(YuiType type) {
        super(type);
    }

    public AjaxSettings(YuiType type, IPluginConfig config) {
        super(type, config);
    }

    public void setCallbackFunction(JsFunction function) {
        CALLBACK_FUNCTION.set(function, this);
    }

}
