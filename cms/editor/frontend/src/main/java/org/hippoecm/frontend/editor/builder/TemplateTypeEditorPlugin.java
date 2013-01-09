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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.PluginRequestTarget;
import org.hippoecm.frontend.editor.ITemplateEngine;
import org.hippoecm.frontend.editor.impl.TemplateEngineFactory;
import org.hippoecm.frontend.editor.layout.ILayoutContext;
import org.hippoecm.frontend.editor.layout.ILayoutPad;
import org.hippoecm.frontend.editor.layout.ILayoutTransition;
import org.hippoecm.frontend.editor.layout.JavaLayoutPad;
import org.hippoecm.frontend.model.IModelReference;
import org.hippoecm.frontend.model.ModelReference;
import org.hippoecm.frontend.model.event.IEvent;
import org.hippoecm.frontend.model.event.IObservable;
import org.hippoecm.frontend.model.event.IObserver;
import org.hippoecm.frontend.plugin.IClusterControl;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JavaPluginConfig;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.service.render.RenderService;
import org.hippoecm.frontend.types.ITypeDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TemplateTypeEditorPlugin extends RenderPlugin<Node> {

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(TemplateTypeEditorPlugin.class);

    private TemplateBuilder builder;
    private IClusterControl child;
    private IObserver<IObservable> templateObserver;
    private final String clusterModelId;
    private final String typeModelId;
    private final String selectedPluginId;
    private final String selectedExtPtId;
    private String engineId;

    private List<String> names = null;
    private final ModelReference<String> selectedPluginService;
    private final ExtensionPointLocator locator;

    public TemplateTypeEditorPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        addExtensionPoint("template");

        clusterModelId = getServiceId(TemplateBuilderConstants.MODEL_CLUSTER);
        ModelReference clusterModelService = new ModelReference(clusterModelId, null);
        clusterModelService.init(getPluginContext());

        typeModelId = getServiceId(TemplateBuilderConstants.MODEL_TYPE);
        ModelReference typeModelService = new ModelReference(typeModelId, null);
        typeModelService.init(getPluginContext());

        selectedPluginId = getServiceId(TemplateBuilderConstants.MODEL_SELECTED_PLUGIN);
        selectedPluginService = new ModelReference<String>(selectedPluginId, null);
        selectedPluginService.init(getPluginContext());

        selectedExtPtId = getServiceId(TemplateBuilderConstants.MODEL_SELECTED_EXTENSION_POINT);

        locator = new ExtensionPointLocator(selectedPluginService);
        final IModel selectedExtensionPointModel = new IModel() {

            @Override
            public String getObject() {
                return locator.getSelectedExtensionPoint();
            }

            @Override
            public void setObject(final Object object) {
                throw new UnsupportedOperationException("Selected extension point is read only");
            }

            @Override
            public void detach() {
            }
        };
        final ModelReference selectedExtPtService = new ModelReference(selectedExtPtId, selectedExtensionPointModel);
        selectedExtPtService.init(getPluginContext());

        IModel<Node> nodeModel = getModel();
        try {
            Node node = nodeModel.getObject();
            String typeName = node.getParent().getName() + ":" + node.getName();

            engineId = context.getReference(this).getServiceId() + ".engine";
            TemplateEngineFactory factory = new TemplateEngineFactory(node.getParent().getName());
            context.registerService(factory, engineId);

            final IModel<String> selectedPluginModel = new SelectedPluginModel(selectedPluginService);
            builder = new TemplateBuilder(typeName, !"edit".equals(config.getString("mode")), context,
                                          selectedExtensionPointModel, selectedPluginModel);

            context.registerService(builder, getServiceId(TemplateBuilderConstants.MODEL_BUILDER));
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
            throw new RuntimeException("Failed to initialize", ex);
        } catch (BuilderException e) {
            log.info(e.getMessage());
        }
    }

    private String getServiceId(String key) {
        return getPluginConfig().getString(key, getPluginContext().getReference(this).getServiceId() + "." + key);
    }

    @Override
    protected void onStart() {
        super.onStart();
        onModelChanged();
    }

    @Override
    public void onModelChanged() {
        super.onModelChanged();

        if (builder == null) {
            return;
        }

        final IPluginContext context = getPluginContext();
        final IPluginConfig config = getPluginConfig();

        IModel selectedPlugin = null;
        if (child != null) {
            selectedPlugin = context.getService(selectedPluginId, IModelReference.class).getModel();
            context.unregisterService(templateObserver, IObserver.class.getName());
            templateObserver = null;
            child.stop();
            child = null;
        }

        try {
            String mode = config.getString("mode");

            Map<String, String> builderParameters = new TreeMap<String, String>();
            builderParameters.put("wicket.helper.id", config.getString("wicket.helper.id"));
            builderParameters.put("wicket.model", clusterModelId);
            builderParameters.put("model.type", typeModelId);
            builderParameters.put("model.plugin", selectedPluginId);
            PreviewClusterConfig template = new PreviewClusterConfig(builder.getTemplate(), builderParameters,
                                                                     "edit".equals(mode));

            context.getService(clusterModelId, IModelReference.class).setModel(new Model<IClusterConfig>(builder.getTemplate()));
            context.getService(typeModelId, IModelReference.class).setModel(
                    new Model<ITypeDescriptor>(builder.getTypeDescriptor()));
            context.getService(selectedPluginId, IModelReference.class).setModel(selectedPlugin);

            IPluginConfig parameters = new JavaPluginConfig();
            parameters.put(ITemplateEngine.ENGINE, engineId);
            parameters.put(ITemplateEngine.MODE, mode);
            parameters.put(RenderService.WICKET_ID, config.getString("template"));

            final IClusterControl control = context.newCluster(template, parameters);
            String modelId = control.getClusterConfig().getString(RenderService.MODEL_ID);
            IModel<Node> prototypeModel = builder.getPrototype();
            final ModelReference<Node> modelService = new ModelReference<Node>(modelId, prototypeModel);
            modelService.init(getPluginContext());

            String typeModelId = config.getString("model.type");
            final ModelReference<ITypeDescriptor> typeService = new ModelReference<ITypeDescriptor>(typeModelId, new Model<ITypeDescriptor>(builder.getTypeDescriptor()));
            typeService.init(getPluginContext());

            control.start();

            // select initial target area
            final JavaLayoutPad rootLayoutPad = new JavaLayoutPad("root");
            ILayoutAware layout = context.getService(config.getString("template"), ILayoutAware.class);
            layout.setLayoutContext(new RootLayoutContext(rootLayoutPad));
            locator.setLayoutAwareRoot(layout);
            if (selectedPlugin == null) {
                selectedPlugin = new Model<String>(locator.getDefaultSelectedPlugin());
                context.getService(selectedPluginId, IModelReference.class).setModel(selectedPlugin);
            }

            context.registerService(templateObserver = new TemplateObserver(builder), IObserver.class.getName());
            child = new ChildClusterControl(control, typeService, modelService);

            redraw();
        } catch (BuilderException ex) {
            log.error("Failed to update model", ex);
        }
    }

    @Override
    public void render(PluginRequestTarget target) {
        if (builder != null) {
            try {
                List<IPluginConfig> plugins = builder.getTemplate().getPlugins();
                List<String> newNames = new LinkedList<String>();
                for (IPluginConfig config : plugins) {
                    newNames.add(config.getName());
                }
                if ((this.names == null || !this.names.equals(newNames))) {
                    modelChanged();
                }
                this.names = newNames;
            } catch (BuilderException e) {
                log.error("could not determine whether to repaint", e);
            }
        }
        super.render(target);
    }

    @Override
    protected void onDetach() {
        // null-check; if plugin has registered its render-service and then throws
        // exception, no builder may be available.
        if (builder != null) {
            builder.detach();
        }
        super.onDetach();
    }

    private static class RootLayoutContext implements ILayoutContext {

        private final JavaLayoutPad rootLayoutPad;

        public RootLayoutContext(final JavaLayoutPad rootLayoutPad) {
            this.rootLayoutPad = rootLayoutPad;
        }

        @Override
        public ILayoutPad getLayoutPad() {
            return rootLayoutPad;
        }

        @Override
        public void apply(final ILayoutTransition transition) {
            throw new UnsupportedOperationException("Cannot transition root plugin");
        }
    }

    private class TemplateObserver implements IObserver<IObservable> {
        private static final long serialVersionUID = 1L;

        private final IObservable template;

        public TemplateObserver(IObservable template) {
            this.template = template;
        }

        public IObservable getObservable() {
            return template;
        }

        public void onEvent(Iterator<? extends IEvent<IObservable>> event) {
            modelChanged();
        }
    }

    private static class SelectedPluginModel implements IModel<String> {

        private final ModelReference<String> selectedPluginService;

        public SelectedPluginModel(final ModelReference<String> selectedPluginService) {
            this.selectedPluginService = selectedPluginService;
        }

        @Override
        public String getObject() {
            if (selectedPluginService.getModel() != null) {
                return selectedPluginService.getModel().getObject();
            }
            return null;
        }

        @Override
        public void setObject(final String object) {
            selectedPluginService.setModel(new Model<String>(object));
        }

        @Override
        public void detach() {
            // do nothing
        }
    }

    private static class ChildClusterControl implements IClusterControl {
        private static final long serialVersionUID = 1L;
        private final IClusterControl control;
        private final ModelReference<ITypeDescriptor> typeService;
        private final ModelReference<Node> modelService;

        public ChildClusterControl(final IClusterControl control, final ModelReference<ITypeDescriptor> typeService, final ModelReference<Node> modelService) {
            this.control = control;
            this.typeService = typeService;
            this.modelService = modelService;
        }

        public void start() {
            // do nothing
        }

        public void stop() {
            control.stop();
            typeService.destroy();
            modelService.destroy();
        }

        public IClusterConfig getClusterConfig() {
            return null;
        }
    }

}
