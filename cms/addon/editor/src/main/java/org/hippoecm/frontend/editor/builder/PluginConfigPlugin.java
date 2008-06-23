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
import org.hippoecm.frontend.editor.ITemplateEngine;
import org.hippoecm.frontend.editor.config.AutoTypeStore;
import org.hippoecm.frontend.editor.config.BuiltinTemplateStore;
import org.hippoecm.frontend.editor.impl.TemplateEngine;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.ModelService;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.IPluginControl;
import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.ClusterConfigDecorator;
import org.hippoecm.frontend.plugins.standardworkflow.types.IFieldDescriptor;
import org.hippoecm.frontend.plugins.standardworkflow.types.ITypeDescriptor;
import org.hippoecm.frontend.plugins.standardworkflow.types.ITypeStore;
import org.hippoecm.frontend.plugins.standardworkflow.types.JavaFieldDescriptor;
import org.hippoecm.frontend.plugins.standardworkflow.types.JavaTypeDescriptor;
import org.hippoecm.frontend.plugins.standardworkflow.types.JcrFieldDescriptor;
import org.hippoecm.frontend.plugins.standardworkflow.types.JcrTypeDescriptor;
import org.hippoecm.frontend.service.IRenderService;
import org.hippoecm.frontend.service.PluginRequestTarget;
import org.hippoecm.frontend.service.ServiceTracker;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.service.render.RenderService;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PluginConfigPlugin extends RenderPlugin {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(PluginConfigPlugin.class);

    private static int instanceCount = 0;

    private IPluginControl field;
    private IPluginControl fieldParams;
    private IPluginControl template;
    private List<IRenderService> children;
    private BuiltinTemplateStore templateStore;

    private JcrTypeDescriptor typeModel;

    public PluginConfigPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        children = new LinkedList<IRenderService>();
        templateStore = new BuiltinTemplateStore(new AutoTypeStore());

        addExtension("field");
        addExtension("fieldParams");
        addExtension("template");

        onModelChanged();
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
        if (field != null) {
            field.stopPlugin();
            field = null;
        }
        if (fieldParams != null) {
            fieldParams.stopPlugin();
            fieldParams = null;
        }
        if (template != null) {
            template.stopPlugin();
            template = null;
        }

        // look for the type
        typeModel = getTypeModel();
        if (typeModel != null) {
            field = createFieldPlugin();
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

    protected IPluginControl createFieldPlugin() {
        IFieldDescriptor fieldModel = getFieldModel();
        if (fieldModel != null && fieldModel instanceof JcrFieldDescriptor) {
            JcrNodeModel nodeModel = ((JcrFieldDescriptor) fieldModel).getNodeModel();
            ITemplateEngine engine = getTemplateEngine();
            ITypeDescriptor type = engine.getType(HippoNodeType.NT_FIELD);

            if (type != null) {
                IClusterConfig clusterConfig = engine.getTemplate(type, ITemplateEngine.EDIT_MODE);
                if (clusterConfig != null) {
                    IPluginContext context = getPluginContext();
                    String id = getExtensionId("field");
                    clusterConfig.put(RenderService.WICKET_ID, id);

                    String modelId = clusterConfig.getString(RenderService.MODEL_ID);
                    final ModelService modelService = new ModelService(modelId, nodeModel);
                    modelService.init(context);

                    final IPluginControl control = context.start(clusterConfig);
                    return new IPluginControl() {
                        private static final long serialVersionUID = 1L;

                        public void stopPlugin() {
                            control.stopPlugin();
                            modelService.destroy();
                        }
                    };
                }
            }
        }
        return null;
    }

    private IPluginControl createFieldParamsPlugin() {
        JcrNodeModel itemNodeModel = (JcrNodeModel) getModel();
        Node itemNode = itemNodeModel.getNode();
        if (itemNode != null && getFieldModel() != null) {
            ITypeDescriptor typeDescriptor = new JavaTypeDescriptor("internal", "frontend:plugin") {
                private static final long serialVersionUID = 1L;

                @Override
                public Map<String, IFieldDescriptor> getFields() {
                    Map<String, IFieldDescriptor> result = new HashMap<String, IFieldDescriptor>();

                    JavaFieldDescriptor caption = new JavaFieldDescriptor("caption");
                    caption.setIsMultiple(false);
                    caption.setType("String");
                    result.put("caption", caption);

                    JavaFieldDescriptor css = new JavaFieldDescriptor("css");
                    css.setIsMultiple(true);
                    css.setType("String");
                    result.put("css", css);

                    return result;
                }
            };
            return editPluginNode("fieldParams", itemNodeModel, typeDescriptor);
        }
        return null;
    }

    private IPluginControl createTemplatePlugin() {
        IFieldDescriptor fieldModel = getFieldModel();
        if (fieldModel != null) {
            IPluginContext context = getPluginContext();
            IPluginConfig config = getPluginConfig();

            String type = fieldModel.getType();
            ITemplateEngine engine = context.getService(config.getString("engine"), ITemplateEngine.class);
            final IClusterConfig target = engine.getTemplate(engine.getType(type), "edit");

            final List<String> exempt = new ArrayList<String>();
            exempt.add("wicket.id");
            exempt.add("engine");
            exempt.add("mode");
            ITypeDescriptor typeDescriptor = new JavaTypeDescriptor("internal", "frontend:plugin") {
                private static final long serialVersionUID = 1L;

                @Override
                public Map<String, IFieldDescriptor> getFields() {
                    Map<String, IFieldDescriptor> result = new HashMap<String, IFieldDescriptor>();
                    for (String override : target.getOverrides()) {
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

    private IPluginControl editPluginNode(String extension, JcrNodeModel itemNodeModel,
            final ITypeDescriptor typeDescriptor) {
        IPluginConfig config = getPluginConfig();
        final IPluginContext context = getPluginContext();
        final ITemplateEngine origEngine = context.getService(config.getString("engine"), ITemplateEngine.class);
        final ITemplateEngine engine = new TemplateEngine(context, new ITypeStore() {
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

        IClusterConfig templateConfig = new ClusterConfigDecorator(templateStore.getTemplate(typeDescriptor, "edit"), newId());
        templateConfig.put("wicket.id", getExtensionId(extension));
        templateConfig.put("engine", engineId);

        String modelId = templateConfig.getString(RenderService.MODEL_ID);
        final ModelService modelService = new ModelService(modelId, itemNodeModel);
        modelService.init(context);

        final IPluginControl control = context.start(templateConfig);
        return new IPluginControl() {
            private static final long serialVersionUID = 1L;

            public void stopPlugin() {
                control.stopPlugin();
                modelService.destroy();
                context.unregisterService(engine, engineId);
            }

        };
    }

    private ITemplateEngine getTemplateEngine() {
        return getPluginContext().getService(getPluginConfig().getString("engine"), ITemplateEngine.class);
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
        JcrNodeModel typeNodeModel = (JcrNodeModel) getModel();
        try {
            while (typeNodeModel != null) {
                Node typeNode = typeNodeModel.getNode();
                if (typeNode.isNodeType(HippoNodeType.NT_TEMPLATETYPE)) {
                    return new JcrTypeHelper(new JcrNodeModel(typeNode)).getTypeDescriptor("draft");
                }
                typeNodeModel = typeNodeModel.getParentModel();
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
        return null;
    }

    private static String newId() {
        synchronized (PluginConfigPlugin.class) {
            return PluginConfigPlugin.class.getName() + "." + (instanceCount++);
        }
    }

}
