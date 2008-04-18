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
import org.hippoecm.frontend.core.PluginContext;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.service.IEditService;
import org.hippoecm.frontend.service.render.RenderService;

public class EditorService extends RenderService implements IEditService {
    private static final long serialVersionUID = 1L;

    private NodeEditor editor;

    @Override
    public void init(PluginContext context, String serviceId, String parentId, String wicketId, String modelId) {
        editor = new NodeEditor("editor", new JcrNodeModel("/"));
        add(editor);
        super.init(context, serviceId, parentId, wicketId, modelId);
    }

    @Override
    public void destroy() {
        remove(editor);
        super.destroy();
    }

    @Override
    public void onModelChanged() {
        super.onModelChanged();
        editor.setModel(getModel());
    }

    public void edit(IModel model) {
        setModel(model);
    }

}
