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
package org.hippoecm.frontend.plugins.console.editor;

import javax.jcr.Node;

import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.yui.togglebehavior.ToggleBehavior;
import org.hippoecm.frontend.service.render.RenderPlugin;

public class EditorPlugin extends RenderPlugin<Node> {

    private static final long serialVersionUID = 1L;

    private final NodeEditor editor;

    public EditorPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        editor = new NodeEditor("editor", getModel());
        add(editor);
        add(new ToggleBehavior());
    }

    @Override
    public void onModelChanged() {
        super.onModelChanged();
        editor.setModel(getModel());
        redraw();
    }
}
