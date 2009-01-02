package org.hippoecm.frontend.plugins.xinha.dialog;

import java.util.HashMap;
import java.util.Map;

import org.apache.wicket.Request;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugins.xinha.AbstractXinhaPlugin;

public abstract class XinhaDialogBehavior extends AbstractDefaultAjaxBehavior {
    private static final long serialVersionUID = 1L;

    protected final IPluginContext context;

    public XinhaDialogBehavior(IPluginContext context) {
        this.context = context;
    }

    protected IDialogService getDialogService() {
        return context.getService(IDialogService.class.getName(), IDialogService.class);
    }

    protected Map<String, String> getParameters() {
        Request request = RequestCycle.get().getRequest();
        HashMap<String, String> p = new HashMap<String, String>();
        Map<String, String> requestParams = request.getRequestParameters().getParameters();
        for (String key : requestParams.keySet()) {
            if (key.startsWith(AbstractXinhaPlugin.XINHA_PARAM_PREFIX)) {
                p.put(key.substring(AbstractXinhaPlugin.XINHA_PARAM_PREFIX.length()), request.getParameter(key));
            }
        }
        return p;
    }

}
