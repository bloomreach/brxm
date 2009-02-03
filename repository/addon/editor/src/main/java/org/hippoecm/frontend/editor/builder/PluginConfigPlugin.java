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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.model.IDetachable;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.PluginRequestTarget;
import org.hippoecm.frontend.editor.ITemplateEngine;
import org.hippoecm.frontend.editor.config.AutoTypeStore;
import org.hippoecm.frontend.editor.config.BuiltinTemplateStore;
import org.hippoecm.frontend.editor.impl.TemplateEngine;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.ModelReference;
import org.hippoecm.frontend.plugin.IClusterControl;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JavaPluginConfig;
import org.hippoecm.frontend.service.IRenderService;
import org.hippoecm.frontend.service.ServiceTracker;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.service.render.RenderService;
import org.hippoecm.frontend.types.IFieldDescriptor;
import org.hippoecm.frontend.types.ITypeDescriptor;
import org.hippoecm.frontend.types.ITypeStore;
import org.hippoecm.frontend.types.JavaFieldDescriptor;
import org.hippoecm.frontend.types.JavaTypeDescriptor;
import org.hippoecm.frontend.types.JcrTypeDescriptor;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PluginConfigPlugin extends RenderPlugin {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(PluginConfigPlugin.class);

    private IClusterControl fieldParams;
    private IClusterControl template;
    private List<IRenderService> children;
    private BuiltinTemplateStore templateStore;

    private JcrTypeDescriptor typeModel;

    public PluginConfigPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        children = new LinkedList<IRenderService>();
        templateStore = new BuiltinTemplateStore(new AutoTypeStore(), this);

        addExtension("fieldParams");
        addExtension("template");

        onModelChanged();

        add(new EmptyPanel("field"));
    }

    @Override
    public void onDetach() {
        if (typeModel != null) {
            typeModel.detach();
        }
        super.onDetach();
    }

    @Override
    public void onModelChanged() {
        if (fieldParams != null) {
            fieldParams.stop();
            fieldParams = null;
        }
        if (template != null) {
            template.stop();
            template = null;
        }

        // look for the type
        typeModel = getTypeModel();
        if (typeModel != null) {
            createFieldEditor();
            template = createTemplatePlugin();
            fieldParams = createFieldParamsPlugin();
        }

        redraw();
    }

    @Override
    public void render(PluginRequestTarget target) {
        super.render(target);
        for (IRenderService service : children) {
            service.render(target);
        }
    }

    private void createFieldEditor() {
        IFieldDescriptor descriptor = getFieldModel();
        if (descriptor != null) {
            FieldEditor service = new FieldEditor("field", new Model(getFieldModel()) {
                private static final long serialVersionUID = 1L;

                @Override
                public void detach() {
                    Object object = getObject();
                    if (object != null) {
                        ((IDetachable) object).detach();
                    }
                }
            });
            service.setType(typeModel);
            replace(service);
        } else {
            replace(new EmptyPanel("field"));
        }
    }

    private IClusterControl createFieldParamsPlugin() {
        JcrNodeModel itemNodeModel = (JcrNodeModel) getModel();
        Node itemNode = itemNodeModel.getNode();
        if (itemNode != null && getFieldModel() != null) {
            ITypeDescriptor typeDescriptor = new JavaTypeDescriptor("internal", "frontend:plugin") {
                private static final long serialVersionUID = 1L;

                @Override
                public Map<String, IFieldDescriptor> getFields() {
                    Map<String, IFieldDescriptor> result = new HashMap<String, IFieldDescriptor>();

                    JavaFieldDescriptor caption = new JavaFieldDescriptor("caption");
                    caption.setMultiple(false);
                    caption.setType("String");
                    result.put("caption", caption);

                    JavaFieldDescriptor css = new JavaFieldDescriptor("wicket.css");
                    css.setMultiple(true);
                    css.setType("String");
                    result.put("css", css);

                    return result;
                }
            };
            return editPluginNode("fieldParams", itemNodeModel, typeDescriptor);
        }
        return null;
    }

    private IClusterControl createTemplatePlugin() {
        IFieldDescriptor fieldModel = getFieldModel();
        if (fieldModel != null) {
            IPluginContext context = getPluginContext();
            IPluginConfig config = getPluginConfig();

            String type = fieldModel.getType();
            ITemplateEngine engine = context.getService(config.getString("engine"), ITemplateEngine.class);
            final IClusterConfig target = engine.getTemplate(engine.getType(type), "edit");

            final List<String> exempt = new ArrayList<String>();
            exempt.add("mode");
            ITypeDescriptor typeDescriptor = new JavaTypeDescriptor("internal", "frontend:plugin") {
                private static final long serialVersionUID = 1L;

                @Override
                public Map<String, IFieldDescriptor> getFields() {
                    Map<String, IFieldDescriptor> result = new HashMap<String, IFieldDescriptor>();
                    for (String override : target.getProperties()) {
                        JavaFieldDescriptor field = new JavaFieldDescriptor("template." + override);
                        if (!exempt.contains(override)) {
                            field.setType("String");
                            result.put(override, field);
                        }
                    }
                    return result;
                }
            };
            JcrNodeModel itemNodeModel = (JcrNodeModel) getModel();
            return editPluginNode("template", itemNodeModel, typeDescriptor);
        }
        return null;
    }

    private IClusterControl editPluginNode(String extension, JcrNodeModel itemNodeModel,
            final ITypeDescriptor typeDescriptor) {
        IPluginConfig config = getPluginConfig();
        final IPluginContext context = getPluginContext();
        final ITemplateEngine origEngine = context.getService(config.getString("engine"), ITemplateEngine.class);
        final TemplateEngine engine = new TemplateEngine(context, new ITypeStore() {
            private static final long serialVersionUID = 1L;

            public ITypeDescriptor getTypeDescriptor(String type) {
                if ("frontend:plugin".equals(type)) {
                    return typeDescriptor;
                }
                return origEngine.getType(type);
            }

            public List<ITypeDescriptor> getTypes(String namespace) {
                return null;
            }

            public void detach() {
            }
        });
        final String engineId = getExtensionId(extension + ".engine");
        context.registerService(engine, engineId);

        IClusterConfig templateConfig = templateStore.getTemplate(typeDescriptor, "edit");
        IPluginConfig parameters = new JavaPluginConfig();
        parameters.put("wicket.id", getExtensionId(extension));
        parameters.put("engine", engineId);
        final IClusterControl control = context.newCluster(templateConfig, parameters);

        String modelId = control.getClusterConfig().getString(RenderService.MODEL_ID);
        final ModelReference modelService = new ModelReference(modelId, itemNodeModel);
        modelService.init(context);

        control.start();
        return new IClusterControl() {
            private static final long serialVersionUID = 1L;

            public void stop() {
                control.stop();
                modelService.destroy();
                context.unregisterService(engine, engineId);
            }

            public IClusterConfig getClusterConfig() {
                return null;
            }

            public void start() {
            }

        };
    }

    private String getExtensionId(String id) {
        return getPluginContext().getReference(this).getServiceId() + "." + id;
    }

    private void addExtension(final String id) {
        add(new EmptyPanel(id));

        IPluginContext context = getPluginContext();
        ServiceTracker<IRenderService> fieldTracker = new ServiceTracker<IRenderService>(IRenderService.class) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onServiceAdded(IRenderService service, String name) {
                service.bind(PluginConfigPlugin.this, id);
                replace(service.getComponent());
                children.add(service);
            }

            @Override
            public void onRemoveService(IRenderService service, String name) {
                replace(new EmptyPanel(id));
                service.unbind();
                children.remove(service);
            }
        };
        context.registerTracker(fieldTracker, getExtensionId(id));
    }

    private IFieldDescriptor getFieldModel() {
        try {
            JcrNodeModel pluginNodeModel = (JcrNodeModel) getModel();
            Node itemNode = pluginNodeModel.getNode();
            if (itemNode.hasProperty("field")) {
                return typeModel.getField(itemNode.getProperty("field").getString());
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
        return null;
    }

    private JcrTypeDescriptor getTypeModel() {
        if ("edit".equals(getPluginConfig().getString("mode"))) {
            JcrNodeModel typeNodeModel = (JcrNodeModel) getModel();
            try {
                while (typeNodeModel != null) {
                    Node typeNode = typeNodeModel.getNode();
                    if (typeNode != null && typeNode.isNodeType(HippoNodeType.NT_TEMPLATETYPE)) {
                        return new JcrTypeHelper(new JcrNodeModel(typeNode)).getTypeDescriptor();
                    }
                    typeNodeModel = typeNodeModel.getParentModel();
                }
            } catch (RepositoryException ex) {
                log.error(ex.getMessage());
            }
        }
        return null;
    }

}
