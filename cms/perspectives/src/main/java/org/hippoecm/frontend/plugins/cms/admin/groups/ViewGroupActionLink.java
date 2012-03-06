package org.hippoecm.frontend.plugins.cms.admin.groups;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.breadcrumb.IBreadCrumbModel;
import org.apache.wicket.extensions.breadcrumb.panel.BreadCrumbPanel;
import org.apache.wicket.extensions.breadcrumb.panel.IBreadCrumbPanelFactory;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugins.cms.admin.groups.Group;
import org.hippoecm.frontend.plugins.cms.admin.groups.ViewGroupPanel;
import org.hippoecm.frontend.plugins.cms.admin.widgets.AjaxLinkLabelContainer;

public class ViewGroupActionLink extends AjaxLinkLabelContainer {
    private final IModel<Group> groupModel;
    private final IPluginContext context;
    private final BreadCrumbPanel breadCrumbPanel;

    private static final long serialVersionUID = 1L;

    public ViewGroupActionLink(final String id, final IModel labelTextModel, final IModel<Group> groupModel,
                               final IPluginContext context, final BreadCrumbPanel breadCrumbPanel) {
        super(id, labelTextModel);

        this.groupModel = groupModel;
        this.context = context;
        this.breadCrumbPanel = breadCrumbPanel;
    }

    @Override
    public void onClick(AjaxRequestTarget target) {
        breadCrumbPanel.activate(new IBreadCrumbPanelFactory() {
            public BreadCrumbPanel create(String componentId, IBreadCrumbModel breadCrumbModel) {
                return new ViewGroupPanel(componentId, context, breadCrumbModel, groupModel);
            }
        });
    }
}