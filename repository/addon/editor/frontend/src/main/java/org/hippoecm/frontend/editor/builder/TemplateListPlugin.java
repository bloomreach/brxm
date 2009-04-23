/*
 *  Copyright 2008 Hippo.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.frontend.editor.builder;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AbstractBehavior;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.hippoecm.frontend.editor.ITemplateEngine;
import org.hippoecm.frontend.i18n.types.TypeTranslator;
import org.hippoecm.frontend.model.nodetypes.JcrNodeTypeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.types.IFieldDescriptor;
import org.hippoecm.frontend.types.ITypeDescriptor;
import org.hippoecm.frontend.types.JavaFieldDescriptor;
import org.hippoecm.frontend.widgets.AbstractView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TemplateListPlugin extends RenderPlugin {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(TemplateListPlugin.class);

    private List<ITypeDescriptor> templateList;

    public TemplateListPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        templateList = new LinkedList<ITypeDescriptor>();
        ITemplateEngine engine = context.getService(config.getString(ITemplateEngine.ENGINE), ITemplateEngine.class);
        if (engine != null) {
            String[] templates = config.getStringArray("templates");
            if (templates != null) {
                for (String type : templates) {
                    ITypeDescriptor typeDescriptor = engine.getType(type);
                    if (engine.getTemplate(typeDescriptor, ITemplateEngine.EDIT_MODE) != null) {
                        templateList.add(typeDescriptor);
                    }
                }
            } else {
                log.warn("No templates configured");
            }
        } else {
            log.error("No template engine found under {}", config.getString(ITemplateEngine.ENGINE));
        }

        AbstractView templateView = new AbstractView("templates", new ListDataProvider(templateList)) {
            private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item item) {
                final ITypeDescriptor type = (ITypeDescriptor) item.getModelObject();
                AjaxLink link = new AjaxLink("template") {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        ITypeDescriptor containingType = (ITypeDescriptor) TemplateListPlugin.this.getModelObject();
                        String prefix = containingType.getName();
                        if (prefix.indexOf(':') > 0) {
                            prefix = prefix.substring(0, prefix.indexOf(':'));
                            containingType.addField(new JavaFieldDescriptor(prefix, type.getName()));
                            // TODO: save!
                        } else {
                            log.warn("adding a field to a primitive type is not supported");
                        }
                    }

                    @Override
                    public boolean isEnabled() {
                        return "edit".equals(getPluginConfig().getString("mode"));
                    }
                };
                final String name = type.getName();
                link.add(new Label("template-name", new TypeTranslator(new JcrNodeTypeModel(name)).getTypeName()));
                item.add(link);

                item.add(new AbstractBehavior() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onComponentTag(final Component component, final ComponentTag tag) {
                        Object classes = tag.getUserData("class");
                        if (classes instanceof String) {
                            tag.put("class", (String) classes + " " + name.toLowerCase().replace(':', '_'));
                        } else {
                            tag.put("class", name.toLowerCase().replace(':', '_'));
                        }
                    }
                });
            }

            @Override
            public void destroyItem(Item item) {
                // nothing
            }
        };
        templateView.populate();
        add(templateView);
    }

}
