package org.hippoecm.frontend.plugins.xinha.dialog;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.apache.wicket.Request;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.xinha.AbstractXinhaPlugin;

public abstract class XinhaDialogBehavior extends DialogBehavior {
    private static final long serialVersionUID = 1L;

    public XinhaDialogBehavior(IPluginContext context, IPluginConfig config, String serviceId) {
        super(context, config, serviceId);
    }
    
    @Override
    protected void configureModal(final ModalWindow modal) {
        modal.setResizable(false);
        modal.setCookieName("XinhaDialog" + getId());
    }

    @Override
    protected String createTitle() {
        return new StringResourceModel(getId() + "-dialog-title", getComponent(), null).getString();
    }

    @Override
    protected Serializable newDialogModelObject() {
        Request request = RequestCycle.get().getRequest();
        HashMap<String, String> p = new HashMap<String, String>();
        Map<String, String> requestParams = request.getRequestParameters().getParameters();
        for (String key : requestParams.keySet()) {
            if (key.startsWith(AbstractXinhaPlugin.XINHA_PARAM_PREFIX)) {
                p.put(key.substring(AbstractXinhaPlugin.XINHA_PARAM_PREFIX.length()), request.getParameter(key));
            }
        }
        return newDialogModelObject(p);
    }

    public String onDialogOk() {
        IModel model = modelService.getModel();
        JsBean bean = (JsBean) model.getObject();
        onOk(bean);
        return bean.toJsString();
    }

    @Override
    protected String getServiceId() {
        return "cms-pickers/" + getId();
    }
    
    protected abstract JsBean newDialogModelObject(HashMap<String, String> p);

    protected abstract void onOk(JsBean bean);

    protected abstract String getId();

}
