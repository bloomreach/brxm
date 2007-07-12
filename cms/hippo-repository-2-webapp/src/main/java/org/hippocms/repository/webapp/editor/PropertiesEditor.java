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
package org.hippocms.repository.webapp.editor;

import javax.jcr.Property;
import javax.jcr.RepositoryException;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.ReuseIfModelsEqualStrategy;
import org.apache.wicket.markup.repeater.data.DataView;
import org.hippocms.repository.webapp.model.JcrNodeModel;
import org.hippocms.repository.webapp.model.JcrPropertyModel;

public class PropertiesEditor extends DataView {
    private static final long serialVersionUID = 1L;

    public PropertiesEditor(String id, JcrNodeModel dataProvider) {
        super(id, dataProvider);
        setItemReuseStrategy(ReuseIfModelsEqualStrategy.getInstance());
    }

    protected void populateItem(Item item) {
        JcrPropertyModel model = (JcrPropertyModel) item.getModel();
        if (model.getProperty() != null) {
            try {
                item.add(deleteLink("delete", model));
                item.add(new Label("name", model.getProperty().getName()));
                item.add(new PropertyValueEditor("values", model));
            } catch (RepositoryException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        } else {
            item.add(new Label("delete", "null"));
            item.add(new Label("name", "null"));
            item.add(new Label("values", "null"));            
        }
    }

    // privates

    private Component deleteLink(String id, final JcrPropertyModel model) throws RepositoryException {
        Component result = null;
        if (model.getProperty().getDefinition().isProtected()) {
            result = new Label(id, "(protected)");

        } else {
            result = new AjaxLink(id, model) {
                private static final long serialVersionUID = 1L;
                public void onClick(AjaxRequestTarget target) {
                    try {
                        Property prop = model.getProperty();
                        prop.remove();
                    } catch (RepositoryException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    NodeEditor editor = (NodeEditor) findParent(NodeEditor.class);
                    target.addComponent(editor);
                }
            };
        }
        return result;
    }

}
