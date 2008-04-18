package org.hippoecm.frontend.plugin.editor;

import java.io.Serializable;
import java.util.List;

import org.hippoecm.frontend.core.Plugin;
import org.hippoecm.frontend.core.PluginContext;
import org.hippoecm.frontend.plugin.render.RenderPlugin;
import org.hippoecm.frontend.service.IDynamicService;
import org.hippoecm.frontend.service.IFactoryService;
import org.hippoecm.frontend.service.editor.EditorService;
import org.hippoecm.frontend.util.ServiceTracker;

public class EditorPlugin extends EditorService implements Plugin, IDynamicService {
    private static final long serialVersionUID = 1L;

    private ServiceTracker factory;

    public EditorPlugin() {
        factory = new ServiceTracker(IFactoryService.class);
    }

    public void start(PluginContext context) {
        factory.open(context, context.getProperty(Plugin.FACTORY_ID));
        init(context, context.getProperty(Plugin.SERVICE_ID), context.getProperty(RenderPlugin.PARENT_ID), context
                .getProperty(RenderPlugin.WICKET_ID), context.getProperty(RenderPlugin.MODEL_ID));
    }

    public void stop() {
        destroy();
        factory.close();
    }

    public void delete() {
        List<Serializable> services = factory.getServices();
        if (services.size() == 1) {
            IFactoryService service = (IFactoryService) services.get(0);
            service.delete(this);
        }
    }

}
