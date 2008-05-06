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
package org.hippoecm.frontend.service.render;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.application.PluginRequestTarget;
import org.hippoecm.frontend.core.PluginContext;
import org.hippoecm.frontend.plugin.parameters.ParameterValue;
import org.hippoecm.frontend.service.IDialogService;
import org.hippoecm.frontend.service.IRenderService;
import org.hippoecm.frontend.util.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RenderService extends Panel implements ModelReference.IView, IRenderService {
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(RenderService.class);

    public static final String MODEL_ID = "wicket.model";
    public static final String DIALOG_ID = "wicket.dialog";
    public static final String DECORATOR_ID = "wicket.decorator";

    private boolean redraw;
    private String serviceId;
    private String wicketId;
    private PluginContext context;
    private Map<String, ParameterValue> properties;
    private Map<String, ServiceTracker<IRenderService>> children;
    private ModelReference modelRef;
    private IRenderService parent;
    private ServiceTracker dialogTracker;

    public RenderService() {
        super("id");
        setOutputMarkupId(true);
        redraw = false;

        wicketId = "service.render";

        this.children = new HashMap<String, ServiceTracker<IRenderService>>();
        this.dialogTracker = new ServiceTracker(IDialogService.class);
    }

    protected void init(PluginContext context, String serviceId, Map<String, ParameterValue> properties) {
        this.context = context;
        this.properties = properties;
        this.serviceId = serviceId;

        if (properties.get(MODEL_ID) != null) {
            String modelId = properties.get(MODEL_ID).getStrings().get(0);
            if (modelId != null) {
                this.modelRef = new ModelReference(modelId, this);

                modelRef.init(context);
            }
        } else {
            log.warn("No model ({}) defined for service {}", MODEL_ID, serviceId);
        }

        if (properties.get(DIALOG_ID) != null) {
            dialogTracker.open(context, properties.get(DIALOG_ID).getStrings().get(0));
        } else {
            log.warn("No dialog service ({}) defined for service {}", DIALOG_ID, serviceId);
        }

        for (Map.Entry<String, ServiceTracker<IRenderService>> entry : children.entrySet()) {
            entry.getValue().open(context, properties.get(entry.getKey()).getStrings().get(0));
        }
        context.registerService(this, serviceId);
    }

    protected void destroy() {
        PluginContext context = getPluginContext();

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
        modelRef.setModel(model);
        updateModel(model);
        return this;
    }

    public void updateModel(IModel model) {
        super.setModel(model);
    }

    // utility routines for subclasses

    protected PluginContext getPluginContext() {
        return context;
    }

    protected Object getProperty(String key) {
        return properties.get(key);
    }

    protected void redraw() {
        redraw = true;
    }

    protected void addExtensionPoint(String name, ServiceTracker.IListener<IRenderService> listener) {
        ServiceTracker<IRenderService> tracker = new ServiceTracker<IRenderService>(IRenderService.class);
        tracker.addListener(listener);
        children.put(name, tracker);
        add(new EmptyPanel(name));
    }

    protected void addExtensionPoint(final String extension) {
        addExtensionPoint(extension, new ServiceTracker.IListener() {
            private static final long serialVersionUID = 1L;

            public void onServiceAdded(String name, Serializable service) {
                ((IRenderService) service).bind(RenderService.this, extension);
                replace((Component) service);
            }

            public void onServiceChanged(String name, Serializable service) {
            }

            public void onRemoveService(String name, Serializable service) {
                replace(new EmptyPanel(extension));
                ((IRenderService) service).unbind();
            }
        });
    }

    protected void removeExtensionPoint(String name) {
        children.remove(name);
        replace(new EmptyPanel(name));
    }

    protected IDialogService getDialogService() {
        List<Serializable> dialogs = dialogTracker.getServices();
        if (dialogs.size() > 0) {
            return (IDialogService) dialogs.get(0);
        }
        return null;
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

    public List<? extends IRenderService> getChildServices(String name) {
        ServiceTracker tracker = children.get(name);
        if (tracker != null) {
            return tracker.getServices();
        } else {
            return new ArrayList<IRenderService>();
        }
    }

    public List<String> getExtensionPoints() {
        List<String> result = new LinkedList<String>();
        result.addAll(children.keySet());
        return result;
    }

    public String getDecoratorId() {
        if (properties.get(DECORATOR_ID) != null) {
            return properties.get(DECORATOR_ID).getStrings().get(0);
        } else {
            log.warn("No decorator id ({}) defined for service {}", DECORATOR_ID, serviceId);
            return null;
        }
    }

}
