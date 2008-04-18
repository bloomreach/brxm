package org.hippoecm.frontend.plugin.editor;

import java.io.Serializable;

import org.hippoecm.frontend.core.Plugin;
import org.hippoecm.frontend.core.PluginContext;
import org.hippoecm.frontend.plugin.render.RenderPlugin;
import org.hippoecm.frontend.service.IDynamicService;
import org.hippoecm.frontend.service.IFactoryService;
import org.hippoecm.frontend.service.editor.EditorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EditorPlugin extends EditorService implements Plugin, IDynamicService {
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(EditorPlugin.class);

    private String factoryId;
    private IFactoryService factory;

    public void start(PluginContext context) {
        factoryId = context.getProperty(Plugin.FACTORY_ID);
        init(context, context.getProperty(Plugin.SERVICE_ID), context.getProperty(RenderPlugin.PARENT_ID), context
                .getProperty(RenderPlugin.WICKET_ID), context.getProperty(RenderPlugin.MODEL_ID));

        if (factoryId != null) {
            context.registerListener(this, factoryId);
        }
    }

    public void stop() {
        if (factoryId != null) {
            PluginContext context = getPluginContext();
            context.unregisterListener(this, factoryId);
        }

        destroy();
    }

    public void delete() {
        if (factory != null) {
            factory.delete(this);
        }
    }

    @Override
    protected void onServiceAdded(String name, Serializable service) {
        if (factoryId != null && factoryId.equals(name)) {
            if (service instanceof IFactoryService) {
                factory = (IFactoryService) service;
            } else {
                log.error("registering factory is not a MultiEditorPlugin");
            }
        }
        super.onServiceAdded(name, service);
    }

    @Override
    protected void onServiceRemoved(String name, Serializable service) {
        if (service == factory) {
            factory = null;
        }
        super.onServiceRemoved(name, service);
    }
}
