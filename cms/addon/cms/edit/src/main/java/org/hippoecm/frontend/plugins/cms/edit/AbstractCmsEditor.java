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
package org.hippoecm.frontend.plugins.cms.edit;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import org.apache.wicket.IClusterable;
import org.apache.wicket.model.IDetachable;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.ModelReference;
import org.hippoecm.frontend.plugin.IClusterControl;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfigService;
import org.hippoecm.frontend.plugin.config.impl.JavaPluginConfig;
import org.hippoecm.frontend.service.EditorException;
import org.hippoecm.frontend.service.IEditor;
import org.hippoecm.frontend.service.IEditorFilter;
import org.hippoecm.frontend.service.IFocusListener;
import org.hippoecm.frontend.service.IRenderService;
import org.hippoecm.frontend.service.ServiceContext;
import org.hippoecm.frontend.service.ServiceTracker;
import org.hippoecm.frontend.service.render.RenderService;

class AbstractCmsEditor<T extends IModel> implements IEditor, IDetachable {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static int editorCount = 0;

    private class EditorWrapper extends RenderService {
        private static final long serialVersionUID = 1L;

        public EditorWrapper(IPluginContext context, IPluginConfig properties) {
            super(new ServiceContext(context), properties);

            addExtensionPoint("editor");
        }

        @Override
        protected ExtensionPoint createExtensionPoint(String extension) {
            return new Editor(extension);
        }

        protected String getServiceId() {
            return getPluginContext().getReference(this).getServiceId();
        }
        
        void dispose() {
            ((ServiceContext) getPluginContext()).stop();
        }

        // forward 

        protected class Forwarder extends ServiceTracker<IClusterable> {
            private static final long serialVersionUID = 1L;

            public Forwarder() {
                super(IClusterable.class);
            }

            @Override
            protected void onServiceAdded(IClusterable service, String name) {
                getPluginContext().registerService(service, getServiceId());
            }

            @Override
            protected void onRemoveService(IClusterable service, String name) {
                getPluginContext().unregisterService(service, getServiceId());
            }
        }

        protected class Editor extends ExtensionPoint {
            private static final long serialVersionUID = 1L;

            private int count = 0;
            private Forwarder forwarder;

            protected Editor(String extension) {
                super(extension);
            }

            @Override
            public void onServiceAdded(IRenderService service, String name) {
                super.onServiceAdded(service, name);
                count++;
                if (forwarder == null) {
                    String rendererServiceId = context.getReference(service).getServiceId();
                    forwarder = new Forwarder();
                    getPluginContext().registerTracker(forwarder, rendererServiceId);
                }
            }

            @Override
            public void onRemoveService(IRenderService service, String name) {
                if (--count == 0) {
                    String rendererServiceId = context.getReference(service).getServiceId();
                    getPluginContext().unregisterTracker(forwarder, rendererServiceId);
                    forwarder = null;
                }
                super.onRemoveService(service, name);
            }
        }

    }

    private EditorManagerPlugin manager;
    private T model;
    private IPluginContext context;
    private IPluginConfig config;

    private IClusterControl cluster;
    private EditorWrapper renderer;
    private ModelReference<T> modelService;
    private IFocusListener focusListener;
    private String editorId;
    private String wicketId;
    private Mode mode;

    AbstractCmsEditor(final EditorManagerPlugin manager, IPluginContext context, IPluginConfig config, T model,
            Mode mode) throws CmsEditorException {
        this.manager = manager;
        this.model = model;
        this.context = context;
        this.config = config;
        this.mode = mode;

        IPluginConfig previewConfig = config.getPluginConfig("cluster.preview.options");
        IPluginConfig editConfig = config.getPluginConfig("cluster.edit.options");
        if (!previewConfig.getString(RenderService.WICKET_ID).equals(editConfig.getString(RenderService.WICKET_ID))) {
            throw new CmsEditorException("preview and edit clusters have different wicket.id values");
        }

        editorId = getClass().getName() + "." + (editorCount++);
        wicketId = editConfig.getString(RenderService.WICKET_ID);
        JavaPluginConfig renderConfig = new JavaPluginConfig();
        renderConfig.put(RenderService.WICKET_ID, wicketId);
        renderConfig.put("editor", editorId);
        renderer = new EditorWrapper(context, renderConfig);
    }

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) throws EditorException {
        if (mode != this.mode && cluster != null) {
            stop();
            this.mode = mode;
            try {
                start();
            } catch (CmsEditorException ex) {
                throw new EditorException("failed to restart editor", ex);
            }
        } else {
            this.mode = mode;
        }
    }

    public T getModel() {
        return model;
    }

    public void close() throws EditorException {
        if (context.getReference(this) != null) {
            Map<IEditorFilter, Object> filterContexts = preClose();

            stop();

            onClose();

            postClose(filterContexts);
        }
        manager.onClose(this);
    }

    protected Map<IEditorFilter, Object> preClose() throws EditorException {
        List<IEditorFilter> filters = context.getServices(context.getReference(this).getServiceId(),
                IEditorFilter.class);
        IdentityHashMap<IEditorFilter, Object> filterContexts = new IdentityHashMap<IEditorFilter, Object>();
        for (IEditorFilter filter : filters) {
            Object filterContext = filter.preClose();
            if (filterContext == null) {
                throw new EditorException("Close operation cancelled by filter");
            }
            filterContexts.put(filter, filterContext);
        }
        return filterContexts;
    }
    
    protected void postClose(Map<IEditorFilter, Object> contexts) {
        for (Map.Entry<IEditorFilter, Object> entry : contexts.entrySet()) {
            entry.getKey().postClose(entry.getValue());
        }
    }
    
    protected IPluginContext getPluginContext() {
        return context;
    }

    protected IPluginConfig getPluginConfig() {
        return config;
    }

    protected T getEditorModel() {
        return model;
    }

    protected void start() throws CmsEditorException {
        String clusterName;
        IPluginConfig parameters;
        switch (mode) {
        case EDIT:
            clusterName = config.getString("cluster.edit.name");
            parameters = config.getPluginConfig("cluster.edit.options");
            break;
        case VIEW:
        default:
            clusterName = config.getString("cluster.preview.name");
            parameters = config.getPluginConfig("cluster.preview.options");
            break;
        }
        JavaPluginConfig editorConfig = new JavaPluginConfig(parameters);
        editorConfig.put("wicket.id", editorId);

        IPluginConfigService pluginConfigService = context.getService(IPluginConfigService.class.getName(),
                IPluginConfigService.class);
        IClusterConfig clusterConfig = pluginConfigService.getCluster(clusterName);
        if (clusterConfig == null) {
            throw new CmsEditorException("No cluster found with name " + clusterName);
        }

        cluster = context.newCluster(clusterConfig, editorConfig);
        IClusterConfig decorated = cluster.getClusterConfig();

        String modelId = decorated.getString(RenderService.MODEL_ID);
        modelService = new ModelReference<T>(modelId, getEditorModel());
        modelService.init(context);

        String editorId = decorated.getString("editor.id");
        context.registerService(this, editorId);
        context.registerService(manager, editorId);

        cluster.start();

        IRenderService renderer = context
                .getService(decorated.getString(RenderService.WICKET_ID), IRenderService.class);
        if (renderer == null) {
            cluster.stop();
            context.unregisterService(this, editorId);
            context.unregisterService(manager, editorId);
            modelService.destroy();
            throw new CmsEditorException("No IRenderService found");
        }

        String renderId = getRendererServiceId();

        // attach self to renderer, so that other plugins can close us
        context.registerService(this, renderId);

        // observe focus events, those need to be synchronized with the active model of the editor manager
        focusListener = new IFocusListener() {
            private static final long serialVersionUID = 1L;

            public void onFocus(IRenderService renderService) {
                if (!manager.active) {
                    manager.active = true;
                    manager.onFocus(AbstractCmsEditor.this);
                    manager.active = false;
                }
            }

        };
        context.registerService(focusListener, renderId);
    }

    protected void stop() {
        String renderId = getRendererServiceId();
        context.unregisterService(focusListener, renderId);
        context.unregisterService(this, renderId);

        cluster.stop();

        String editorId = cluster.getClusterConfig().getString("editor.id");
        context.unregisterService(manager, editorId);
        context.unregisterService(this, editorId);

        modelService.destroy();

        cluster = null;
        modelService = null;
        focusListener = null;
    }

    protected String getRendererServiceId() {
        return context.getReference(renderer).getServiceId();
    }

    protected void onClose() {
        renderer.dispose();
        renderer = null;
    }

    void refresh() {
    }

    void focus() {
        if (renderer != null) {
            renderer.focus(null);
        }
    }

    public void detach() {
        model.detach();
    }

}
