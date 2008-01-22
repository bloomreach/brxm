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

import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugins.template.config.TemplateDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EditorForm extends Form {
    private static final long serialVersionUID = 1L;

    private static Logger log = LoggerFactory.getLogger(EditorForm.class);

    private TemplateEngine engine;

    public EditorForm(String wicketId, JcrNodeModel model, TemplateEngine engine) {
        super(wicketId, model);
        this.engine = engine;

        add(createTemplate());
    }

    @Override
    public Component setModel(IModel model) {
        Component template = createTemplate();
        if (template != null) {
            replace(template);
        }
        return super.setModel(model);
    }

    protected Component createTemplate() {
        JcrNodeModel model = (JcrNodeModel) getModel();
        try {
            NodeType type = model.getNode().getPrimaryNodeType();
            TemplateDescriptor descriptor = engine.getConfig().getTemplate(type.getName());
            return engine.createTemplate("template", model, descriptor);
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
        return null;
    }

}
