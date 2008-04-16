/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.frontend.console.editor;

import org.hippoecm.frontend.console.RenderPlugin;
import org.hippoecm.frontend.core.PluginContext;
import org.hippoecm.frontend.model.JcrNodeModel;

public class EditorPlugin extends RenderPlugin {
    private static final long serialVersionUID = 1L;

    private NodeEditor editor;

    @Override
    public void init(PluginContext context, String wicketId) {
        super.init(context, wicketId);
        editor = new NodeEditor("editor", new JcrNodeModel("/"));
        add(editor);
    }

    @Override
    public void destroy(PluginContext context) {
        remove(editor);
        super.destroy(context);
    }

    @Override
    public void onModelChanged() {
        super.onModelChanged();
        editor.setModel(getModel());
    }
}
