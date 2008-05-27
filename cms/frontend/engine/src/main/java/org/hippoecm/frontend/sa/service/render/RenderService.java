/*
 * Copyright 2008 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.frontend.sa.service.render;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.sa.dialog.IDialogService;
import org.hippoecm.frontend.sa.plugin.IPlugin;
import org.hippoecm.frontend.sa.plugin.IPluginContext;
import org.hippoecm.frontend.sa.plugin.config.IPluginConfig;
import org.hippoecm.frontend.sa.service.IRenderService;
import org.hippoecm.frontend.sa.service.PluginRequestTarget;
import org.hippoecm.frontend.sa.service.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class RenderService extends Panel implements ModelReference.IView, IRenderService {
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(RenderService.class);

    public static final String WICKET_ID = "wicket.id";
    public static final String MODEL_ID = "wicket.model";
    public static final String DIALOG_ID = "wicket.dialog";

    private boolean redraw;
    private String serviceId;
    private String wicketServiceId;
    private String wicketId;

    private IPluginContext context;
    private IPluginConfig config;
    private Map<String, ExtensionPoint> children;
    private ModelReference modelRef;
    private IRenderService parent;

    public RenderService(IPluginContext context, IPluginConfig properties) {
        super("id");

        this.context = context;
        this.config = properties;

        setOutputMarkupId(true);
        redraw = false;

        wicketId = "service.render";

        this.children = new HashMap<String, ExtensionPoint>();

        if (properties.getString(WICKET_ID) != null) {
            this.wicketServiceId = properties.getString(WICKET_ID);
        } else {
            log.warn("No service id ({}) defined", WICKET_ID);
        }

        if (properties.getString(MODEL_ID) != null) {
            String modelId = properties.getString(MODEL_ID);
            if (modelId != null) {
                this.modelRef = new ModelReference(modelId, this);

                modelRef.init(context);
            }
        } else {
            log.warn("No model ({}) defined for service {}", MODEL_ID, wicketServiceId);
        }

        if (config.getString(IPlugin.SERVICE_ID) != null) {
            serviceId = config.getString(IPlugin.SERVICE_ID);
            context.registerService(this, serviceId);
        } else {
            log.warn("No unique service id ({}) defined", IPlugin.SERVICE_ID);
        }

        context.registerService(this, wicketServiceId);
    }

    // override model change methods

    @Override
    public void onModelChanged() {
        super.onModelChanged();
        redraw();
    }

    @Override
    public Component setModel(IModel model) {
        if (modelRef != null) {
            modelRef.setModel(model);
        }
        updateModel(model);
        return this;
    }

    public void updateModel(IModel model) {
        super.setModel(model);
    }

    // utility routines for subclasses

    protected IPluginContext getPluginContext() {
        return context;
    }

    protected IPluginConfig getPluginConfig() {
        return config;
    }

    protected Object getProperty(String key) {
        return config.get(key);
    }

    protected void redraw() {
        redraw = true;
    }

    protected void addExtensionPoint(final String extension) {
        ExtensionPoint extPt = new ExtensionPoint(extension);
        children.put(extension, extPt);
        context.registerTracker(extPt, config.getString(extension));
        add(new EmptyPanel(extension));
    }

    protected void removeExtensionPoint(String name) {
        context.unregisterTracker(children.get(name), config.getString(name));
        children.remove(name);
        replace(new EmptyPanel(name));
    }

    protected IDialogService getDialogService() {
        return context.getService(config.getString(DIALOG_ID));
    }

    // implement IRenderService

    public void render(PluginRequestTarget target) {
        if (redraw) {
            PluginRequestTarget pluginTarget = (PluginRequestTarget) target;
            pluginTarget.addComponent(this);
            redraw = false;
        }
        for (Map.Entry<String, ExtensionPoint> entry : children.entrySet()) {
            for (IRenderService service : entry.getValue().getChildren()) {
                service.render(target);
            }
        }
    }

    public void focus(IRenderService child) {
        IRenderService parent = getParentService();
        if (parent != null) {
            parent.focus(this);
        }
    }

    @Override
    public String getId() {
        return wicketId;
    }

    public void bind(IRenderService parent, String wicketId) {
        this.parent = parent;
        this.wicketId = wicketId;
    }

    public void unbind() {
        this.parent = null;
        wicketId = "service.render.unbound";
    }

    public IRenderService getParentService() {
        return parent;
    }

    public String getServiceId() {
        return serviceId;
    }

    private class ExtensionPoint extends ServiceTracker<IRenderService> {
        private static final long serialVersionUID = 1L;

        private List<IRenderService> list;
        private String extension;

        ExtensionPoint(String extension) {
            super(IRenderService.class);
            this.extension = extension;
            this.list = new LinkedList<IRenderService>();
        }

        List<IRenderService> getChildren() {
            return list;
        }

        public void onServiceAdded(IRenderService service, String name) {
            service.bind(RenderService.this, extension);
            replace((Component) service);
            list.add(service);
        }

        public void onServiceChanged(IRenderService service, String name) {
        }

        public void onRemoveService(IRenderService service, String name) {
            replace(new EmptyPanel(extension));
            service.unbind();
            list.remove(service);
        }

    }
}
