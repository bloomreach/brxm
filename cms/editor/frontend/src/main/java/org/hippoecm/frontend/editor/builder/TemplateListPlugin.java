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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AbstractBehavior;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.hippoecm.frontend.editor.ITemplateEngine;
import org.hippoecm.frontend.editor.TemplateEngineException;
import org.hippoecm.frontend.i18n.types.TypeTranslator;
import org.hippoecm.frontend.model.event.IEvent;
import org.hippoecm.frontend.model.event.IObservable;
import org.hippoecm.frontend.model.event.IObserver;
import org.hippoecm.frontend.model.nodetypes.JcrNodeTypeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.list.resolvers.CssClassAppender;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.types.ITypeDescriptor;
import org.hippoecm.frontend.types.JavaFieldDescriptor;
import org.hippoecm.frontend.types.TypeException;
import org.hippoecm.frontend.widgets.AbstractView;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TemplateListPlugin extends RenderPlugin<ITypeDescriptor> {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(TemplateListPlugin.class);

    abstract class Category implements IDataProvider<ITypeDescriptor> {
        private static final long serialVersionUID = 1L;

        private final ITemplateEngine engine;
        private final List<String> editableTypes;
        private List<ITypeDescriptor> list;
        private String name;

        Category(ITemplateEngine engine, List<String> editableTypes, String name) {
            this.engine = engine;
            this.editableTypes = editableTypes;
            this.name = name;
        }

        void load() {
            if (list == null) {
                ITypeDescriptor containingType = TemplateListPlugin.this.getModelObject();
                SortedMap<String, ITypeDescriptor> types = new TreeMap<String, ITypeDescriptor>();
                for (String type : editableTypes) {
                    try {
                        ITypeDescriptor descriptor = engine.getType(type);
                        if (containingType.getName().equals(type)) {
                            continue;
                        }
                        if (descriptor.isType(HippoNodeType.NT_DOCUMENT)) {
                            continue;
                        }
                        if (descriptor.isType(HippoNodeType.NT_TEMPLATETYPE)) {
                            continue;
                        }
                        if (isTypeInCategory(descriptor)) {
                            TypeTranslator translator = new TypeTranslator(new JcrNodeTypeModel(descriptor.getName()));
                            String name = (String) translator.getTypeName().getObject();
                            types.put(name, descriptor);
                        }
                    } catch (TemplateEngineException ex) {
                        log.error("Failed to obtain descriptor for " + type);
                    }
                }
                list = new ArrayList<ITypeDescriptor>(types.values());
            }
        }

        abstract boolean isTypeInCategory(ITypeDescriptor descriptor);

        public Iterator<ITypeDescriptor> iterator(int first, int count) {
            load();
            int toIndex = first + count;
            if (toIndex > list.size()) {
                toIndex = list.size();
            }
            return list.subList(first, toIndex).listIterator();
        }

        public IModel<ITypeDescriptor> model(ITypeDescriptor object) {
            return new Model<ITypeDescriptor>(object);
        }

        public int size() {
            load();
            return list.size();
        }

        public void detach() {
            list = null;
        }

        public String getName() {
            return name;
        }
    }

    private final class CategoryView extends AbstractView<ITypeDescriptor> {
        private static final long serialVersionUID = 1L;

        private CategoryView(String id, Category category) {
            super(id, category);
        }

        @Override
        public void populateItem(Item<ITypeDescriptor> item) {
            final ITypeDescriptor type = item.getModelObject();
            AjaxLink<Void> link = new AjaxLink<Void>("template") {
                private static final long serialVersionUID = 1L;

                @Override
                public void onClick(AjaxRequestTarget target) {
                    ITypeDescriptor containingType = TemplateListPlugin.this.getModelObject();
                    String prefix = containingType.getName();
                    if (prefix.indexOf(':') > 0) {
                        prefix = prefix.substring(0, prefix.indexOf(':'));
                        if (!type.isMixin()) {
                            try {
                                containingType.addField(new JavaFieldDescriptor(prefix, type));
                            } catch (TypeException e) {
                                TemplateListPlugin.this.error(e.getLocalizedMessage());
                            }
                        } else {
                            List<String> superTypes = containingType.getSuperTypes();
                            superTypes.add(type.getName());
                            containingType.setSuperTypes(superTypes);
                        }
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
        public void destroyItem(Item<ITypeDescriptor> item) {
            // nothing
        }
    }

    static boolean hasPrefix(ITypeDescriptor descriptor, String prefix) {
        String typeName = descriptor.getName();
        if (typeName.indexOf(':') <= 0) {
            return false;
        }
        typeName = typeName.substring(0, typeName.indexOf(':'));
        if (prefix.equals(typeName)) {
            return true;
        }
        return false;
    }

    private Category active;
    private List<Category> categories;

    public TemplateListPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        String mode = getPluginConfig().getString("mode", "view");
        Fragment fragment = new Fragment("fragment", mode, this);
        add(fragment);

        if ("edit".equals(mode)) {
            final ITemplateEngine engine = context.getService(config.getString(ITemplateEngine.ENGINE),
                    ITemplateEngine.class);
            final List<String> editableTypes = engine.getEditableTypes();
            if (editableTypes instanceof IObservable) {
                context.registerService(new IObserver<IObservable>() {
                    private static final long serialVersionUID = 1L;

                    public IObservable getObservable() {
                        return (IObservable) editableTypes;
                    }

                    public void onEvent(Iterator<? extends IEvent<IObservable>> events) {
                        redraw();
                    }

                }, IObserver.class.getName());
            }

            ITypeDescriptor containingType = TemplateListPlugin.this.getModelObject();
            String typeName = containingType.getName();
            final String prefix = typeName.substring(0, typeName.indexOf(':'));

            categories = new ArrayList<Category>(3);
            categories.add(new Category(engine, editableTypes, "primitive") {
                private static final long serialVersionUID = 1L;

                @Override
                boolean isTypeInCategory(ITypeDescriptor descriptor) {
                    if (!descriptor.isNode() && !descriptor.isMixin()) {
                        return true;
                    }
                    return false;
                }
            });
            categories.add(new Category(engine, editableTypes, "compound") {
                private static final long serialVersionUID = 1L;

                @Override
                boolean isTypeInCategory(ITypeDescriptor descriptor) {
                    if (descriptor.isNode() && !hasPrefix(descriptor, prefix) && !descriptor.isMixin()) {
                        return true;
                    }
                    return false;
                }
            });
            categories.add(new Category(engine, editableTypes, "custom") {
                private static final long serialVersionUID = 1L;

                @Override
                boolean isTypeInCategory(ITypeDescriptor descriptor) {
                    if (descriptor.isNode() && hasPrefix(descriptor, prefix) && !descriptor.isMixin()) {
                        return true;
                    }
                    return false;
                }
            });
            categories.add(new Category(engine, editableTypes, "mixins") {
                private static final long serialVersionUID = 1L;

                @Override
                boolean isTypeInCategory(ITypeDescriptor descriptor) {
                    return descriptor.isMixin();
                }
            });
            active = categories.get(0);

            fragment.add(new ListView<Category>("categories", categories) {
                private static final long serialVersionUID = 1L;

                @Override
                protected void populateItem(ListItem<Category> item) {
                    final Category category = item.getModelObject();

                    AbstractView<ITypeDescriptor> templateView = new CategoryView("templates", category);
                    templateView.populate();
                    MarkupContainer container = new WebMarkupContainer("container") {
                        private static final long serialVersionUID = 1L;

                        @Override
                        public boolean isVisible() {
                            return active == category;
                        }

                    };
                    container.add(templateView);
                    item.add(container);

                    AjaxLink<Void> link = new AjaxLink<Void>("link") {
                        private static final long serialVersionUID = 1L;

                        @Override
                        public void onClick(AjaxRequestTarget target) {
                            active = category;
                            redraw();
                        }
                    };
                    link.add(new Label("category", new ResourceModel(category.getName())));
                    link.add(new CssClassAppender(new LoadableDetachableModel<String>() {
                        private static final long serialVersionUID = 1L;

                        @Override
                        protected String load() {
                            if (active == category) {
                                return "focus";
                            }
                            return "";
                        }
                    }));
                    item.add(link);
                }

            });

        }
    }

}
