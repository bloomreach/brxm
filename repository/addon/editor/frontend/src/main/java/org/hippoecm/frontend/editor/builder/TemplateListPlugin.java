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

import java.io.Serializable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AbstractBehavior;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.editor.ITemplateEngine;
import org.hippoecm.frontend.editor.TemplateEngineException;
import org.hippoecm.frontend.i18n.types.TypeTranslator;
import org.hippoecm.frontend.model.event.IEvent;
import org.hippoecm.frontend.model.event.IObservable;
import org.hippoecm.frontend.model.event.IObserver;
import org.hippoecm.frontend.model.nodetypes.JcrNodeTypeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.render.RenderPlugin;
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

    private IDataProvider templateProvider;

    public TemplateListPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        final ITemplateEngine engine = context.getService(config.getString(ITemplateEngine.ENGINE), ITemplateEngine.class);
        final List<String> editableTypes = engine.getEditableTypes();
        if (editableTypes instanceof IObservable) {
            context.registerService(new IObserver() {
                private static final long serialVersionUID = 1L;

                public IObservable getObservable() {
                    return (IObservable) editableTypes;
                }

                public void onEvent(Iterator<? extends IEvent> events) {
                    redraw();
                }
                
            }, IObserver.class.getName());
        }
        templateProvider = new IDataProvider() {
            private static final long serialVersionUID = 1L;

            private List<ITypeDescriptor> list;

            void load() {
                if (list == null) {
                    list = new LinkedList<ITypeDescriptor>();
                    for (String type : editableTypes) {
                        try {
                            ITypeDescriptor descriptor = engine.getType(type);
                            if (descriptor.isType("hippo:document")) {
                                continue;
                            }
                            if (descriptor.isType("hippo:templatetype")) {
                                continue;
                            }
                            list.add(descriptor);
                        } catch (TemplateEngineException ex) {
                            log.error("Failed to obtain descriptor for " + type);
                        }
                    }
                }
            }

            public Iterator iterator(int first, int count) {
                load();
                int toIndex = first + count;
                if (toIndex > list.size())
                {
                    toIndex = list.size();
                }
                return list.subList(first, toIndex).listIterator();
            }

            public IModel model(Object object) {
                return new Model((Serializable) object);
            }

            public int size() {
                load();
                return list.size();
            }

            public void detach() {
                list = null;
            }
            
        };
        String mode = getPluginConfig().getString("mode", "view");
        Fragment fragment = new Fragment("fragment", mode, this);
        add(fragment);

        if("edit".equals(mode)) {
            AbstractView templateView = new AbstractView("templates", templateProvider) {
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
                            } else {
                                log.warn("adding a field to a primitive type is not supported");
                            }
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
            fragment.add(templateView);
        }
    }

}
