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
package org.hippoecm.frontend.plugins.cms.dashboard;

import java.util.HashMap;
import java.util.Map;

import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.perspective.Perspective;
import org.hippoecm.frontend.plugins.yui.layout.YuiWireframeBehavior;

public class DashboardPerspective extends Perspective {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    public DashboardPerspective(IPluginContext context, IPluginConfig config) {
        super(context, config);
        
        YuiWireframeBehavior wireframe = new YuiWireframeBehavior("dashboard-perspective-wrapper", true);
        Map<String, String> optsLeft = new HashMap<String, String>();
        optsLeft.put("width", "50%");
        optsLeft.put("id", "lefthalf");
        optsLeft.put("resize", "true");
        optsLeft.put("scroll", "true");
        optsLeft.put("gutter", "0px 5px 0px 0px");
        wireframe.addUnit("left", optsLeft);
        
        wireframe.addUnit("top", "height=200", "id=welcomepanel", "resize=true", "gutter=0px 0px 5px 0px");
        wireframe.addUnit("center", "id=righthalf");
        add(wireframe);
    }
}
