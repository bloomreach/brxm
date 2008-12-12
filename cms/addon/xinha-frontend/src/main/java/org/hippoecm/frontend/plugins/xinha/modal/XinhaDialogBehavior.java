package org.hippoecm.frontend.plugins.xinha.modal;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.apache.wicket.Request;
import org.apache.wicket.RequestCycle;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.xinha.AbstractXinhaPlugin;

public abstract class XinhaDialogBehavior<K extends Enum<K>> extends DialogBehavior {
    private static final long serialVersionUID = 1L;

    protected XinhaDialogService<K> xds;
    
    public XinhaDialogBehavior(IPluginContext context, IPluginConfig config, String modalWindowServiceId) {
        super(context, config, modalWindowServiceId);
        
        xds = createXinhaDialogService();
    }

    @Override
    protected String createTitle() {
        Request request = RequestCycle.get().getRequest();
        String pluginName = request.getParameter("pluginName");
        return "XinhaDialog[" + pluginName + "]";
    }

    @Override
    protected Serializable getDialogModelObject() {
        Request request = RequestCycle.get().getRequest();
        Map<String, String> params = new HashMap<String, String>();
        Map<String, String> requestParams = request.getRequestParameters().getParameters();
        for (String key : requestParams.keySet()) {
            if (key.startsWith(AbstractXinhaPlugin.XINHA_PARAM_PREFIX)) {
                params.put(key.substring(AbstractXinhaPlugin.XINHA_PARAM_PREFIX.length()), request.getParameter(key));
            }
        }
        xds.update(params);
        return xds;
    }

    
    protected abstract XinhaDialogService<K> createXinhaDialogService();

}
