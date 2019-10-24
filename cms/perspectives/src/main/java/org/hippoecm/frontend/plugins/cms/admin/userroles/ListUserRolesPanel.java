/*
 *  Copyright 2019 Hippo B.V. (http://www.onehippo.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.frontend.plugins.cms.admin.userroles;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.extensions.breadcrumb.IBreadCrumbModel;
import org.apache.wicket.extensions.breadcrumb.IBreadCrumbParticipant;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugins.cms.admin.AdminBreadCrumbPanel;
import org.hippoecm.frontend.plugins.cms.admin.widgets.AdminDataTable;
import org.hippoecm.frontend.plugins.standards.panelperspective.breadcrumb.PanelPluginBreadCrumbLink;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoSession;
import org.onehippo.repository.security.SecurityConstants;

import com.bloomreach.xm.repository.security.UserRole;

/**
 * This panel displays a pageable list of userroles.
 */
public class ListUserRolesPanel extends AdminBreadCrumbPanel {

    private static final int NUMBER_OF_ITEMS_PER_PAGE = 20;

    /**
     * Constructs a new ListUserRolesPanel.
     *
     * @param id              the id
     * @param context         the context
     * @param breadCrumbModel the breadCrumbModel
     */
    public ListUserRolesPanel(final String id, final IPluginContext context, final IBreadCrumbModel breadCrumbModel) {
        super(id, breadCrumbModel);

        final HippoSession session = UserSession.get().getJcrSession();
        final boolean isSecurityApplManager = session.isUserInRole(
                SecurityConstants.USERROLE_SECURITY_APPLICATION_MANAGER);

        final PanelPluginBreadCrumbLink createLink =
                new PanelPluginBreadCrumbLink("create-userrole", breadCrumbModel) {
                    @Override
                    protected IBreadCrumbParticipant getParticipant(final String componentId) {
                        return new CreateUserRolePanel(componentId, breadCrumbModel);
                    }
                };
        createLink.setVisible(isSecurityApplManager);
        add(createLink);

        final List<IColumn<UserRole, String>> columns = new ArrayList<>();
        columns.add(new AbstractColumn<UserRole, String>(new ResourceModel("userrole-name"), "name") {
            @Override
            public void populateItem(final Item<ICellPopulator<UserRole>> cellItem, final String componentId,
                                     final IModel<UserRole> rowModel) {
                cellItem.add(new ViewUserRoleLinkLabel(componentId, rowModel, ListUserRolesPanel.this, context));
            }
        });
        columns.add(new PropertyColumn<>(new ResourceModel("userrole-system"), "system"));
        columns.add(new PropertyColumn<>(new ResourceModel("userrole-description"), "description"));

        final AdminDataTable<UserRole> table = new AdminDataTable<>("table", columns, new UserRoleDataProvider(),
                NUMBER_OF_ITEMS_PER_PAGE);
        add(table);
    }

    @Override
    public IModel<String> getTitle(final Component component) {
        return new ResourceModel("admin-userroles-title");
    }
}
