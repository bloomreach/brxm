/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.extensions.breadcrumb.IBreadCrumbModel;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.validation.validator.StringValidator;
import org.hippoecm.frontend.plugins.cms.admin.AdminBreadCrumbPanel;
import org.hippoecm.frontend.plugins.cms.admin.users.User;
import org.hippoecm.frontend.plugins.cms.admin.users.UserDataProvider;
import org.hippoecm.frontend.plugins.cms.admin.widgets.AdminDataTable;
import org.hippoecm.frontend.plugins.cms.admin.widgets.AjaxLinkLabel;
import org.hippoecm.frontend.plugins.cms.admin.widgets.DefaultFocusBehavior;
import org.hippoecm.frontend.session.UserSession;
import org.onehippo.cms7.event.HippoEvent;
import org.onehippo.cms7.event.HippoEventConstants;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.eventbus.HippoEventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SetMembersPanel extends AdminBreadCrumbPanel {
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(SetMembersPanel.class);

    private final IModel model;
    private final ListView localList;

    public SetMembersPanel(final String id, final IBreadCrumbModel breadCrumbModel,
                           final IModel model) {
        super(id, breadCrumbModel);
        setOutputMarkupId(true);

        this.model = model;
        final Group group = (Group) model.getObject();

        // members
        Label localLabel = new Label("group-members-label", new StringResourceModel("group-members-label", this, model));
        localList = new MembershipsListEditView("group-members", "group-member", group);
        add(localLabel);
        add(localList);

        // All local groups
        List<IColumn<User, String>> allUserColumns = new ArrayList<IColumn<User, String>>();
        allUserColumns.add(new PropertyColumn<User, String>(new ResourceModel("user-username"), "username"));
        allUserColumns.add(new PropertyColumn<User, String>(new ResourceModel("user-firstname"), "firstName"));
        allUserColumns.add(new PropertyColumn<User, String>(new ResourceModel("user-lastname"), "lastName"));

        allUserColumns.add(new AbstractColumn<User, String>(new ResourceModel("group-member-actions"), "add") {
            private static final long serialVersionUID = 1L;

            public void populateItem(final Item<ICellPopulator<User>> item, final String componentId,
                                     final IModel<User> model) {
                final User user = model.getObject();
                AjaxLinkLabel action = new AjaxLinkLabel(componentId, new ResourceModel("group-member-add-action")) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        try {
                            if (group.getMembers().contains(user.getUsername())) {
                                info(getString("group-member-already-member", model));
                            } else {
                                group.addMembership(user.getUsername());
                                HippoEventBus eventBus = HippoServiceRegistry.getService(HippoEventBus.class);
                                if (eventBus != null) {
                                    final UserSession userSession = UserSession.get();
                                    HippoEvent event = new HippoEvent(userSession.getApplicationName())
                                            .user(userSession.getJcrSession().getUserID())
                                            .action("add-user-to-group")
                                            .category(HippoEventConstants.CATEGORY_GROUP_MANAGEMENT)
                                            .message("added user " + user.getUsername() + " to group " +
                                                    group.getGroupname());
                                    eventBus.post(event);
                                }
                                info(getString("group-member-added", model));
                                localList.removeAll();
                            }
                        } catch (RepositoryException e) {
                            error(getString("group-member-add-failed", model));
                            log.error("Failed to add member", e);
                        }
                        target.add(SetMembersPanel.this);
                    }
                };
                item.add(action);
            }
        });

        final Form form = new Form("search-form");
        form.setOutputMarkupId(true);
        add(form);

        final UserDataProvider userDataProvider = new UserDataProvider();

        final TextField<String> search = new TextField<String>("search-query",
                new PropertyModel<String>(userDataProvider, "searchTerm"));
        search.add(StringValidator.minimumLength(1));
        search.setRequired(false);
        search.add(new DefaultFocusBehavior());
        form.add(search);

        final AdminDataTable table = new AdminDataTable<User>("table", allUserColumns, userDataProvider, 20);
        table.setOutputMarkupId(true);
        add(table);

        form.add(new AjaxButton("search-button", form) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target, final Form form) {
                target.add(table);
            }
        });
    }

    /**
     * list view to be nested in the form.
     */
    private final class MembershipsListEditView extends ListView<String> {
        private static final long serialVersionUID = 1L;
        private String labelId;
        private Group group;

        public MembershipsListEditView(final String id, final String labelId, final Group group) {
            super(id, new PropertyModel<List<String>>(group, "members"));
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
                private static final long serialVersionUID = 1L;

                @Override
                public void onClick(AjaxRequestTarget target) {
                    try {
                        group.removeMembership(username);
                        HippoEventBus eventBus = HippoServiceRegistry.getService(HippoEventBus.class);
                        if (eventBus != null) {
                            final UserSession userSession = UserSession.get();
                            HippoEvent event = new HippoEvent(userSession.getApplicationName())
                                    .user(userSession.getJcrSession().getUserID())
                                    .action("remove-user-from-group")
                                    .category(HippoEventConstants.CATEGORY_GROUP_MANAGEMENT)
                                    .message("removed user " + username + " from group " + group.getGroupname());
                            eventBus.post(event);
                        }
                        info(getString("group-member-removed", null));
                        localList.removeAll();
                    } catch (RepositoryException e) {
                        error(getString("group-member-remove-failed", null));
                        log.error("Failed to remove memberships", e);
                    }
                    target.add(SetMembersPanel.this);
                }
            });
        }
    }

    public IModel<String> getTitle(Component component) {
        return new StringResourceModel("group-edit-title", component, model);
    }

}
