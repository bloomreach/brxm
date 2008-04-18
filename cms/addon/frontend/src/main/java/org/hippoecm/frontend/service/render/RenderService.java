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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.wicket.Component;
import org.apache.wicket.IRequestTarget;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.core.PluginContext;
import org.hippoecm.frontend.core.ServiceListener;
import org.hippoecm.frontend.service.IRenderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RenderService extends Panel implements ServiceListener, ModelReference.IListener, IRenderService {
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(RenderService.class);

    private String serviceId;
    private String wicketId;
    private String parentId;
    private PluginContext context;
    private Map<String, List<Serializable>> references;
    private ModelReference modelRef;
    private IRenderService parent;

    public RenderService() {
        super("id");
        setOutputMarkupId(true);

        this.references = new HashMap<String, List<Serializable>>();
    }

    @Override
    public String getId() {
        return wicketId;
    }

    protected void init(PluginContext context, String serviceId, String parentId, String wicketId, String modelId) {
        this.serviceId = serviceId;
        this.parentId = parentId;
        this.wicketId = wicketId;
        this.context = context;

        if (modelId != null) {
            this.modelRef = new ModelReference(this, modelId);

            modelRef.init(context);
        }

        context.registerService(this, serviceId);
        context.registerListener(this, parentId);
    }

    public void destroy() {
        PluginContext context = getPluginContext();
        context.unregisterListener(this, parentId);
        context.unregisterService(this, serviceId);

        if (modelRef != null) {
            modelRef.destroy();
            modelRef = null;
        }
    }

    protected PluginContext getPluginContext() {
        return context;
    }

    // override model change methods

    @Override
    public void onModelChanged() {
        super.onModelChanged();
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

    protected void redraw() {
        IRequestTarget target = RequestCycle.get().getRequestTarget();
        if (target != null) {
            if (target instanceof PluginRequestTarget) {
                PluginRequestTarget pluginTarget = (PluginRequestTarget) target;
                pluginTarget.addUpdate(this);
            } else {
                log.warn("Request target is not an instance of PluginRequestTarget, ajax update is cancelled");
            }
        }
    }

    protected void registerListener(String serviceName) {
        context.registerListener(this, context.getProperty(serviceName));
    }

    protected void unregisterListener(String serviceName) {
        context.unregisterListener(this, context.getProperty(serviceName));
    }

    public final void processEvent(int type, String name, Serializable service) {
        List<Serializable> list = references.get(name);
        switch (type) {
        case ServiceListener.ADDED:
            if (list == null) {
                list = new LinkedList<Serializable>();
                references.put(name, list);
            }
            list.add(service);
            onServiceAdded(name, service);
            break;

        case ServiceListener.CHANGED:
            onServiceChanged(name, service);
            break;

        case ServiceListener.REMOVED:
            list.remove(service);
            if (list.isEmpty()) {
                references.put(name, null);
            }
            onServiceRemoved(name, service);
            break;
        }
    }

    protected void onServiceAdded(String name, Serializable service) {
        if (parentId != null && parentId.equals(name)) {
            if (service instanceof RenderService) {
                parent = (RenderService) service;
            } else {
                log.error("parent service is not a RenderService");
            }
        }
    }

    protected void onServiceChanged(String name, Serializable service) {
    }

    protected void onServiceRemoved(String name, Serializable service) {
        if (parentId != null && parentId.equals(name)) {
            if (parent == service) {
                parent = null;
            } else {
                log.error("unrecognised parent service");
            }
        }
    }

    public void focus(IRenderService child) {
        if (parent != null) {
            parent.focus(this);
        }
    }

    public String getPath(IRenderService child) {
        StringBuilder sb = new StringBuilder();
        if (parent != null) {
            sb.append(parent.getPath(this));
            sb.append(':');
        }

        for (Map.Entry<String, List<Serializable>> entry : references.entrySet()) {
            List<Serializable> list = entry.getValue();
            if (list.contains(child)) {
                sb.append(entry.getKey());
                sb.append(':');
                sb.append(list.indexOf(child));
                break;
            }
        }
        return sb.toString();
    }

    public IRenderService resolvePath(String path) {
        int sep = path.indexOf(':');
        String name = path.substring(0, sep);
        path = path.substring(sep + 1);

        List<Serializable> list = references.get(name);
        if (list == null) {
            return null;
        }

        sep = path.indexOf(':');
        int idx;
        if (sep < 0) {
            idx = Integer.valueOf(path);
        } else {
            idx = Integer.valueOf(path.substring(0, sep));
            path = path.substring(sep + 1);
        }

        if (list.get(idx) instanceof IRenderService) {
            IRenderService service = (IRenderService) list.get(idx);
            if (sep < 0) {
                return service;
            } else {
                if (service == null) {
                    return null;
                }
                return service.resolvePath(path);
            }
        } else {
            log.warn("Service found under path " + path + " is not a RenderService.");
            return null;
        }
    }
}
