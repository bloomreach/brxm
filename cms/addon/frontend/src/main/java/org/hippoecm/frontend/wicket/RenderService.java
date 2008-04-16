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
package org.hippoecm.frontend.wicket;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RenderService extends Panel implements ServiceListener {
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(RenderService.class);

    private String wicketId;
    private PluginContext context;
    private Map<String, List<Serializable>> references;
    private ModelReference modelRef;

    public RenderService() {
        super("id");
        setOutputMarkupId(true);

        this.references = new HashMap<String, List<Serializable>>();

        this.modelRef = new ModelReference(this, "model");
    }

    @Override
    public String getId() {
        return wicketId;
    }

    protected void init(PluginContext context, String wicketId) {
        this.wicketId = wicketId;
        this.context = context;

        modelRef.init(context);
    }

    protected void destroy(PluginContext context) {
        modelRef.destroy();
    }

    // override model change methods

    @Override
    public void onModelChanged() {
        IRequestTarget target = RequestCycle.get().getRequestTarget();
        if (target != null) {
            if (target instanceof PluginRequestTarget) {
                PluginRequestTarget pluginTarget = (PluginRequestTarget) target;
                pluginTarget.addUpdate(this);
            } else {
                log.warn("Request target is not an instance of PluginRequestTarget, ajax update is cancelled");
            }
        }
        super.onModelChanged();
    }

    @Override
    public Component setModel(IModel model) {
        modelRef.setModel(model);
        return updateModel(model);
    }

    public Component updateModel(IModel model) {
        return super.setModel(model);
    }

    protected PluginContext getPluginContext() {
        return context;
    }

    protected void registerListener(String aggregationPoint) {
        context.registerListener(this, aggregationPoint);
    }

    protected void unregisterListener(String aggregationPoint) {
        context.unregisterListener(this, aggregationPoint);
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
    }

    protected void onServiceChanged(String name, Serializable service) {
    }

    protected void onServiceRemoved(String name, Serializable service) {
    }

    public Object resolvePath(String path) {
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

        RenderService service = (RenderService) list.get(idx);
        if (sep < 0) {
            return service;
        } else {
            if (service == null) {
                return null;
            }
            return service.resolvePath(path);
        }
    }
}
