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
package org.hippoecm.frontend.plugins.cms.admin.permissions;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.extensions.breadcrumb.IBreadCrumbModel;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.basic.MultiLineLabel;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugins.cms.admin.crumbs.AdminBreadCrumbPanel;
import org.hippoecm.frontend.plugins.cms.admin.domains.Domain;
import org.hippoecm.frontend.plugins.cms.admin.domains.DomainDataProvider;
import org.hippoecm.frontend.plugins.cms.admin.widgets.AdminDataTable;

public class PermissionsPanel extends AdminBreadCrumbPanel {
    
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;
    
    private AdminDataTable table;

    /** Visibility toggle so that either the link or the form is visible. */
    private boolean formVisible = false;

    public PermissionsPanel(final String id, final IPluginContext context, final IBreadCrumbModel breadCrumbModel) {
        super(id, breadCrumbModel);
        setOutputMarkupId(true);

        // add feedback panel to show errors
        FeedbackPanel feedback = new FeedbackPanel("feedback");
        feedback.setOutputMarkupId(true);
        add(feedback);
        
        List<IColumn> columns = new ArrayList<IColumn>();

        //        columns.add(new AbstractColumn(new Model("Actions"))
        //        {
        //            public void populateItem(Item cellItem, String componentId,
        //                IModel model)
        //            {
        //                cellItem.add(new ActionPanel(componentId, model));
        //            }
        //        });

        columns.add(new PropertyColumn(new Model("Name"), "name"));
        columns.add(new PropertyColumn(new Model("Description"), "description"));
        columns.add(new AbstractColumn(new Model("Roles")) {
            private static final long serialVersionUID = 1L;

            public void populateItem(Item cellItem, String componentId, IModel model) {
                Domain domain = (Domain) model.getObject();
                StringBuilder sb = new StringBuilder();
                for (Domain.AuthRole authRole : domain.getAuthRoles().values()) {
                    sb.append(authRole.getRole());
                    if (authRole.getUsers().size() > 0) {
                        sb.append("->[users: ");
                        boolean first = true;
                        for (String user: authRole.getUsers()) {
                            if (first) {
                                sb.append(user);
                                first = false;
                            } else {
                                sb.append(',').append(user);
                            }
                            
                        }
                        sb.append(']');
                    }
                    if (authRole.getGroups().size() > 0) {
                        sb.append("->[groups: ");
                        boolean first = true;
                        for (String group: authRole.getGroups()) {
                            if (first) {
                                sb.append(group);
                                first = false;
                            } else {
                                sb.append(',').append(group);
                            }
                            
                        }
                        sb.append(']');
                    }
                    sb.append("\n");
                }
                cellItem.add(new MultiLineLabel(componentId, sb.toString()));
            }
        });

        table = new AdminDataTable("table", columns, new DomainDataProvider(), 40) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return !formVisible;
            }
        };
        add(table);
    }

    public IModel getTitle(Component component) {
        return new StringResourceModel("admin-permissions-title", component, null);
    }

}