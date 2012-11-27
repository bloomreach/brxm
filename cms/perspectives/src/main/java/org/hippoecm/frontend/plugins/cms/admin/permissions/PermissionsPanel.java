/*
 *  Copyright 2008-2012 Hippo.
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
import org.apache.wicket.extensions.breadcrumb.panel.BreadCrumbPanel;
import org.apache.wicket.extensions.breadcrumb.panel.IBreadCrumbPanelFactory;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.hippoecm.frontend.plugins.cms.admin.AdminBreadCrumbPanel;
import org.hippoecm.frontend.plugins.cms.admin.domains.Domain;
import org.hippoecm.frontend.plugins.cms.admin.domains.DomainDataProvider;
import org.hippoecm.frontend.plugins.cms.admin.groups.Group;
import org.hippoecm.frontend.plugins.cms.admin.users.User;
import org.hippoecm.frontend.plugins.cms.admin.widgets.AdminDataTable;
import org.hippoecm.frontend.plugins.cms.admin.widgets.AjaxLinkLabel;
import org.hippoecm.frontend.plugins.cms.admin.widgets.UserGroupListPanel;

public class PermissionsPanel extends AdminBreadCrumbPanel {


    private static final long serialVersionUID = 1L;

    /**
     * Visibility toggle so that either the link or the form is visible.
     */
    private boolean formVisible = false;

    public PermissionsPanel(final String id, final IBreadCrumbModel breadCrumbModel) {
        super(id, breadCrumbModel);
        setOutputMarkupId(true);

        List<String> roles = Group.getAllRoles();
        List<IColumn<Domain>> columns = new ArrayList<IColumn<Domain>>();

        columns.add(new AbstractColumn<Domain>(new ResourceModel("permissions-column-header"), "name") {
            private static final long serialVersionUID = 1L;

            public void populateItem(final Item<ICellPopulator<Domain>> item, final String componentId,
                                     final IModel<Domain> model) {

                AjaxLinkLabel action = new AjaxLinkLabel(componentId, new PropertyModel(model, "name")) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        activate(new IBreadCrumbPanelFactory() {
                            public BreadCrumbPanel create(String componentId,
                                                          IBreadCrumbModel breadCrumbModel) {
                                return new SetPermissionsPanel(componentId, breadCrumbModel, model);
                            }
                        });
                    }
                };
                item.add(action);
            }
        });

        for (final String role : roles) {
            columns.add(new AbstractColumn<Domain>(new Model<String>("Role: " + role)) {
                private static final long serialVersionUID = 1L;

                public void populateItem(Item<ICellPopulator<Domain>> cellItem, String componentId, IModel<Domain> model) {
                    Domain domain = model.getObject();
                    ArrayList<User> userList = new ArrayList<User>();
                    ArrayList<Group> groupList = new ArrayList<Group>();

                    Domain.AuthRole authRole = domain.getAuthRoles().get(role);

                    if (authRole != null) {
                        for (String userName : authRole.getUsernames()) {
                            User user = new User(userName);
                            userList.add(user);
                        }

                        for (String groupName : authRole.getGroupnames()) {
                            if (!Group.groupExists(groupName)) {
                                continue;
                            }
                            Group group = Group.forName(groupName);
                            groupList.add(group);
                        }
                    }

                    UserGroupListPanel listContainer = new UserGroupListPanel(componentId, userList, groupList);
                    cellItem.add(listContainer);
                }
            });
        }


        final AdminDataTable<Domain> table = new AdminDataTable<Domain>("table", columns, new DomainDataProvider(), 20) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return !formVisible;
            }
        };
        add(table);
    }

    public IModel<String> getTitle(Component component) {
        return new ResourceModel("admin-permissions-title");
    }

}
