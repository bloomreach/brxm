/*
 * Copyright 2008 Hippo
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
package org.hippoecm.frontend.service.editor;

import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.sa.plugin.IPluginContext;
import org.hippoecm.frontend.sa.plugin.config.IPluginConfig;
import org.hippoecm.frontend.sa.service.IViewService;
import org.hippoecm.frontend.sa.service.render.RenderService;

public class EditorService extends RenderService implements IViewService {
    private static final long serialVersionUID = 1L;

    private NodeEditor editor;

    public EditorService(IPluginContext context, IPluginConfig properties) {
        super(context, properties);

        editor = new NodeEditor("editor", (JcrNodeModel) getModel());
        add(editor);
    }

    @Override
    public void onModelChanged() {
        super.onModelChanged();
        editor.setModel(getModel());
    }

    public void view(IModel model) {
        setModel(model);
    }

}
