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
package org.hippoecm.frontend.editor.plugins.field;

import java.util.Iterator;

import javax.jcr.RepositoryException;

import org.apache.wicket.Session;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.editor.ITemplateEngine;
import org.hippoecm.frontend.editor.TemplateEngineException;
import org.hippoecm.frontend.editor.builder.FieldEditor;
import org.hippoecm.frontend.editor.builder.IBuilderListener;
import org.hippoecm.frontend.editor.builder.IEditorContext;
import org.hippoecm.frontend.editor.builder.RenderPluginEditorPlugin;
import org.hippoecm.frontend.model.IModelReference;
import org.hippoecm.frontend.model.event.IEvent;
import org.hippoecm.frontend.model.event.IObservable;
import org.hippoecm.frontend.model.event.IObserver;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JavaPluginConfig;
import org.hippoecm.frontend.service.render.RenderService;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.types.IFieldDescriptor;
import org.hippoecm.frontend.types.ITypeDescriptor;
import org.hippoecm.frontend.types.TypeDescriptorEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FieldPluginEditorPlugin extends RenderPluginEditorPlugin {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(FieldPluginEditorPlugin.class);

    private static final String TEMPLATE_PARAMETER_EDITOR = "templateParameterEditor";
    private static final String FIELD_PLUGIN_EDITOR = "fieldPluginEditor";
    private static final String FIELD_EDITOR = "fieldEditor";

    class PropertyEditor extends RenderService {
        private static final long serialVersionUID = 1L;

        private ITypeDescriptor type;
        private IPluginConfig edited;
        private IModel fieldModel;
        private boolean shown = true;

        public PropertyEditor(IPluginContext context, IPluginConfig properties, IPluginConfig edited,
                ITypeDescriptor type, boolean edit) throws TemplateEngineException {
            super(context, properties);

            this.type = type;
            this.edited = edited;
            context.registerService(new IObserver() {
                private static final long serialVersionUID = 1L;

                public IObservable getObservable() {
                    return PropertyEditor.this.edited;
                }

                public void onEvent(Iterator<? extends IEvent> events) {
                    redraw();
                }

            }, IObserver.class.getName());

            fieldModel = new LoadableDetachableModel() {
                private static final long serialVersionUID = 1L;

                @Override
                protected Object load() {
                    if (PropertyEditor.this.type != null) {
                        return PropertyEditor.this.type.getField(PropertyEditor.this.edited.getString("field"));
                    }
                    return null;
                }

            };
            add(new FieldEditor(FIELD_EDITOR, type, fieldModel, edit));
            add(new FieldPluginEditor(FIELD_PLUGIN_EDITOR, new Model(edited), edit));

            context.registerService(new IObserver() {

                public IObservable getObservable() {
                    return PropertyEditor.this.type;
                }

                public void onEvent(Iterator<? extends IEvent> events) {
                    while (events.hasNext()) {
                        IEvent event = events.next();
                        if (event instanceof TypeDescriptorEvent) {
                            TypeDescriptorEvent tde = (TypeDescriptorEvent) event;
                            IFieldDescriptor field = tde.getField();
                            switch (tde.getType()) {
                            case FIELD_CHANGED:
                                if (field.equals(PropertyEditor.this.edited.getString("field"))) {
                                    PropertyEditor.this.redraw();
                                    FieldPluginEditorPlugin.this.updatePreview();
                                    break;
                                }
                            }
                        }
                    }
                }

            }, IObserver.class.getName());
            add(new EmptyPanel(TEMPLATE_PARAMETER_EDITOR));
        }

        @Override
        protected void onBeforeRender() {
            IFieldDescriptor descriptor = (IFieldDescriptor) fieldModel.getObject();
            // Field editor
            Panel panel = new EmptyPanel(TEMPLATE_PARAMETER_EDITOR);
            if (descriptor != null) {
                String subType = descriptor.getType();
                ITemplateEngine engine = getTemplateEngine();
                try {
                    IClusterConfig target = engine.getTemplate(engine.getType(subType), "edit");
                    panel = new TemplateParameterEditor(TEMPLATE_PARAMETER_EDITOR, getClusterParameters(edit), target,
                            edit);
                } catch (TemplateEngineException e) {
                    log.error("engine exception when rendering template parameter editor", e);
                }
            }
            replace(panel);
            super.onBeforeRender();
        }

        void show(boolean show) {
            if (show != shown) {
                if (shown) {
                    getPluginContext().unregisterService(this, getPluginConfig().getString("wicket.id"));
                } else {
                    getPluginContext().registerService(this, getPluginConfig().getString("wicket.id"));
                }
                shown = show;
            }
        }

        private IModel getClusterParameters(boolean edit) {
            if (edit && edited.getPluginConfig("cluster.options") == null) {
                edited.put("cluster.options", new JavaPluginConfig());
                try {
                    ((UserSession) Session.get()).getJcrSession().save();
                } catch (RepositoryException ex) {
                    log.error("failed to add child node to plugin config", ex);
                }
            }
            return new Model(edited.getPluginConfig("cluster.options"));
        }

    }

    private PropertyEditor helper;
    private boolean edit;

    public FieldPluginEditorPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        edit = (getLayoutContext().getMode() == IEditorContext.Mode.EDIT);

        try {
            ITypeDescriptor type = getTypeModel();
            IPluginConfig helperConfig = new JavaPluginConfig(config.getName() + ".helper");
            helperConfig.putAll(config);
            helperConfig.put("wicket.id", config.getString("wicket.helper.id"));
            helper = new PropertyEditor(getPluginContext(), helperConfig, getLayoutContext().getEditablePluginConfig(),
                    type, edit);
            getLayoutContext().addBuilderListener(new IBuilderListener() {
                private static final long serialVersionUID = 1L;

                public void onFocus() {
                    helper.show(true);
                }

                public void onBlur() {
                    helper.show(false);
                }
            });
        } catch (TemplateEngineException ex) {
            log.error("Unable to open property editor", ex);
        }
    }

    private ITemplateEngine getTemplateEngine() {
        return getPluginContext().getService(getEffectivePluginConfig().getString("engine"), ITemplateEngine.class);
    }

    private ITypeDescriptor getTypeModel() throws TemplateEngineException {
        IPluginConfig config = getEffectivePluginConfig();
        ITemplateEngine engine = getTemplateEngine();
        if (config.getString("type") == null) {
            IPluginContext context = getPluginContext();
            IModelReference reference = context.getService(config.getString("wicket.model"), IModelReference.class);
            return engine.getType(reference.getModel());
        } else {
            return engine.getType(config.getString("type"));
        }
    }

}
