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
package org.hippoecm.frontend.plugins.cms.root;

import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;

import org.hippoecm.frontend.plugin.ContextMenu;
import org.hippoecm.frontend.plugin.ContextMenuManager;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.render.RenderPlugin;

public class FooterPlugin extends RenderPlugin implements ContextMenuManager {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    public FooterPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        setOutputMarkupId(true);

        add(new AjaxEventBehavior("onclick") {
            public void onEvent(AjaxRequestTarget target) {
                collapse(null, target);
            }
        });
    }

    public void addContextMenu(ContextMenu activeMenu) {
        ContextMenuManager parent = (ContextMenuManager) findParent(ContextMenuManager.class);
        if(parent != null) {
            parent.addContextMenu(activeMenu);
        }
    }

    public void collapse(ContextMenu current, AjaxRequestTarget target) {
        ContextMenuManager parent = (ContextMenuManager) findParent(ContextMenuManager.class);
        if(parent != null) {
            parent.collapse(current, target);
        }
    }
}
