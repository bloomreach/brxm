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
package org.hippoecm.frontend.plugins.template;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.apache.jackrabbit.value.StringValue;
import org.apache.wicket.Component;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.hippoecm.frontend.model.JcrItemModel;
import org.hippoecm.frontend.model.properties.JcrPropertyModel;
import org.hippoecm.frontend.session.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ValueTemplate extends Panel {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(ValueTemplate.class);

    public ValueTemplate(String wicketId, JcrPropertyModel model, FieldDescriptor descriptor, TemplateEngine engine) {
        super(wicketId, model);

        if (!descriptor.isProtected()) {
            if (model.getObject() != null) {
                add(deleteLink("delete", model));
            } else {
                add(new Label("delete", ""));
            }
        } else {
            add(new Label("delete", "(protected)"));
        }
        add(new Label("name", descriptor.getName()));
        add(new ValueEditor("values", model, descriptor, engine));
        if (descriptor.isMultiple() || model.getObject() == null) {
            add(addLink("add", model));
        } else {
            add(new Label("add", ""));
        }
    }

    // Called when a new value is added to a multi-valued property

    protected void onAddValue(AjaxRequestTarget target) {
        JcrPropertyModel model = (JcrPropertyModel) getModel();
        try {
            Property prop = model.getProperty();
            if (prop != null) {
                Value[] oldValues = prop.getValues();
                Value[] newValues = new Value[oldValues.length + 1];
                for (int i = 0; i < oldValues.length; i++) {
                    newValues[i] = oldValues[i];
                }
                newValues[oldValues.length] = new StringValue("...");
                prop.setValue(newValues);
            } else {
                Value value = new StringValue("");

                // get the path to the node
                JcrItemModel itemModel = model.getItemModel();
                String path = itemModel.getPath().substring(1);

                // get the parent
                Node node = ((UserSession) Session.get()).getRootNode();
                int pos = path.lastIndexOf('/');
                node = node.getNode(path.substring(0, pos));
                node.setProperty(path.substring(pos + 1), value);
            }
            Component template = findParent(Template.class);
            if (target != null && template != null) {
                target.addComponent(template);
            }
        } catch (RepositoryException e) {
            log.error(e.getMessage());
        }
    }

    protected void onDeleteValue(AjaxRequestTarget target) {
        JcrPropertyModel model = (JcrPropertyModel) getModel();
        try {
            Property prop = model.getProperty();
            prop.remove();

            Component template = findParent(Template.class);
            if (target != null && template != null) {
                target.addComponent(template);
            }
        } catch (RepositoryException e) {
            log.error(e.getMessage());
        }
    }

    // privates

    private Component deleteLink(String id, final JcrPropertyModel model) {
        return new AjaxLink(id, model) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                ValueTemplate.this.onDeleteValue(target);
            }
        };
    }

    private AjaxLink addLink(String id, final JcrPropertyModel model) {
        return new AjaxLink(id, model) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                ValueTemplate.this.onAddValue(target);
            }
        };
    }
}
