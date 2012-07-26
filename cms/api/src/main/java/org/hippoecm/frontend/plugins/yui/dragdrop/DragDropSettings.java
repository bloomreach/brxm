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

package org.hippoecm.frontend.plugins.yui.dragdrop;

import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.yui.javascript.AjaxSettings;
import org.hippoecm.frontend.plugins.yui.javascript.StringArraySetting;
import org.hippoecm.frontend.plugins.yui.javascript.YuiType;

public class DragDropSettings extends AjaxSettings {

    private static final long serialVersionUID = 1L;

    private static final StringArraySetting GROUPS = new StringArraySetting("groups", "default");

    protected static final YuiType TYPE = new YuiType(AjaxSettings.TYPE, GROUPS);

    public DragDropSettings(IPluginConfig config) {
        this(TYPE, config);
    }

    public DragDropSettings(YuiType type, IPluginConfig config) {
        super(type, config);
    }

    public DragDropSettings setGroups(String... groups) {
        GROUPS.set(groups, this);
        return this;
    }
}
