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
import java.util.Map;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.sa.PluginRequestTarget;
import org.hippoecm.frontend.sa.core.IPlugin;
import org.hippoecm.frontend.sa.core.IPluginConfig;
import org.hippoecm.frontend.sa.core.IPluginContext;
import org.hippoecm.frontend.sa.service.IDialogService;
import org.hippoecm.frontend.sa.service.IRenderService;
import org.hippoecm.frontend.sa.util.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RenderService extends Panel implements ModelReference.IView, IRenderService {
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(RenderService.class);

    public static final String WICKET_ID = "wicket.id";
    public static final String MODEL_ID = "wicket.model";
    public static final String DIALOG_ID = "wicket.dialog";

    private boolean redraw;
    private String serviceId;
    private String wicketId;

    private IPluginContext context;
    private IPluginConfig config;
    private Map<String, ServiceTracker<IRenderService>> children;
    private ModelReference modelRef;
    private IRenderService parent;
    private ServiceTracker<IDialogService> dialogTracker;

    public RenderService() {
        super("id");
        setOutputMarkupId(true);
        redraw = false;

        wicketId = "service.render";

        this.children = new HashMap<String, ServiceTracker<IRenderService>>();
        this.dialogTracker = new ServiceTracker<IDialogService>(IDialogService.class);
    }

    protected void init(IPluginContext context, IPluginConfig properties) {
        this.context = context;
        this.config = properties;

        if (properties.getString(WICKET_ID) != null) {
            this.serviceId = properties.getString(WICKET_ID);
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
            log.warn("No model ({}) defined for service {}", MODEL_ID, serviceId);
        }

        if (properties.getString(DIALOG_ID) != null) {
            dialogTracker.open(context, properties.getString(DIALOG_ID));
        } else {
            log.warn("No dialog service ({}) defined for service {}", DIALOG_ID, serviceId);
        }

        for (Map.Entry<String, ServiceTracker<IRenderService>> entry : children.entrySet()) {
            entry.getValue().open(context, properties.getString(entry.getKey()));
        }
        context.registerService(this, serviceId);
    }

    protected void destroy() {
        IPluginContext context = getPluginContext();

        context.unregisterService(this, serviceId);
        for (Map.Entry<String, ServiceTracker<IRenderService>> entry : children.entrySet()) {
            entry.getValue().close();
        }

        dialogTracker.close();

        if (modelRef != null) {
            modelRef.destroy();
            modelRef = null;
        }
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

    protected Object getProperty(String key) {
        return config.get(key);
    }

    protected void redraw() {
        redraw = true;
    }

    protected void addExtensionPoint(final String extension) {
        ServiceTracker<IRenderService> tracker = new ServiceTracker<IRenderService>(IRenderService.class);
        tracker.addListener(new ServiceTracker.IListener<IRenderService>() {
            private static final long serialVersionUID = 1L;

            public void onServiceAdded(String name, IRenderService service) {
                service.bind(RenderService.this, extension);
                replace((Component) service);
            }

            public void onServiceChanged(String name, IRenderService service) {
            }

            public void onRemoveService(String name, IRenderService service) {
                replace(new EmptyPanel(extension));
                service.unbind();
            }
        });
        children.put(extension, tracker);
        add(new EmptyPanel(extension));
    }

    protected void removeExtensionPoint(String name) {
        children.remove(name);
        replace(new EmptyPanel(name));
    }

    protected IDialogService getDialogService() {
        return dialogTracker.getService();
    }

    // implement IRenderService

    public void render(PluginRequestTarget target) {
        if (redraw) {
            PluginRequestTarget pluginTarget = (PluginRequestTarget) target;
            pluginTarget.addComponent(this);
            redraw = false;
        }
        for (Map.Entry<String, ServiceTracker<IRenderService>> entry : children.entrySet()) {
            for (IRenderService service : entry.getValue().getServices()) {
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
        if (config.getString(IPlugin.SERVICE_ID) != null) {
            return config.getString(IPlugin.SERVICE_ID);
        } else {
            log.warn("No decorator id ({}) defined", IPlugin.SERVICE_ID);
            return null;
        }
    }

}
