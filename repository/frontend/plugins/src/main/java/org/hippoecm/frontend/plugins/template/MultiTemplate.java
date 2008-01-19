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
package org.hippoecm.frontend.plugins.template;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.hippoecm.frontend.model.JcrItemModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugins.template.config.FieldDescriptor;
import org.hippoecm.frontend.plugins.template.model.FieldModel;
import org.hippoecm.frontend.plugins.template.model.MultiProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MultiTemplate extends Panel {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(MultiTemplate.class);

    private MultiProvider provider;

    public MultiTemplate(String wicketId, FieldModel model, TemplateEngine engine) {
        super(wicketId, model);

        JcrItemModel itemModel = model.getItemModel();
        FieldDescriptor descriptor = model.getDescriptor();

        if (!descriptor.isNode()) {
            throw new IllegalArgumentException("Descriptor " + descriptor.getName() + " for " + descriptor.getPath() + "does not describe a node");
        }

        String name;
        if (descriptor != null) {
            name = descriptor.getName();
        } else {
            name = "no name";
        }
        add(new Label("name", name));

        provider = new MultiProvider(descriptor, new JcrNodeModel(itemModel));
        add(new MultiView("field", provider, engine, this));

        add(createAddLink());

        setOutputMarkupId(true);
    }

    @Override
    public void onDetach() {
        provider.detach();
        super.onDetach();
    }

    public void onAddNode(AjaxRequestTarget target) {
        FieldModel fieldModel = (FieldModel) getModel();
        try {
            // get the path to the node
            JcrItemModel model = fieldModel.getItemModel();
            FieldDescriptor descriptor = fieldModel.getDescriptor();

                // create the node
            String type = fieldModel.getDescriptor().getType();
            Node parent = (Node) model.getObject();
            if (parent != null) {
                parent.addNode(descriptor.getPath(), type);
            } else {
                log.error("parent " + model.getPath() + " does not exist");
            }

            // refresh
            provider.detach();
            replace(createAddLink());

            if (target != null) {
                target.addComponent(this);
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
    }

    public void onRemoveNode(FieldModel childModel, AjaxRequestTarget target) {
        try {
            JcrItemModel model = childModel.getChildModel();

            if (model.exists()) {
                Node child = (Node) model.getObject();

                // remove the item
                log.info("removing item " + model.getPath());
                child.remove();
            } else {
                log.error("item " + model.getPath() + " does not exist");
            }

            // refresh
            provider.detach();
            replace(createAddLink());

            if (target != null) {
                target.addComponent(this);
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
    }

    protected Component createAddLink() {
        FieldModel model = (FieldModel) getModel();
        if (model.getDescriptor().isMultiple() || (provider.size() == 0)) {
            return new AjaxLink("add") {
                private static final long serialVersionUID = 1L;

                @Override
                public void onClick(AjaxRequestTarget target) {
                    MultiTemplate.this.onAddNode(target);
                }
            };
        } else {
            return new Label("add", "");
        }
    }
}
