package org.hippoecm.frontend.plugins.yui;

import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.yui.header.IYuiContext;
import org.hippoecm.frontend.plugins.yui.layout.IYuiManager;

public abstract class AbstractYuiAjaxBehavior extends AbstractDefaultAjaxBehavior {
    private static final long serialVersionUID = 1L;
    
    private IYuiContext _helper;

    public AbstractYuiAjaxBehavior(IPluginContext context, IPluginConfig config) {
        this(context.getService("service.behavior.yui", IYuiManager.class));
    }

    public AbstractYuiAjaxBehavior(IYuiManager service) {
        if (service == null) {
            throw new IllegalStateException("No root yui behavior found, unable to register module dependencies.");
        }
        _helper = service.newContext();
    }
    
    @Override
    protected void onBind() {
        super.onBind();
        addHeaderContribution(_helper);
    }
    
    /**
     * Override to implement header contrib
     * @param helper
     */
    public void addHeaderContribution(IYuiContext helper) {
    }

    
    /**
     * Don't call super since WicketAjax is loaded by Yui webapp behavior
     * TODO: webapp ajax is configurable, maybe check here and still load it. 
     */
    @Override
    public void renderHead(IHeaderResponse response) {
        _helper.renderHead(response);
    }
    
}
