/*
 *  Copyright 2008-2017 Hippo B.V. (http://www.onehippo.com)
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
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.plugins.cms.admin.AdminBreadCrumbPanel;
import org.hippoecm.frontend.plugins.cms.admin.users.User;
import org.hippoecm.frontend.plugins.cms.admin.users.UserDataProvider;
import org.hippoecm.frontend.plugins.cms.admin.widgets.AdminDataTable;
import org.hippoecm.frontend.plugins.cms.admin.widgets.AjaxLinkLabel;
import org.hippoecm.frontend.plugins.cms.admin.widgets.SearchTermPanel;
import org.hippoecm.frontend.util.EventBusUtils;
import org.onehippo.cms7.event.HippoEventConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SetMembersPanel extends AdminBreadCrumbPanel {
    private static final Logger log = LoggerFactory.getLogger(SetMembersPanel.class);

    private final IModel<Group> model;
    private final ListView localList;

    public SetMembersPanel(final String id, final IBreadCrumbModel breadCrumbModel,
                           final IModel<Group> model) {
        super(id, breadCrumbModel);
        setOutputMarkupId(true);

        this.model = model;
        final Group group = model.getObject();

        // members
        final Label localLabel = new Label("group-members-label", new StringResourceModel("group-members-label", this, model));
        localList = new MembershipsListEditView("group-members", "group-member", group);
        add(localLabel);
        add(localList);

        // All local groups
        final List<IColumn<User, String>> allUserColumns = new ArrayList<>();
        allUserColumns.add(new PropertyColumn<>(new ResourceModel("user-username"), "username"));
        allUserColumns.add(new PropertyColumn<>(new ResourceModel("user-firstname"), "firstName"));
        allUserColumns.add(new PropertyColumn<>(new ResourceModel("user-lastname"), "lastName"));

        allUserColumns.add(new AbstractColumn<User, String>(new ResourceModel("group-member-actions"), "add") {

            public void populateItem(final Item<ICellPopulator<User>> cellItem, final String componentId,
                                     final IModel<User> rowModel) {
                final User user = rowModel.getObject();
                final AjaxLinkLabel action = new AjaxLinkLabel(componentId, new ResourceModel("group-member-add-action")) {

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        try {
                            if (group.getMembers().contains(user.getUsername())) {
                                showInfo(getString("group-member-already-member", rowModel));
                            } else {
                                group.addMembership(user.getUsername());
                                EventBusUtils.post("add-user-to-group", HippoEventConstants.CATEGORY_GROUP_MANAGEMENT,
                                        "added user " + user.getUsername() + " to group " + group.getGroupname());
                                showInfo(getString("group-member-added", rowModel));
                                localList.removeAll();
                            }
                        } catch (RepositoryException e) {
                            showError(getString("group-member-add-failed", rowModel));
                            log.error("Failed to add member", e);
                        }
                        target.add(SetMembersPanel.this);
                    }
                };
                cellItem.add(action);
            }
        });

        final UserDataProvider userDataProvider = new UserDataProvider();
        final AdminDataTable table = new AdminDataTable<>("table", allUserColumns, userDataProvider, 20);
        table.setOutputMarkupId(true);
        add(table);

        final SearchTermPanel searchTermPanel = new SearchTermPanel("search-field") {
            @Override
            public void processSubmit(final AjaxRequestTarget target, final Form<?> form, final String searchTerm) {
                super.processSubmit(target, form, searchTerm);
                userDataProvider.setSearchTerm(searchTerm);
                target.add(table);
            }
        };
        add(searchTermPanel);

    }

    private void showError(final String msg) {
        error(msg);
    }

    private void showInfo(final String msg) {
        info(msg);
    }

    /**
     * list view to be nested in the form.
     */
    private final class MembershipsListEditView extends ListView<String> {
        private final String labelId;
        private final Group group;

        MembershipsListEditView(final String id, final String labelId, final Group group) {
            super(id, PropertyModel.of(group, "members"));
            this.labelId = labelId;
            this.group = group;
            setReuseItems(false);
            setOutputMarkupId(true);
        }

        @Override
        protected void populateItem(ListItem<String> item) {
            item.setOutputMarkupId(true);
            final String username = item.getModelObject();
            item.add(new Label(labelId, username));
            item.add(new AjaxLinkLabel("remove", new ResourceModel("group-member-remove-action")) {

                @Override
                public void onClick(AjaxRequestTarget target) {
                    try {
                        group.removeMembership(username);
                        EventBusUtils.post("remove-user-from-group", HippoEventConstants.CATEGORY_GROUP_MANAGEMENT,
                                "removed user " + username + " from group " + group.getGroupname());
                        showInfo(getString("group-member-removed"));
                        localList.removeAll();
                    } catch (RepositoryException e) {
                        showError(getString("group-member-remove-failed"));
                        log.error("Failed to remove memberships", e);
                    }
                    target.add(SetMembersPanel.this);
                }
            });
        }
    }

    @Override
    public IModel<String> getTitle(Component component) {
        return new StringResourceModel("group-edit-title", component, model);
    }

}
