package org.hippoecm.frontend.service;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.hippoecm.frontend.core.PluginContext;
import org.hippoecm.frontend.core.ServiceListener;

public class RenderService extends Panel implements ServiceListener {
    private static final long serialVersionUID = 1L;

    private String wicketId;
    private PluginContext context;
    private Map<String, List<Serializable>> references;

    public RenderService() {
        super("id");

        this.references = new HashMap<String, List<Serializable>>();
    }

    @Override
    public String getId() {
        return wicketId;
    }

    protected void init(PluginContext context, String wicketId) {
        this.wicketId = wicketId;
        this.context = context;
    }

    protected void destroy(PluginContext context) {
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

    protected IDataProvider getChildRenderers(String name) {
        return new ListDataProvider(references.get(name));
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
