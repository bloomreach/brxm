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

import java.util.Iterator;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.editor.ITemplateEngine;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TemplateTypeEditorPlugin extends RenderPlugin {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(TemplateTypeEditorPlugin.class);

    private class TemplateObserver implements IObserver {
        private static final long serialVersionUID = 1L;

        private final IObservable template;

        public TemplateObserver(IObservable template) {
            this.template = template;
        }

        public IObservable getObservable() {
            return (IObservable) template;
        }

        public void onEvent(Iterator<? extends IEvent> event) {
            modelChanged();
        }

    };

    private TemplateBuilder builder;
    private IClusterControl child;
    private IObserver templateObserver;
    private String clusterModelId;
    private String selectedPluginId;
    private String selectedExtPtId;

    public TemplateTypeEditorPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        addExtensionPoint("template");

        clusterModelId = context.getReference(this).getServiceId() + ".model.cluster";
        ModelReference clusterModelService = new ModelReference(clusterModelId, null);
        clusterModelService.init(getPluginContext());

        selectedPluginId = context.getReference(this).getServiceId() + ".model.selected_plugin";
        ModelReference selectedPluginService = new ModelReference(selectedPluginId, null);
        selectedPluginService.init(getPluginContext());

        selectedExtPtId = context.getReference(this).getServiceId() + ".model.selected_extension_point";
        final ModelReference selectedExtPtService = new ModelReference(selectedExtPtId, null);
        selectedExtPtService.init(getPluginContext());

        IModel nodeModel = getModel();
        try {
            Node node = (Node) nodeModel.getObject();
            String typeName = node.getParent().getName() + ":" + node.getName();
            IModel selectedExtensionPointModel = new IModel() {

                public Object getObject() {
                    IModel upstream = selectedExtPtService.getModel();
                    if (upstream != null) {
                        if (upstream.getObject() != null) {
                            return upstream.getObject();
                        }
                    }
                    return "${cluster.id}.field";
                }

                public void setObject(Object object) {
                    // TODO Auto-generated method stub

                }

                public void detach() {
                    // TODO Auto-generated method stub

                }

            };
            builder = new TemplateBuilder(typeName, !"edit".equals(config.getString("mode")), context,
                    selectedExtensionPointModel);
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
            throw new RuntimeException("Failed to initialize", ex);
        } catch (BuilderException e) {
            log.error(e.getMessage());
            throw new RuntimeException("Failed to initialize", e);
        }

        onModelChanged();
    }

    @Override
    public void onModelChanged() {
        super.onModelChanged();

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

            PreviewClusterConfig template = new PreviewClusterConfig(builder.getTemplate(), clusterModelId,
                    selectedPluginId, selectedExtPtId, config.getString("wicket.helper.id"), "edit".equals(mode));

            context.getService(clusterModelId, IModelReference.class).setModel(new Model(builder.getTemplate()));
            context.getService(selectedPluginId, IModelReference.class).setModel(selectedPlugin);
            // selectedExtPt ?

            IPluginConfig parameters = new JavaPluginConfig();
            parameters.put(ITemplateEngine.ENGINE, config.getString("engine"));
            parameters.put(ITemplateEngine.MODE, mode);
            parameters.put(RenderService.WICKET_ID, config.getString("template"));

            final IClusterControl control = context.newCluster(template, parameters);
            String modelId = control.getClusterConfig().getString(RenderService.MODEL_ID);
            IModel prototypeModel = builder.getPrototype();
            final ModelReference modelService = new ModelReference(modelId, prototypeModel);
            modelService.init(getPluginContext());

            String typeModelId = config.getString("model.type");
            final ModelReference typeService = new ModelReference(typeModelId, new Model(builder.getTypeDescriptor()));
            typeService.init(getPluginContext());

            control.start();

            context.registerService(templateObserver = new TemplateObserver(builder), IObserver.class.getName());
            child = new IClusterControl() {
                private static final long serialVersionUID = 1L;

                public void stop() {
                    control.stop();
                    typeService.destroy();
                    modelService.destroy();
                }

                public IClusterConfig getClusterConfig() {
                    return null;
                }

                public void start() {
                }
            };

            redraw();
        } catch (BuilderException ex) {
            log.error("Failed to update model", ex);
        }
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

}
