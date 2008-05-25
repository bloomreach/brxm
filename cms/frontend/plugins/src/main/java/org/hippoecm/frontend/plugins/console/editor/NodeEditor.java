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
package org.hippoecm.frontend.plugins.console.editor;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.repeater.data.EmptyDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.properties.JcrPropertiesProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NodeEditor extends Form {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(NodeEditor.class);

    @SuppressWarnings("unused")
    private String primaryType;
    private String mixinTypes;
    private PropertiesEditor propertiesEditor;
    private NodeTypesEditor typesEditor;

    public NodeEditor(String id) {
        super(id);
        setOutputMarkupId(true);

        add(new Label("primarytype", new PropertyModel(this, "primaryType")));
        add(new Label("types", new PropertyModel(this, "mixinTypes")));

        propertiesEditor = new PropertiesEditor("properties", new EmptyDataProvider());
        add(propertiesEditor);

        typesEditor = new NodeTypesEditor("mixintypes", new ArrayList<String>(), null);
        add(typesEditor);
    }

    @Override
    public void onModelChanged() {
        super.onModelChanged();
        IModel model = getModel();
        if (model instanceof JcrNodeModel) {
            try {
                JcrNodeModel newModel = (JcrNodeModel) model;

                replace(new PropertiesEditor("properties", new JcrPropertiesProvider(newModel)));

                List<String> result = new ArrayList<String>();
                NodeType[] nodeTypes = newModel.getNode().getMixinNodeTypes();
                for (NodeType nodeType : nodeTypes) {
                    result.add(nodeType.getName());
                }
                typesEditor.setModelObject(result);
                typesEditor.setNodeModel(newModel);

                primaryType = newModel.getNode().getPrimaryNodeType().getName();
                mixinTypes = new String();
                for (NodeType type : nodeTypes) {
                    mixinTypes += type.getName() + ", ";
                }
                mixinTypes = StringUtils.substringBeforeLast(mixinTypes, ",");

            } catch (RepositoryException e) {
                log.error(e.getMessage());
            }
        }
    }
}
