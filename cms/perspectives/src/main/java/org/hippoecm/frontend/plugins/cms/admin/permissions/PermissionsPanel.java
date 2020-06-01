/*
 *  Copyright 2008-2019 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.cms.admin.permissions;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.breadcrumb.IBreadCrumbModel;
import org.apache.wicket.extensions.breadcrumb.panel.IBreadCrumbPanelFactory;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;

import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugins.cms.admin.AdminBreadCrumbPanel;
import org.hippoecm.frontend.plugins.cms.admin.domains.Domain;
import org.hippoecm.frontend.plugins.cms.admin.domains.DomainDataProvider;
import org.hippoecm.frontend.plugins.cms.admin.groups.Group;
import org.hippoecm.frontend.plugins.cms.admin.users.User;
import org.hippoecm.frontend.plugins.cms.admin.widgets.AdminDataTable;
import org.hippoecm.frontend.plugins.cms.admin.widgets.AjaxLinkLabel;
import org.hippoecm.frontend.plugins.cms.admin.widgets.UserGroupListPanel;

public class PermissionsPanel extends AdminBreadCrumbPanel {

    public PermissionsPanel(final String id, final IBreadCrumbModel breadCrumbModel, final IPluginContext pluginContext) {
        super(id, breadCrumbModel);
        setOutputMarkupId(true);

        final List<IColumn<Domain, String>> columns = new ArrayList<>();

        columns.add(new AbstractColumn<Domain, String>(new ResourceModel("permissions-column-header"), "name") {

            public void populateItem(final Item<ICellPopulator<Domain>> item, final String componentId,
                                     final IModel<Domain> model) {

                final AjaxLinkLabel action = new AjaxLinkLabel(componentId, PropertyModel.of(model, "name")) {
                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        activate((IBreadCrumbPanelFactory) (componentId1, breadCrumbModel1) -> new SetPermissionsPanel(componentId1, breadCrumbModel1, model));
                    }
                };
                item.add(action);
            }
        });

        final List<String> roles = Group.getAllRoles();
        for (final String role : roles) {
            columns.add(new AbstractColumn<Domain, String>(new Model<>("Role: " + role)) {
                public void populateItem(final Item<ICellPopulator<Domain>> cellItem, final String componentId, final IModel<Domain> model) {
                    final Domain domain = model.getObject();
                    final List<User> userList = new ArrayList<>();
                    final List<Group> groupList = new ArrayList<>();

                    final Domain.AuthRole authRole = domain.getAuthRoles().get(role);

                    if (authRole != null) {
                        for (final String userName : authRole.getUsernames()) {
                            final User user = new User(userName);
                            userList.add(user);
                        }

                        for (final String groupName : authRole.getGroupnames()) {
                            final Group group = Group.getGroup(groupName);
                            if (group == null) {
                                continue;
                            }
                            groupList.add(group);
                        }
                    }

                    final UserGroupListPanel listContainer = new UserGroupListPanel(componentId, userList, groupList,
                            pluginContext);
                    cellItem.add(listContainer);
                }
            });
        }

        final AdminDataTable<Domain> table = new AdminDataTable<Domain>("table", columns, new DomainDataProvider(), 20);
        add(table);
    }

    public IModel<String> getTitle(final Component component) {
        return new ResourceModel("admin-permissions-title");
    }

}
