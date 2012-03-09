package org.hippoecm.frontend.plugins.cms.admin.users;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.breadcrumb.IBreadCrumbModel;
import org.apache.wicket.extensions.breadcrumb.panel.BreadCrumbPanel;
import org.apache.wicket.extensions.breadcrumb.panel.IBreadCrumbPanelFactory;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugins.cms.admin.widgets.AjaxLinkLabel;

/**
 */
public class ViewUserLinkLabel extends AjaxLinkLabel {

    private static final long serialVersionUID = 1L;
    private final BreadCrumbPanel panelToReplace;
    private final IModel<User> userModel;
    private final IPluginContext pluginContext;

    public ViewUserLinkLabel(final String id, final IModel<User> userModel, final BreadCrumbPanel panelToReplace,
                             final IPluginContext pluginContext) {
        super(id, new PropertyModel(userModel, "username"));
        this.panelToReplace = panelToReplace;
        this.userModel = userModel;
        this.pluginContext = pluginContext;
    }

    @Override
    public void onClick(final AjaxRequestTarget target) {
        panelToReplace.activate(new IBreadCrumbPanelFactory() {
            public BreadCrumbPanel create(final String componentId,
                                          final IBreadCrumbModel breadCrumbModel) {
                return new ViewUserPanel(componentId, pluginContext, breadCrumbModel, userModel);
            }
        });
    }


}
