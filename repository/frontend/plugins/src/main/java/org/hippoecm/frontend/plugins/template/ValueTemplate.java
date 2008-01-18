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

import org.apache.commons.lang.ArrayUtils;
import org.apache.jackrabbit.value.StringValue;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.hippoecm.frontend.model.JcrItemModel;
import org.hippoecm.frontend.model.properties.JcrPropertyModel;
import org.hippoecm.frontend.model.properties.JcrPropertyValueModel;
import org.hippoecm.frontend.plugins.template.config.FieldDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ValueTemplate extends Panel {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(ValueTemplate.class);

    private FieldDescriptor descriptor;

    public ValueTemplate(String wicketId, JcrPropertyModel model,
            FieldDescriptor descriptor, TemplateEngine engine) {
        super(wicketId, model);

        this.descriptor = descriptor;

        add(createAddLink(model));
        add(createDeleteLink(model));
        add(new Label("name", descriptor.getName()));
        add(new ValueView("values", model, descriptor, engine));

        setOutputMarkupId(true);
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
                Node parent = (Node) itemModel.getParentModel().getObject();
                parent.setProperty(descriptor.getPath(), value);

                // use a fresh model; the property has to be re-retrieved
                model.detach();
            }

            // update labels/links
            replace(createAddLink(model));
            replace(createDeleteLink(model));

            if (target != null) {
                target.addComponent(this);
            }
        } catch (RepositoryException e) {
            log.error(e.getMessage());
        }
    }

    protected void onRemoveValue(AjaxRequestTarget target,
            JcrPropertyValueModel model) {
        try {
            Property prop = model.getJcrPropertymodel().getProperty();
            Value[] values = prop.getValues();
            values = (Value[]) ArrayUtils.remove(values, model.getIndex());
            prop.setValue(values);

            if (target != null) {
                target.addComponent(this);
            }
        } catch (RepositoryException e) {
            log.error(e.getMessage());
        }
    }

    protected void onDeleteProperty(AjaxRequestTarget target) {
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

    private Component createDeleteLink(final JcrPropertyModel model) {
        String id = "delete";
        if (!descriptor.isProtected()) {
            if (model.getItemModel().exists()) {
                return new AjaxLink(id, model) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        ValueTemplate.this.onDeleteProperty(target);
                    }
                };
            } else {
                return new Label(id, "");
            }
        } else {
            return new Label("delete", "(protected)");
        }
    }

    private Component createAddLink(final JcrPropertyModel model) {
        String id = "add";
        if (descriptor.isMultiple() || !model.getItemModel().exists()) {
            return new AjaxLink(id, model) {
                private static final long serialVersionUID = 1L;

                @Override
                public void onClick(AjaxRequestTarget target) {
                    ValueTemplate.this.onAddValue(target);
                }
            };
        } else {
            return new Label("add", "");
        }
    }
}
