/*
 *  Copyright 2008-2017 Hippo B.V. (http://www.onehippo.com)
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
import javax.jcr.RepositoryException;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.yui.togglebehavior.ToggleBehavior;
import org.hippoecm.frontend.service.render.RenderPlugin;

public class EditorPlugin extends RenderPlugin<Node> {
    private static final long serialVersionUID = 1L;

    private final NodeEditor editor;
    private Label message;

    public EditorPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        message = new Label("message", getString("node.checked.in.info"));
        add(message);

        editor = new NodeEditor("editor", getModel());
        add(editor);
        add(new ToggleBehavior());

        onModelChanged();
    }

    protected IPluginContext getPluginContext() {
        return super.getPluginContext();
    }

    @Override
    public void onModelChanged() {
        super.onModelChanged();
        IModel<Node> newModel = getModel();
        try {
            final Node node = newModel.getObject();
            if (node != null) {
                if (node.isCheckedOut()) {
                    add(new AttributeModifier("style", ""));
                } else {
                    add(new AttributeModifier("style", "background-color:#ddd;"));
                }
                message.setVisible(!node.isCheckedOut());
                editor.setEnabled(node.isCheckedOut());
                redraw();
            }

        } catch (RepositoryException e) {
            // ignore
        }
        if (!editor.getModel().equals(newModel)) {
            editor.setModel(newModel);
            redraw();
        }
    }


    @Override
    public void onEvent(IEvent<?> event) {
        super.onEvent(event);

        if (event.getPayload() instanceof EditorUpdate)
        {
            redraw();
            
            EditorUpdate update = (EditorUpdate) event.getPayload();
            update.getTarget().add(this);
        }
    }
}
