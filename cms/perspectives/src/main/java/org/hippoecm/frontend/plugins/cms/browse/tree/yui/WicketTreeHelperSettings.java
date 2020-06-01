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

package org.hippoecm.frontend.plugins.cms.browse.tree.yui;

import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.yui.javascript.BooleanSetting;
import org.hippoecm.frontend.plugins.yui.javascript.StringSetting;
import org.hippoecm.frontend.plugins.yui.javascript.YuiObject;
import org.hippoecm.frontend.plugins.yui.javascript.YuiType;

public class WicketTreeHelperSettings extends YuiObject {
    private static final long serialVersionUID = 1L;


    protected static final BooleanSetting TREE_AUTOWIDTH = new BooleanSetting("treeAutowidth", true);
    protected static final StringSetting SET_WIDTH_TO_CLASSNAME = new StringSetting("setWidthToClassname", "hippo-tree");
    protected static final StringSetting USE_WIDTH_FROM_CLASSNAME = new StringSetting("useWidthFromClassname");
    protected static final BooleanSetting BIND_TO_LAYOUT_UNIT = new BooleanSetting("bindToLayoutUnit", true);
    
    protected static final BooleanSetting WORKFLOW_ENABLED = new BooleanSetting("workflowEnabled", true);

    protected static final YuiType TYPE = new YuiType(TREE_AUTOWIDTH, SET_WIDTH_TO_CLASSNAME,
            USE_WIDTH_FROM_CLASSNAME, BIND_TO_LAYOUT_UNIT, WORKFLOW_ENABLED);

    public WicketTreeHelperSettings(IPluginConfig config) {
        super(TYPE, config);
    }

    public boolean isTreeAutowidth() {
        return TREE_AUTOWIDTH.get(this);
    }

}
