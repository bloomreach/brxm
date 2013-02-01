/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TemplateListPlugin extends RenderPlugin<ITypeDescriptor> {

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


    private final class CategoryView extends SectionView<ITypeDescriptor> {

        private CategoryView(String id, Category category) {
            super(id, category);
        }

        @Override
        public void populateItem(final Item<ITypeDescriptor> item) {
            super.populateItem(item);

            final String name = item.getModelObject().getName();
            item.add(new AbstractBehavior() {
                private static final long serialVersionUID = 1L;

                @Override
                public void onComponentTag(final Component component, final ComponentTag tag) {
                    Object classes = tag.getUserData("class");
                    if (classes instanceof String) {
                        tag.put("class", classes + " " + name.toLowerCase().replace(':', '_'));
                    } else {
                        tag.put("class", name.toLowerCase().replace(':', '_'));
                    }
                }
            });
        }

        @Override
        void onClickItem(final ITypeDescriptor type) {
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

                    // Check whether the mixin is already added to the containing type or not
                    if (!superTypes.contains(type.getName())) {
                        superTypes.add(type.getName());
                        containingType.setSuperTypes(superTypes);
                    }
                }
            } else {
                log.warn("adding a field to a primitive type is not supported");
            }
        }

        @Override
        IModel<String> getNameModel(final ITypeDescriptor type) {
            return new TypeTranslator(new JcrNodeTypeModel(type.getName())).getTypeName();
        }
    }

    static boolean hasPrefix(ITypeDescriptor descriptor, String prefix) {
        String typeName = descriptor.getName();

        if (typeName.indexOf(':') <= 0) {
            return false;
        }

        typeName = typeName.substring(0, typeName.indexOf(':'));
        return ((prefix.equals(typeName)) ? true : false);
    }

    abstract class CategorySection extends Section {
        final Category category;

        CategorySection(ITemplateEngine engine, List<String> editableTypes, String name) {
            this.category = new Category(engine, editableTypes, name) {
                @Override
                boolean isTypeInCategory(final ITypeDescriptor descriptor) {
                    return CategorySection.this.isTypeInCategory(descriptor);
                }
            };
        }

        abstract boolean isTypeInCategory(final ITypeDescriptor descriptor);

        public IModel<String> getTitleModel() {
            return new ResourceModel(category.getName());
        }

        public SectionView<?> createView(final String id) {
            SectionView<?> templateView = new CategoryView(id, category);
            templateView.populate();
            return templateView;
        }
    }

    private Section active;

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

            final List<Section> sections = new ArrayList<Section>(5);
            sections.add(new CategorySection(engine, editableTypes, "primitive") {
                private static final long serialVersionUID = 1L;

                @Override
                boolean isTypeInCategory(ITypeDescriptor descriptor) {
                    return ((!descriptor.isNode() && !descriptor.isMixin()) ? true : false);
                }
            });
            sections.add(new CategorySection(engine, editableTypes, "compound") {
                private static final long serialVersionUID = 1L;

                @Override
                boolean isTypeInCategory(ITypeDescriptor descriptor) {
                    return ((descriptor.isNode() && !hasPrefix(descriptor, prefix) && !descriptor.isMixin()) ? true : false);
                }
            });
            sections.add(new CategorySection(engine, editableTypes, "custom") {
                private static final long serialVersionUID = 1L;

                @Override
                boolean isTypeInCategory(ITypeDescriptor descriptor) {
                    return ((descriptor.isNode() && hasPrefix(descriptor, prefix) && !descriptor.isMixin()) ? true : false);
                }
            });
            sections.add(new CategorySection(engine, editableTypes, "mixins") {
                private static final long serialVersionUID = 1L;

                @Override
                boolean isTypeInCategory(ITypeDescriptor descriptor) {
                    return descriptor.isMixin();
                }
            });
            sections.add(new InheritedFieldSection(context, config));
            active = sections.get(0);

            fragment.add(new ListView<Section>("categories", sections) {
                private static final long serialVersionUID = 1L;

                @Override
                protected void populateItem(ListItem<Section> item) {
                    final Section section = item.getModelObject();

                    MarkupContainer container = new WebMarkupContainer("container") {
                        private static final long serialVersionUID = 1L;

                        @Override
                        public boolean isVisible() {
                            return active == section;
                        }

                    };

                    SectionView<?> templateView = section.createView("templates");
                    container.add(templateView);
                    item.add(container);

                    AjaxLink<Void> link = new AjaxLink<Void>("link") {
                        private static final long serialVersionUID = 1L;

                        @Override
                        public void onClick(AjaxRequestTarget target) {
                            active = section;
                            redraw();
                        }
                    };
                    link.add(new Label("category", section.getTitleModel()));
                    link.add(new CssClassAppender(new LoadableDetachableModel<String>() {
                        private static final long serialVersionUID = 1L;

                        @Override
                        protected String load() {
                            if (active == section) {
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
