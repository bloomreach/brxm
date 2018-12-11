/*
 *  Copyright 2008-2018 Hippo B.V. (http://www.onehippo.com)
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

import org.apache.wicket.event.IEvent;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.ReadOnlyModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.list.resolvers.CssClass;
import org.hippoecm.frontend.plugins.yui.togglebehavior.ToggleBehavior;
import org.hippoecm.frontend.service.render.RenderPlugin;

public class EditorPlugin extends RenderPlugin<Node> {

    private final NodeEditor editor;
    private final Label message;

    public EditorPlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config);

        message = new Label("message", getString("node.checked.in.info"));
        add(message);

        editor = new NodeEditor("editor", getModel());
        add(editor);
        add(new ToggleBehavior());

        add(CssClass.append(ReadOnlyModel.of(() -> {
            final Node node = getModel().getObject();
            try {
                if (node != null && !node.isCheckedOut()) {
                    return "checked-in";
                }
            } catch (final RepositoryException ignore) {
            }
            return "";
        })));

        onModelChanged();
    }

    protected IPluginContext getPluginContext() {
        return super.getPluginContext();
    }

    @Override
    public void onModelChanged() {
        super.onModelChanged();
        final IModel<Node> newModel = getModel();
        try {
            final Node node = newModel.getObject();
            if (node != null) {
                final boolean nodeIsEditable = node.isCheckedOut();
                if (editor.isEnabled() != nodeIsEditable) {
                    editor.setEnabled(nodeIsEditable);
                    redraw();
                }

                if (message.isVisible() == nodeIsEditable) {
                    message.setVisible(!nodeIsEditable);
                    redraw();
                }
            }
        } catch (final RepositoryException e) {
            // ignore
        }

        if (!editor.getModel().equals(newModel)) {
            editor.setModel(newModel);
            redraw();
        }
    }


    @Override
    public void onEvent(final IEvent<?> event) {
        super.onEvent(event);

        if (event.getPayload() instanceof EditorUpdate)
        {
            redraw();

            final EditorUpdate update = (EditorUpdate) event.getPayload();
            update.getTarget().add(this);
        }
    }
}
