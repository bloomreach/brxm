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
package org.hippoecm.frontend.plugins.admin.editor;

import javax.jcr.Node;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.nodetypes.JcrNodeTypesProvider;
import org.hippoecm.frontend.model.properties.JcrPropertiesProvider;
import org.hippoecm.frontend.plugin.JcrEvent;
import org.hippoecm.frontend.plugin.PluginEvent;

public class NodeEditor extends Form {
    private static final long serialVersionUID = 1L;
    
    private PropertiesEditor properties;
    private NodeTypesEditor types;

    public NodeEditor(String id, JcrNodeModel model) {
        super(id, model);
        setOutputMarkupId(true);
        
        properties = new PropertiesEditor("properties", new JcrPropertiesProvider(model));
        add(properties);

        types = new NodeTypesEditor("types", new JcrNodeTypesProvider(model)) {
            private static final long serialVersionUID = 1L;

            protected void onAddNodeType(String type) {
                try {
                    JcrNodeModel editorModel = (JcrNodeModel) NodeEditor.this.getModel();
                    Node node = editorModel.getNode();
                    node.addMixin(type);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            
            protected void onRemoveNodeType(String type) {
                try {
                    JcrNodeModel editorModel = (JcrNodeModel) NodeEditor.this.getModel();
                    Node node = editorModel.getNode();
                    node.removeMixin(type);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        add(types);
    }

    public void update(AjaxRequestTarget target, PluginEvent event) {
        JcrNodeModel newModel = event.getNodeModel(JcrEvent.NEW_MODEL);
        if (newModel != null) {
            properties.setProvider(new JcrPropertiesProvider(newModel));
            types.setProvider(new JcrNodeTypesProvider(newModel));
            setModel(newModel);
        }
        if (target != null && findPage() != null) {
            target.addComponent(this);
        }
    }

}
