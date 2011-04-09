package org.hippoecm.frontend.plugins.cms.admin;

import org.apache.wicket.ResourceReference;
import org.apache.wicket.extensions.breadcrumb.panel.IBreadCrumbPanelFactory;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.plugin.IPlugin;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugins.cms.admin.crumbs.AdminBreadCrumbPanel;
import org.hippoecm.frontend.plugins.cms.admin.widgets.AjaxBreadCrumbPanelFactory;

public abstract class AdminPlugin extends AjaxBreadCrumbPanelFactory implements IPlugin, IBreadCrumbPanelFactory {

    public static final String ADMIN_PANEL_ID = "admin.panel";

    private final IPluginContext context;

    public AdminPlugin(IPluginContext context, Class<? extends AdminBreadCrumbPanel> panelClass) {
        super(context, panelClass);
        this.context = context;
    }

    public abstract ResourceReference getImage();

    public abstract IModel<String> getTitle();

    public abstract IModel<String> getHelp();

    @Override
    public void start() {
        context.registerService(this, ADMIN_PANEL_ID);
    }

    @Override
    public void stop() {
    }
}
