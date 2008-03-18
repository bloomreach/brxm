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
package org.hippoecm.frontend.plugins.template.editor;

import javax.jcr.RepositoryException;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.lang.Bytes;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.PluginDescriptor;
import org.hippoecm.frontend.plugin.PluginFactory;
import org.hippoecm.frontend.plugin.empty.EmptyPlugin;
import org.hippoecm.frontend.template.TemplateDescriptor;
import org.hippoecm.frontend.template.TemplateEngine;
import org.hippoecm.frontend.template.TypeDescriptor;
import org.hippoecm.frontend.template.model.TemplateModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EditorForm extends Form {
    private static final long serialVersionUID = 1L;

    private static Logger log = LoggerFactory.getLogger(EditorForm.class);

    private Plugin plugin;
    private Plugin template;

    public EditorForm(String wicketId, JcrNodeModel model, EditorPlugin plugin) {
        super(wicketId, model);

        this.plugin = plugin;

        setMultiPart(true);
        // FIXME: make this configurable
        setMaxSize(Bytes.megabytes(5));

        add(template = createTemplate());
    }

    @Override
    public Component setModel(IModel model) {
        super.setModel(model);
        template.destroy();
        replace(template = createTemplate());
        return this;
    }

    protected Plugin createTemplate() {
        JcrNodeModel model = (JcrNodeModel) getModel();
        try {
            String type = model.getNode().getPrimaryNodeType().getName();
            TemplateEngine engine = plugin.getPluginManager().getTemplateEngine();
            TypeDescriptor typeDescriptor = engine.getTypeConfig().getTypeDescriptor(type);
            TemplateDescriptor templateDescriptor = engine.getTemplateConfig().getTemplate(typeDescriptor);
            if (templateDescriptor != null) {
                TemplateModel templateModel = new TemplateModel(templateDescriptor, model.getParentModel(),
                        model.getNode().getName(), model.getNode().getIndex());

                return engine.createTemplate("template", templateModel, plugin);
            } else {
                PluginDescriptor descriptor = new PluginDescriptor("template", EmptyPlugin.class.getName());
                return new PluginFactory(plugin.getPluginManager()).createPlugin(descriptor, null, plugin);
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
        return null;
    }

}
