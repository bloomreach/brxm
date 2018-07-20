/*
 *  Copyright 2008-2017 Hippo B.V. (http://www.onehippo.com)
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.wicket.markup.head.HeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnLoadHeaderItem;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IDetachable;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.Response;
import org.apache.wicket.util.io.IClusterable;
import org.hippoecm.frontend.PluginRequestTarget;
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
import org.hippoecm.frontend.usagestatistics.UsageEvent;
import org.hippoecm.frontend.usagestatistics.UsageStatisticsHeaderItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractCmsEditor<T> implements IEditor<T>, IDetachable, IRefreshable {

    private static final Logger log = LoggerFactory.getLogger(AbstractCmsEditor.class);

    private static final String EVENT_EDITOR_ACTIVATED = "tab-activated";

    private static int editorCount;

    private class EditorWrapper extends RenderService {

        public EditorWrapper(final IPluginContext context, final IPluginConfig properties) {
            super(new ServiceContext(context), properties);

            addExtensionPoint("editor");
        }

        @Override
        protected ExtensionPoint createExtensionPoint(final String extension) {
            return new Editor(extension);
        }

        protected String getServiceId() {
            return getPluginContext().getReference(this).getServiceId();
        }

        void dispose() {
            ((ServiceContext) getPluginContext()).stop();
        }

        @Override
        public void renderHead(final IHeaderResponse response) {
            super.renderHead(response);
            if (isActive()) {
                response.render(new EditorActivatedHeaderItem());
            }
        }

        @Override
        public void render(final PluginRequestTarget target) {
            if (isActive()) {
                if (!isActivated) {
                    isActivated = true;
                    onActivated();
                }
            } else {
                if (isActivated) {
                    isActivated = false;
                    onDeactivated();
                }
            }
            super.render(target);
        }

        // forward

        protected class Forwarder extends ServiceTracker<IClusterable> {

            public Forwarder() {
                super(IClusterable.class);
            }

            @Override
            protected void onServiceAdded(final IClusterable service, final String name) {
                getPluginContext().registerService(service, getServiceId());
            }

            @Override
            protected void onRemoveService(final IClusterable service, final String name) {
                getPluginContext().unregisterService(service, getServiceId());
            }
        }

        protected class Editor extends ExtensionPoint {

            private int count;
            private Forwarder forwarder;

            protected Editor(final String extension) {
                super(extension);
            }

            @Override
            public void onServiceAdded(final IRenderService service, final String name) {
                super.onServiceAdded(service, name);
                count++;
                if (forwarder == null) {
                    final String rendererServiceId = context.getReference(service).getServiceId();
                    forwarder = new Forwarder();
                    getPluginContext().registerTracker(forwarder, rendererServiceId);
                }
            }

            @Override
            public void onRemoveService(final IRenderService service, final String name) {
                if (--count == 0) {
                    final String rendererServiceId = context.getReference(service).getServiceId();
                    getPluginContext().unregisterTracker(forwarder, rendererServiceId);
                    forwarder = null;
                }
                super.onRemoveService(service, name);
            }
        }
    }

    private final IEditorContext editorContext;
    private final IModel<T> model;
    private final IPluginContext context;
    private final IPluginConfig parameters;
    private IClusterControl cluster;
    private EditorWrapper renderer;
    private ModelReference<T> modelService;
    private ModelReference<T> baseService;
    private IFocusListener focusListener;
    private final String editorId;
    private final String wicketId;
    private Mode mode;
    private boolean isActivated;

    public AbstractCmsEditor(final IEditorContext editorContext, final IPluginContext context, final IPluginConfig parameters,
                             final IModel<T> model, final Mode mode) throws EditorException {
        this.editorContext = editorContext;
        this.model = model;
        this.context = context;
        this.parameters = parameters;
        this.mode = mode;

        final String name = getMyName(getClass());
        editorId = name + "." + (editorCount++);
        wicketId = parameters.getString(RenderService.WICKET_ID);
        final JavaPluginConfig renderConfig = new JavaPluginConfig();
        renderConfig.put(RenderService.WICKET_ID, wicketId);
        renderConfig.put("editor", editorId);
        renderer = new EditorWrapper(context, renderConfig);
    }

    /**
     * Method to be able to mock all static methods in HippostdPublishableEditor.
     *
     * @param aClass Any class implementing AbstractCmsEditor.
     * @return Name of the class.
     */
    static String getMyName(final Class<? extends AbstractCmsEditor> aClass) {
        return aClass.getName();
    }

    public Mode getMode() {
        return mode;
    }

    public void setMode(final Mode mode) throws EditorException {
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
            final Map<IEditorFilter, Object> filterContexts = preClose();

            stop();

            onClose();

            postClose(filterContexts);
        }
        editorContext.onClose();
    }

    /**
     * Hook called when an editor is activated, i.e. transitions from inactive to active state.
     * When overiding, make sure to call super.onActivated() in order to keep the usage statistics working.
     */
    protected void onActivated() {
        publishEvent(EVENT_EDITOR_ACTIVATED);
    }

    /**
     * Hook called when an editor is deactivate, i.e. transitions from active to inactive state.
     * When overriding, make sure to call super.onDeactivated().
     */
    protected void onDeactivated() {
    }

    protected void publishEvent(final String name) {
        final UsageEvent event = createUsageEvent(name);
        if (event != null) {
            event.publish();
        }
    }

    protected UsageEvent createUsageEvent(final String name) {
        IModel<T> editorModel = null;
        try {
            editorModel = getEditorModel();
        } catch (final EditorException e) {
            log.warn("Error retrieving editor model", e);
        }
        return createUsageEvent(name, editorModel);
    }

    protected UsageEvent createUsageEvent(final String name, final IModel<T> model) {
        return new UsageEvent(name);
    }

    protected Map<IEditorFilter, Object> preClose() throws EditorException {
        return Collections.emptyMap();
    }

    protected void postClose(final Map<IEditorFilter, Object> contexts) {
        for (final Map.Entry<IEditorFilter, Object> entry : contexts.entrySet()) {
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
        return model;
    }

    protected IClusterConfig getClusterConfig() {
        return cluster.getClusterConfig();
    }

    public void start() throws EditorException {
        final String clusterName;
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
        final JavaPluginConfig editorConfig = new JavaPluginConfig(parameters);
        editorConfig.put("wicket.id", editorId);
        editorConfig.put("mode", mode.toString());

        final IPluginConfigService pluginConfigService = context.getService(IPluginConfigService.class.getName(),
                IPluginConfigService.class);
        final IClusterConfig clusterConfig = pluginConfigService.getCluster(clusterName);
        if (clusterConfig == null) {
            throw new EditorException("No cluster found with name " + clusterName);
        }

        cluster = context.newCluster(clusterConfig, editorConfig);
        final IClusterConfig decorated = cluster.getClusterConfig();

        final String modelId = decorated.getString(RenderService.MODEL_ID);
        modelService = new ModelReference<>(modelId, getEditorModel());
        modelService.init(context);

        if (mode == Mode.COMPARE || mode == Mode.VIEW) {
            final String baseId = decorated.getString("model.compareTo");
            baseService = new ModelReference<>(baseId, getBaseModel());
            baseService.init(context);
        }

        final String editorId = decorated.getString("editor.id");
        context.registerService(this, editorId);
        context.registerService(editorContext.getEditorManager(), editorId);

        cluster.start();

        final IRenderService renderer = context
                .getService(decorated.getString(RenderService.WICKET_ID), IRenderService.class);
        if (renderer == null) {
            cluster.stop();
            context.unregisterService(this, editorId);
            context.unregisterService(editorContext.getEditorManager(), editorId);
            modelService.destroy();
            throw new EditorException("No IRenderService found");
        }

        final String renderId = getRendererServiceId();

        // attach self to renderer, so that other plugins can close us
        context.registerService(this, renderId);

        // observe focus events, those need to be synchronized with the active model of the editor manager
        focusListener = renderService -> editorContext.onFocus();
        context.registerService(focusListener, renderId);
    }

    public void stop() {
        final String renderId = getRendererServiceId();
        context.unregisterService(focusListener, renderId);
        context.unregisterService(this, renderId);

        cluster.stop();

        final String editorId = cluster.getClusterConfig().getString("editor.id");
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

    private class EditorActivatedHeaderItem extends HeaderItem {

        @Override
        public Iterable<?> getRenderTokens() {
            return Collections.singleton("editor-activated-header-item");
        }

        @Override
        public List<HeaderItem> getDependencies() {
            return Collections.singletonList(UsageStatisticsHeaderItem.get());
        }

        @Override
        public void render(final Response response) {
            final UsageEvent editorActivated = createUsageEvent(EVENT_EDITOR_ACTIVATED);
            final String eventJs = editorActivated.getJavaScript();
            OnLoadHeaderItem.forScript(eventJs).render(response);
        }
    }
}
