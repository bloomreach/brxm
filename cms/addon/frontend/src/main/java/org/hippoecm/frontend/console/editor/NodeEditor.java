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

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.nodetypes.JcrNodeTypesProvider;
import org.hippoecm.frontend.model.properties.JcrPropertiesProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NodeEditor extends Form {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(NodeEditor.class);

    @SuppressWarnings("unused")
    private String primaryType;
    private PropertiesEditor properties;
    private NodeTypesEditor types;

    public NodeEditor(String id, JcrNodeModel model) {
        super(id, model);
        setOutputMarkupId(true);

        try {
            primaryType = model.getNode().getPrimaryNodeType().getName();
            add(new Label("primarytype", new PropertyModel(this, "primaryType")));
        } catch (RepositoryException e) {
            log.error(e.getMessage());
            add(new Label("primarytype", e.getMessage()));
        }

        properties = new PropertiesEditor("properties", new JcrPropertiesProvider(model));
        add(properties);

        types = new NodeTypesEditor("mixintypes", new JcrNodeTypesProvider(model)) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onAddNodeType(String type) {
                try {
                    JcrNodeModel editorModel = (JcrNodeModel) NodeEditor.this.getModel();
                    Node node = editorModel.getNode();
                    node.addMixin(type);
                } catch (Exception e) {
                    log.error(e.getMessage());
                }
            }

            @Override
            protected void onRemoveNodeType(String type) {
                try {
                    JcrNodeModel editorModel = (JcrNodeModel) NodeEditor.this.getModel();
                    Node node = editorModel.getNode();
                    node.removeMixin(type);
                } catch (Exception e) {
                    log.error(e.getMessage());
                }
            }
        };
        add(types);
    }

    @Override
    public Component setModel(IModel model) {
        JcrNodeModel newModel = (JcrNodeModel) model;
        properties.setProvider(new JcrPropertiesProvider(newModel));
        types.setProvider(new JcrNodeTypesProvider(newModel));
        try {
            primaryType = newModel.getNode().getPrimaryNodeType().getName();
        } catch (RepositoryException e) {
            log.error(e.getMessage());
        }
        return super.setModel(newModel);
    }
}
