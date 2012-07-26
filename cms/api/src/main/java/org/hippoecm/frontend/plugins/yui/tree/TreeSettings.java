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

package org.hippoecm.frontend.plugins.yui.tree;

import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.yui.javascript.AjaxSettings;
import org.hippoecm.frontend.plugins.yui.javascript.BooleanSetting;
import org.hippoecm.frontend.plugins.yui.javascript.StringSetting;
import org.hippoecm.frontend.plugins.yui.javascript.YuiType;

/**
 * This class encapsulates various settings for {@link TreeBehavior}
 */

public final class TreeSettings extends AjaxSettings {

    private static final long serialVersionUID = 1L;

    private static final StringSetting TREE = new StringSetting("treeData", false);
    private static final StringSetting ROOT = new StringSetting("root", "/");
    private static final BooleanSetting REGISTER_ONCLICK = new BooleanSetting("registerOnclick", false);
    private static final BooleanSetting REGISTER_ONDBLCLICK = new BooleanSetting("registerOnDoubleclick", false);

    protected static final YuiType TYPE = new YuiType(AjaxSettings.TYPE, TREE, ROOT, REGISTER_ONCLICK, REGISTER_ONDBLCLICK);

    public TreeSettings(IPluginConfig config) {
        super(TYPE, config);
    }

    public void setTreeData(String data) {
        TREE.set(data, this);
    }
    
    public void setRegisterOnClick(boolean set) {
        REGISTER_ONCLICK.set(set, this);
    }
    
    public void setRegisterOnDoubleClick(boolean set) {
        REGISTER_ONDBLCLICK.set(set, this);
    }
    
    public void setRoot(String root) {
        ROOT.set(root, this);
    }
    
    public String getRoot() {
        return ROOT.get(this);
    }

}
