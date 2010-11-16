/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.frontend.plugins.cms.admin.users;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.breadcrumb.IBreadCrumbModel;
import org.apache.wicket.extensions.breadcrumb.panel.BreadCrumbPanel;
import org.apache.wicket.extensions.breadcrumb.panel.IBreadCrumbPanelFactory;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugins.cms.admin.crumbs.AdminBreadCrumbPanel;
import org.hippoecm.frontend.plugins.cms.admin.widgets.AdminDataTable;
import org.hippoecm.frontend.plugins.cms.admin.widgets.AjaxBreadCrumbPanelLink;
import org.hippoecm.frontend.plugins.cms.admin.widgets.AjaxLinkLabel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This panel displays a pageable list of users.
 */
public class ListUsersPanel extends AdminBreadCrumbPanel {
    @SuppressWarnings("unused")
    private static final String SVN_ID = "$Id$";
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(ListUsersPanel.class);

    private final UserDataProvider userDataProvider = new UserDataProvider();
    private final AdminDataTable table;


    public ListUsersPanel(final String id, final IPluginContext context, final IBreadCrumbModel breadCrumbModel) {
        super(id, breadCrumbModel);
        setOutputMarkupId(true);

        add(new AjaxBreadCrumbPanelLink("create-user", context, this, CreateUserPanel.class));
        
        List<IColumn> columns = new ArrayList<IColumn>();

        columns.add(new AbstractColumn(new ResourceModel("user-username"), "username") {
            private static final long serialVersionUID = 1L;

            public void populateItem(final Item item, final String componentId, final IModel model) {
                
                AjaxLinkLabel action = new AjaxLinkLabel(componentId, new PropertyModel(model, "username")) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        //panel.showView(target, model);
                        activate(new IBreadCrumbPanelFactory()
                        {
                            public BreadCrumbPanel create(String componentId,
                                    IBreadCrumbModel breadCrumbModel)
                            {
                                return new ViewUserPanel(componentId, context, breadCrumbModel, model);
                            }
                        });
                    }
                };
                item.add(action);
            }
        });

        columns.add(new PropertyColumn(new ResourceModel("user-firstname"), "frontend:firstname", "firstName"));
        columns.add(new PropertyColumn(new ResourceModel("user-lastname"), "frontend:lastname", "lastName"));
        columns.add(new PropertyColumn(new ResourceModel("user-email"), "frontend:email", "email"));
        columns.add(new AbstractColumn(new Model("Type")) {
            private static final long serialVersionUID = 1L;

            public void populateItem(Item cellItem, String componentId, IModel model) {
                if (((User) model.getObject()).isExternal()) {
                    cellItem.add(new Label(componentId, "external"));
                } else {
                    cellItem.add(new Label(componentId, "repository"));
                }
            }
        });

        table = new AdminDataTable("table", columns, userDataProvider, 20);
        table.setOutputMarkupId(true);
        add(table);
    }

    public IModel getTitle(Component component) {
        return new StringResourceModel("admin-users-title", component, null);
    }
}
