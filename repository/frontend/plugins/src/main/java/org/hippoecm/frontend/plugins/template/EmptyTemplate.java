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

import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.PropertyModel;
import org.hippoecm.frontend.model.JcrItemModel;
import org.hippoecm.frontend.session.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EmptyTemplate extends Panel {
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(EmptyTemplate.class);

    private FieldModel fieldModel;
    private TemplateEngine engine;

    public EmptyTemplate(String wicketId, FieldModel fieldModel, TemplateEngine engine) {
        super(wicketId);

        this.fieldModel = fieldModel;
        this.engine = engine;

        add(new Label("name", new PropertyModel(fieldModel.getDescriptor(), "name")));
        add(new AjaxLink("add") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                EmptyTemplate.this.onAddNode(target);
            }
        });
    }

    public void onAddNode(AjaxRequestTarget target) {
        try {
            // get the path to the node
            JcrItemModel model = fieldModel.getItemModel();
            String path = model.getPath().substring(1);

            // get the parent
            Node node = ((UserSession) Session.get()).getRootNode();
            int pos = path.lastIndexOf('/');
            node = node.getNode(path.substring(0, pos));

            // create the node
            String type = fieldModel.getDescriptor().getType();
            node = node.addNode(path.substring(pos + 1), type);

            // create a new template and replace us with it
            if (getParent() != null) {
                getParent().replace(engine.createTemplate(getId(), fieldModel));
                if (target != null) {
                    target.addComponent(getParent());
                }
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
    }
}
