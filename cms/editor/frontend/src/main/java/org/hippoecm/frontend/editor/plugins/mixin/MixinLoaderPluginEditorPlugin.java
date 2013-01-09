/*
 *  Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.editor.plugins.mixin;

import java.util.Iterator;

import javax.jcr.RepositoryException;

import org.apache.wicket.Session;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.editor.ITemplateEngine;
import org.hippoecm.frontend.editor.TemplateEngineException;
import org.hippoecm.frontend.editor.builder.EditorContext;
import org.hippoecm.frontend.editor.builder.IBuilderListener;
import org.hippoecm.frontend.editor.builder.RenderPluginEditorPlugin;
import org.hippoecm.frontend.editor.plugins.field.TemplateParameterEditor;
import org.hippoecm.frontend.model.event.IEvent;
import org.hippoecm.frontend.model.event.IObserver;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JavaPluginConfig;
import org.hippoecm.frontend.service.IEditor;
import org.hippoecm.frontend.service.render.RenderService;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.types.ITypeDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MixinLoaderPluginEditorPlugin extends RenderPluginEditorPlugin {

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(MixinLoaderPluginEditorPlugin.class);

    private static final String TEMPLATE_PARAMETER_EDITOR = "templateParameterEditor";

    class PropertyEditor extends RenderService<IPluginConfig> {
        private static final long serialVersionUID = 1L;

        private ITypeDescriptor type;
        private IPluginConfig edited;
        private boolean shown = true;

        public PropertyEditor(IPluginContext context, IPluginConfig properties, IPluginConfig edited,
                ITypeDescriptor type, boolean edit) throws TemplateEngineException {
            super(context, properties);

            this.type = type;
            this.edited = edited;
            context.registerService(new IObserver<IPluginConfig>() {
                private static final long serialVersionUID = 1L;

                public IPluginConfig getObservable() {
                    return PropertyEditor.this.edited;
                }

                public void onEvent(Iterator<? extends IEvent<IPluginConfig>> events) {
                    updatePreview();
                    redraw();
                }

            }, IObserver.class.getName());;

            add(new EmptyPanel(TEMPLATE_PARAMETER_EDITOR));
        }

        @Override
        protected void onBeforeRender() {
            // Parameter editor
            Panel panel = new EmptyPanel(TEMPLATE_PARAMETER_EDITOR);
            ITemplateEngine engine = getTemplateEngine();
            try {
                ITypeDescriptor typeDescriptor = engine.getType(PropertyEditor.this.edited.getString("mixin"));
                IClusterConfig target = engine.getTemplate(typeDescriptor, IEditor.Mode.EDIT);
                panel = new TemplateParameterEditor(TEMPLATE_PARAMETER_EDITOR, getClusterParameters(edit), target,
                        edit);
            } catch (TemplateEngineException e) {
                log.error("engine exception when rendering template parameter editor", e);
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

        private IModel<IPluginConfig> getClusterParameters(boolean edit) {
            if (edit && edited.getPluginConfig("cluster.options") == null) {
                edited.put("cluster.options", new JavaPluginConfig());
                try {
                    UserSession.get().getJcrSession().save();
                } catch (RepositoryException ex) {
                    log.error("failed to add child node to plugin config", ex);
                }
            }
            return new Model<IPluginConfig>(edited.getPluginConfig("cluster.options"));
        }

    }

    private PropertyEditor helper;
    private boolean edit;

    public MixinLoaderPluginEditorPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        edit = (getBuilderContext().getMode() == EditorContext.Mode.EDIT);

        try {
            ITypeDescriptor type = getBuilderContext().getType();
            IPluginConfig helperConfig = new JavaPluginConfig(config.getName() + ".helper");
            helperConfig.putAll(config);
            helperConfig.put("wicket.id", config.getString("wicket.helper.id"));
            helper = new PropertyEditor(getPluginContext(), helperConfig,
                    getBuilderContext().getEditablePluginConfig(), type, edit);
            helper.show(getBuilderContext().hasFocus());
            getBuilderContext().addBuilderListener(new IBuilderListener() {
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

}
