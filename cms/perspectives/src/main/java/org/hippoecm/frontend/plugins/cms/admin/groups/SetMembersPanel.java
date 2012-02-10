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
package org.hippoecm.frontend.plugins.cms.admin.groups;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.RepositoryException;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.breadcrumb.IBreadCrumbModel;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.audit.AuditLogger;
import org.hippoecm.audit.HippoEvent;
import org.hippoecm.frontend.plugins.cms.admin.AdminBreadCrumbPanel;
import org.hippoecm.frontend.plugins.cms.admin.users.User;
import org.hippoecm.frontend.plugins.cms.admin.users.UserDataProvider;
import org.hippoecm.frontend.plugins.cms.admin.widgets.AdminDataTable;
import org.hippoecm.frontend.plugins.cms.admin.widgets.AjaxLinkLabel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SetMembersPanel extends AdminBreadCrumbPanel {
    @SuppressWarnings("unused")
    private static final String SVN_ID = "$Id$";
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(SetMembersPanel.class);

    private final IModel model;
    private final ListView localList;

    private final AdminDataTable table;

    public SetMembersPanel(final String id, final IBreadCrumbModel breadCrumbModel,
            final IModel model) {
        super(id, breadCrumbModel);
        setOutputMarkupId(true);
        
        this.model = model;
        final Group group = (Group) model.getObject();

        // members
        Label localLabel = new Label("group-members-label", new ResourceModel("group-members"));
        localList = new MembershipsListEditView("group-members", "group-member", group);
        add(localLabel);
        add(localList);

        // All local groups
        List<IColumn> columns = new ArrayList<IColumn>();
        columns.add(new PropertyColumn(new ResourceModel("user-username"), "username"));
        columns.add(new PropertyColumn(new ResourceModel("user-firstname"), "firstName"));
        columns.add(new PropertyColumn(new ResourceModel("user-lastname"), "lastName"));
        
        columns.add(new AbstractColumn(new Model(""), "add") {
            private static final long serialVersionUID = 1L;

            public void populateItem(final Item item, final String componentId, final IModel model) {
                final User user = (User) model.getObject();
                AjaxLinkLabel action = new AjaxLinkLabel(componentId, new ResourceModel("group-member-add-action")) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        try {
                            if (group.getMembers().contains(user.getUsername())) {
                                info(getString("group-member-already-member", model));
                            } else {
                                group.addMembership(user.getUsername());
                                HippoEvent event = new HippoEvent().user(getSession()).action("add-user-to-group")
                                        .category(HippoEvent.CATEGORY_GROUP_MANAGEMENT)
                                        .message("added user " + user.getUsername() + " to group " + group.getGroupname());
                                AuditLogger.getLogger().info(event.toString());
                                info(getString("group-member-added", model));
                                localList.removeAll();
                            }
                        } catch (RepositoryException e) {
                            error(getString("group-member-add-failed", model));
                            log.error("Failed to add member", e);
                        }
                        target.addComponent(SetMembersPanel.this);
                    }
                };
                item.add(action);
            }
        });

        table = new AdminDataTable("table", columns, new UserDataProvider(), 20);
        table.setOutputMarkupId(true);
        add(table);
    }

    /** list view to be nested in the form. */
    private final class MembershipsListEditView extends ListView {
        private static final long serialVersionUID = 1L;
        private String labelId;
        private Group group;

        public MembershipsListEditView(final String id, final String labelId, final Group group) {
            super(id, new PropertyModel(group, "members"));
            this.labelId = labelId;
            this.group = group;
            setReuseItems(false);
            setOutputMarkupId(true);
        }

        protected void populateItem(ListItem item) {
            item.setOutputMarkupId(true);
            final String username = (String) item.getModelObject();
            item.add(new Label(labelId, username));
            item.add(new AjaxLinkLabel("remove", new ResourceModel("group-member-remove-action")) {
                private static final long serialVersionUID = 1L;

                @Override
                public void onClick(AjaxRequestTarget target) {
                    try {
                        group.removeMembership(username);
                        HippoEvent event = new HippoEvent().user(getSession()).action("remove-user-from-group")
                                .category(HippoEvent.CATEGORY_GROUP_MANAGEMENT)
                                .message("removed user " + username + " from group " + group.getGroupname());
                        AuditLogger.getLogger().info(event.toString());
                        info(getString("group-member-removed", null));
                        localList.removeAll();
                    } catch (RepositoryException e) {
                        error(getString("group-member-remove-failed", null));
                        log.error("Failed to remove memberships", e);
                    }
                    target.addComponent(SetMembersPanel.this);
                }
            });
        }
    }

    public IModel<String> getTitle(Component component) {
        return new StringResourceModel("group-edit-title", component, model);
    }

}
