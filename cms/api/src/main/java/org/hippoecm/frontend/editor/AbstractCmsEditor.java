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
package org.hippoecm.frontend.editor;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IDetachable;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.io.IClusterable;
import org.hippoecm.frontend.model.ModelReference;
import org.hippoecm.frontend.model.event.IRefreshable;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractCmsEditor<T> implements IEditor<T>, IDetachable, IRefreshable {
    private static final long serialVersionUID = 1L;


    private static int editorCount = 0;

    private static final Logger log = LoggerFactory.getLogger(AbstractCmsEditor.class);

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

    private IEditorContext editorContext;
    private IModel<T> model;
    private IPluginContext context;
    private IPluginConfig parameters;

    private IClusterControl cluster;
    private EditorWrapper renderer;
    private ModelReference<T> modelService;
    private ModelReference<T> baseService;
    private IFocusListener focusListener;
    private String editorId;
    private String wicketId;
    private Mode mode;

    public AbstractCmsEditor(IEditorContext editorContext, IPluginContext context, IPluginConfig parameters,
            IModel<T> model, Mode mode) throws EditorException {
        this.editorContext = editorContext;
        this.model = model;
        this.context = context;
        this.parameters = parameters;
        this.mode = mode;

        editorId = getClass().getName() + "." + (editorCount++);
        wicketId = parameters.getString(RenderService.WICKET_ID);
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
            start();
        } else {
            this.mode = mode;
        }
    }

    public IModel<T> getModel() {
        return model;
    }

    public boolean isModified() throws EditorException {
        return false;
    }

    public boolean isValid() throws EditorException {
        return true;
    }

    /**
     * Default implementation that does nothing. Subclasses are expected to override this behaviour.
     *
     * @throws EditorException
     */
    public void save() throws EditorException {
        //INTENTIONALLY LEFT BLANK
    }

    /**
     * Default implementation that does nothing. Subclasses are expected to override this behaviour.
     *
     * @throws EditorException
     */
    public void done() throws EditorException {
        //INTENTIONALLY LEFT BLANK
    }

    /**
     * Default implementation that does nothing. Subclasses are expected to override this behaviour.
     *
     * @throws EditorException
     */
    public void revert() throws EditorException {
        //INTENTIONALLY LEFT BLANK
    }

    /**
     * Default implementation that does nothing. Subclasses are expected to override this behaviour.
     *
     * @throws EditorException
     */
    public void discard() throws EditorException {
        //INTENTIONALLY LEFT BLANK
    }

    public void close() throws EditorException {
        if (context.getReference(this) != null) {
            Map<IEditorFilter, Object> filterContexts = preClose();

            stop();

            onClose();

            postClose(filterContexts);
        }
        editorContext.onClose();
    }

    protected Map<IEditorFilter, Object> preClose() throws EditorException {
        List<IEditorFilter> filters = context.getServices(context.getReference(this).getServiceId(),
                IEditorFilter.class);
        IdentityHashMap<IEditorFilter, Object> filterContexts = new IdentityHashMap<>();
        //        for (IEditorFilter filter : filters) {
        //            Object filterContext = filter.preClose();
        //            if (filterContext == null) {
        //                throw new EditorException("Close operation cancelled by filter");
        //            }
        //            filterContexts.put(filter, filterContext);
        //        }
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

    protected IModel<T> getEditorModel() throws EditorException {
        return model;
    }

    protected IModel<T> getBaseModel() throws EditorException {
        throw new EditorException("Compare not supported");
    }

    protected IClusterConfig getClusterConfig() {
        return cluster.getClusterConfig();
    }

    public void start() throws EditorException {
        String clusterName;
        switch (mode) {
        case EDIT:
            clusterName = "cms-editor";
            break;
        case COMPARE:
        case VIEW:
        default:
            clusterName = "cms-preview";
            break;
        }
        JavaPluginConfig editorConfig = new JavaPluginConfig(parameters);
        editorConfig.put("wicket.id", editorId);

        IPluginConfigService pluginConfigService = context.getService(IPluginConfigService.class.getName(),
                IPluginConfigService.class);
        IClusterConfig clusterConfig = pluginConfigService.getCluster(clusterName);
        if (clusterConfig == null) {
            throw new EditorException("No cluster found with name " + clusterName);
        }

        cluster = context.newCluster(clusterConfig, editorConfig);
        IClusterConfig decorated = cluster.getClusterConfig();

        String modelId = decorated.getString(RenderService.MODEL_ID);
        modelService = new ModelReference<>(modelId, getEditorModel());
        modelService.init(context);

        if (mode == Mode.COMPARE || mode == Mode.VIEW) {
            String baseId = decorated.getString("model.compareTo");
            baseService = new ModelReference<>(baseId, getBaseModel());
            baseService.init(context);
        }

        String editorId = decorated.getString("editor.id");
        context.registerService(this, editorId);
        context.registerService(editorContext.getEditorManager(), editorId);

        cluster.start();

        IRenderService renderer = context
                .getService(decorated.getString(RenderService.WICKET_ID), IRenderService.class);
        if (renderer == null) {
            cluster.stop();
            context.unregisterService(this, editorId);
            context.unregisterService(editorContext.getEditorManager(), editorId);
            modelService.destroy();
            throw new EditorException("No IRenderService found");
        }

        String renderId = getRendererServiceId();

        // attach self to renderer, so that other plugins can close us
        context.registerService(this, renderId);

        // observe focus events, those need to be synchronized with the active model of the editor manager
        focusListener = new IFocusListener() {
            private static final long serialVersionUID = 1L;

            public void onFocus(IRenderService renderService) {
                editorContext.onFocus();
            }

        };
        context.registerService(focusListener, renderId);
    }

    public void stop() {
        String renderId = getRendererServiceId();
        context.unregisterService(focusListener, renderId);
        context.unregisterService(this, renderId);

        cluster.stop();

        String editorId = cluster.getClusterConfig().getString("editor.id");
        context.unregisterService(editorContext.getEditorManager(), editorId);
        context.unregisterService(this, editorId);

        if (baseService != null) {
            baseService.destroy();
            baseService = null;
        }

        if (modelService != null) {
            modelService.destroy();
            modelService = null;
        }

        cluster = null;
        focusListener = null;
    }

    protected String getRendererServiceId() {
        return context.getReference(renderer).getServiceId();
    }

    public Form getForm() {
        if (cluster != null) {
            final String formServiceId = cluster.getClusterConfig().getString("service.form");
            if (formServiceId != null) {
                final IFormService formService = context.getService(formServiceId, IFormService.class);
                if (formService != null) {
                    return formService.getForm();
                }
            }
        }
        return null;
    }

    protected void onClose() {
        renderer.dispose();
        renderer = null;
    }

    public void refresh() {
    }

    public void focus() {
        if (renderer != null) {
            renderer.focus(null);
        }
    }

    public void detach() {
        model.detach();
        if (modelService != null) {
            modelService.detach();
        }
    }

}
