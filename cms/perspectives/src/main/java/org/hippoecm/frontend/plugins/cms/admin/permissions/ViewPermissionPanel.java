/*
 *  Copyright 2019-2022 Hippo B.V. (http://www.onehippo.com)
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
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.jcr.RepositoryException;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.extensions.breadcrumb.IBreadCrumbModel;
import org.apache.wicket.extensions.breadcrumb.IBreadCrumbParticipant;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.model.util.MapModel;
import org.hippoecm.frontend.dialog.Confirm;
import org.hippoecm.frontend.dialog.HippoForm;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.form.PostOnlyForm;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugins.cms.admin.AdminBreadCrumbPanel;
import org.hippoecm.frontend.plugins.cms.admin.SecurityManagerHelper;
import org.hippoecm.frontend.plugins.cms.admin.domains.AuthRolesHelper;
import org.hippoecm.frontend.plugins.cms.admin.groups.DetachableGroup;
import org.hippoecm.frontend.plugins.cms.admin.groups.Group;
import org.hippoecm.frontend.plugins.cms.admin.groups.ViewGroupLinkLabel;
import org.hippoecm.frontend.plugins.cms.admin.users.DetachableUser;
import org.hippoecm.frontend.plugins.cms.admin.users.User;
import org.hippoecm.frontend.plugins.cms.admin.users.UserDataProvider;
import org.hippoecm.frontend.plugins.cms.admin.users.ViewUserLinkLabel;
import org.hippoecm.frontend.plugins.cms.admin.widgets.AdminDataTable;
import org.hippoecm.frontend.plugins.cms.admin.widgets.AjaxLinkLabel;
import org.hippoecm.frontend.plugins.cms.admin.widgets.SearchTermPanel;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoSession;
import org.onehippo.repository.security.SecurityConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bloomreach.xm.repository.security.AuthRole;
import com.bloomreach.xm.repository.security.DomainAuth;
import com.bloomreach.xm.repository.security.UserRole;

/**
 * Panel showing information regarding the permission (Domain.AuthRole).
 */
public class ViewPermissionPanel extends AdminBreadCrumbPanel {

    private static final Logger log = LoggerFactory.getLogger(ViewPermissionPanel.class);

    private final IModel<DomainAuth> model;
    private final String authRoleName;
    private final MapModel nameModel;
    private final boolean isSecurityApplAdmin;
    private final GroupsListView groupsListView;
    private final AddGroupPanel addGroupPanel;
    private final UsersListView usersListView;

    private final IDialogService dialogService;

    public ViewPermissionPanel(final String id, final IPluginContext context, final IBreadCrumbModel breadCrumbModel,
                               final IModel<DomainAuth> model, final String authRoleName) {
        super(id, breadCrumbModel);
        final HippoSession session = UserSession.get().getJcrSession();
        isSecurityApplAdmin = session.isUserInRole(SecurityConstants.USERROLE_SECURITY_APPLICATION_ADMIN);

        this.model = model;
        this.authRoleName = authRoleName;
        DomainAuth domain = model.getObject();
        AuthRole authRole = domain.getAuthRole(authRoleName);
        dialogService = context.getService(IDialogService.class.getName(), IDialogService.class);
        nameModel = new MapModel<>(Collections.singletonMap("name", authRole.getName()));
        add(new Label("permissions-permission-title", new StringResourceModel("permissions-permission-title", this).setModel(nameModel)));
        add(new Label("domain", model.getObject().getName()));
        add(new Label("domain-folder", model.getObject().getFolderPath()));
        final AjaxLinkLabel deletePermission = new AjaxLinkLabel("delete-permission", new ResourceModel("permissions-permission-delete")) {
            @Override
            public void onClick(final AjaxRequestTarget target) {
                final Confirm confirm = new Confirm(
                        getString("permissions-permission-delete-title", nameModel),
                        getString("permissions-permission-delete-text", nameModel)
                ).ok(() -> {
                    deletePermission();
                });

                dialogService.show(confirm);
            }
        };
        deletePermission.setVisible(isSecurityApplAdmin);
        add(deletePermission);

        add(new Label("role", authRole.getRole()));
        add(new Label("userrole", () -> model.getObject().getAuthRole(authRoleName).getUserRole()));

        SetUserRolePanel setUserRolePanel = new SetUserRolePanel("set-userrole");
        setUserRolePanel.setVisible(isSecurityApplAdmin);
        add(setUserRolePanel);

        groupsListView = new GroupsListView("groups", context);
        add(groupsListView);

        addGroupPanel = new AddGroupPanel("add-group");
        addGroupPanel.setVisible(isSecurityApplAdmin);
        add(addGroupPanel);

        usersListView = new UsersListView("users", context);
        add(usersListView);

        if (isSecurityApplAdmin) {
            final List<IColumn<User, String>> allUserColumns = new ArrayList<>();
            allUserColumns.add(new PropertyColumn<>(new ResourceModel("user-username"), "username"));
            allUserColumns.add(new PropertyColumn<>(new ResourceModel("user-firstname"), "firstName"));
            allUserColumns.add(new PropertyColumn<>(new ResourceModel("user-lastname"), "lastName"));

            allUserColumns.add(new AbstractColumn<User, String>(new ResourceModel("table-header-actions"), "add") {

                public void populateItem(final Item<ICellPopulator<User>> cellItem, final String componentId,
                                         final IModel<User> rowModel) {
                    final User user = rowModel.getObject();
                    final AjaxLinkLabel action = new AjaxLinkLabel(componentId, new ResourceModel("permissions-permission-user-add-action")) {

                        @Override
                        public void onClick(final AjaxRequestTarget target) {
                            AuthRole authRole = model.getObject().getAuthRole(authRoleName);
                            try {
                                if (authRole.getUsers().contains(user.getUsername())) {
                                    showInfo(getString("permissions-permission-user-already-added", rowModel));
                                } else {
                                    AuthRolesHelper.authRoleAddUser(authRole, user.getUsername());
                                    showInfo(getString("group-member-added", rowModel));
                                    // backing model is immutable, force reload
                                    model.detach();
                                    usersListView.updateUsers();
                                }
                            } catch (RepositoryException e) {
                                showError(getString("permissions-permission-user-add-failed", rowModel));
                                log.error("Failed to add user", e);
                            }
                            target.add(ViewPermissionPanel.this);
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
                public void processSubmit(final AjaxRequestTarget target, final String searchTerm) {
                    super.processSubmit(target, searchTerm);
                    userDataProvider.setSearchTerm(searchTerm);
                    target.add(table);
                }
            };
            add(searchTermPanel);
        } else {
            add(new Label("search-field") {
                @Override
                public boolean isVisible() {
                    return false;
                }
            });
        }

        // add form with markup id setter so it can be updated via ajax
        final Form<?> form = new PostOnlyForm<>("back-form");
        form.setOutputMarkupId(true);
        add(form);
        // add a cancel/back button
        form.add(new AjaxButton("back-button") {
            @Override
            protected void onSubmit(final AjaxRequestTarget target) {
                // one up
                final List<IBreadCrumbParticipant> all = breadCrumbModel.allBreadCrumbParticipants();
                breadCrumbModel.setActive(all.get(all.size() - 2));
            }
        }.setDefaultFormProcessing(false));
    }

    @Override
    public IModel<String> getTitle(Component component) {
        return new StringResourceModel("permissions-permission-title", component).setModel(nameModel);
    }

    @Override
    public void onActivate(IBreadCrumbParticipant previous) {
        super.onActivate(previous);
        groupsListView.updateGroups();
        addGroupPanel.updateGroupChoice();
        usersListView.updateUsers();
    }

    private void showError(final String msg) {
        error(msg);
    }

    private void showInfo(final String msg) {
        info(msg);
    }

    private void deletePermission() {
        final DomainAuth domain = model.getObject();
        final AuthRole authRole = domain.getAuthRole(authRoleName);
        if (authRole != null) {
            final MapModel nameModel = new MapModel<>(Collections.singletonMap("name", authRole.getName()));
            try {
                AuthRolesHelper.deleteAuthRole(authRole);
                activateParentAndDisplayInfo(getString("permissions-permission-deleted", nameModel));
            } catch (RepositoryException e) {
                error(getString("permissions-permission-delete-failed", nameModel));
                log.error("Unable to delete permission '{}' : ", authRole.getPath(), e);
                redraw();
            }
        }
    }

    private final class SetUserRolePanel extends Panel {

        private final HippoForm hippoForm;
        private String selectedUserRole;

        SetUserRolePanel(final String id) {
            super(id);
            setOutputMarkupId(true);
            hippoForm = new HippoForm("form");

            final AjaxButton submit = new AjaxButton("submit", hippoForm) {
                @Override
                protected void onSubmit(AjaxRequestTarget target) {
                    // clear old feedbacks prior showing new ones
                    hippoForm.clearFeedbackMessages();
                    final DomainAuth domain = model.getObject();
                    final AuthRole authRole = domain.getAuthRole(authRoleName);
                    if (!StringUtils.equals(selectedUserRole, authRole.getUserRole())) {
                        final MapModel nameModel = new MapModel<>(Collections.singletonMap("name", selectedUserRole));
                        try {
                            if (selectedUserRole != null) {
                                AuthRolesHelper.authRoleSetUserRole(authRole, selectedUserRole);
                                info(getString("permissions-permission-userrole-set", nameModel));
                            } else {
                                final MapModel oldNameModel = new MapModel<>(Collections.singletonMap("name", authRole.getUserRole()));
                                AuthRolesHelper.authRoleSetUserRole(authRole, null);
                                info(getString("permissions-permission-userrole-cleared", oldNameModel));
                            }
                            // backing model is immutable, force reload
                            model.detach();
                            redraw();
                        } catch (RepositoryException e) {
                            error(getString("permissions-permission-userrole-set-failed", nameModel));
                            log.error("Unable to set userrole '{}' : ", selectedUserRole, e);
                        }
                    }
                    redraw();
                }

                @Override
                public boolean isEnabled() {
                    return !StringUtils.equals(selectedUserRole, model.getObject().getAuthRole(authRoleName).getUserRole());
                }
            };
            submit.setDefaultFormProcessing(false);
            hippoForm.add(submit);
            selectedUserRole = model.getObject().getAuthRole(authRoleName).getUserRole();
            final DropDownChoice<String> userRoleChoice =
                    new DropDownChoice<>("userroles-select", new PropertyModel<>(this, "selectedUserRole"), getUserRoleChoices());
            userRoleChoice.setNullValid(true);
            userRoleChoice.add(new AjaxFormComponentUpdatingBehavior("change") {
                @Override
                protected void onUpdate(final AjaxRequestTarget target) {
                    target.add(submit);
                }
            });
            hippoForm.add(userRoleChoice);
            add(hippoForm);
        }

        List<String> getUserRoleChoices() {
            return SecurityManagerHelper.getUserRolesProvider().getRoles().stream()
                    .map(UserRole::getName)
                    .sorted()
                    .collect(Collectors.toList());
        }

        @SuppressWarnings("unused")
        public void setSelectedUserRole(final String selectedUserRole) {
            this.selectedUserRole = selectedUserRole;
        }
    }

    /**
     * List view for the groups.
     */
    private final class GroupsListView extends ListView<String> {

        private final IPluginContext context;

        GroupsListView(final String id, final IPluginContext context) {
            super(id, new ArrayList<>(model.getObject().getAuthRole(authRoleName).getGroups()));
            this.context = context;
            setReuseItems(false);
        }

        @Override
        protected void populateItem(final ListItem<String> item) {
            final String groupName = item.getModelObject();
            Group group = Group.getGroup(groupName);
            if (group == null) {
                item.add(new Label("groupname", groupName));
            } else {
                item.add(new ViewGroupLinkLabel("groupname", new DetachableGroup(group), ViewPermissionPanel.this, context));
            }
            if (isSecurityApplAdmin) {
                item.add(new RemoveGroupActionLinkLabel("remove-group",
                        new ResourceModel("permissions-permission-group-remove-action"), groupName));
            } else {
                item.add(new Label("remove-group"));
            }
        }

        void updateGroups() {
            setModelObject(new ArrayList<>(model.getObject().getAuthRole(authRoleName).getGroups()));
        }

        private final class RemoveGroupActionLinkLabel extends AjaxLinkLabel {

            private final String groupToRemove;

            private RemoveGroupActionLinkLabel(final String id, final IModel<String> model, final String groupToRemove) {
                super(id, model);
                this.groupToRemove = groupToRemove;
            }

            @Override
            public void onClick(final AjaxRequestTarget target) {
                final MapModel nameModel = new MapModel<>(Collections.singletonMap("name", groupToRemove));

                final Confirm confirm = new Confirm(
                        getString("permissions-permission-group-remove-title", nameModel),
                        getString("permissions-permission-group-remove-text", nameModel)
                ).ok(() -> {
                    removeGroup(groupToRemove);
                });
                dialogService.show(confirm);
                target.add(ViewPermissionPanel.this);
            }
        }
    }

    private void removeGroup(final String groupToRemove) {
        final MapModel nameModel = new MapModel<>(Collections.singletonMap("name", groupToRemove));
        try {
            AuthRolesHelper.authRoleRemoveGroup(model.getObject().getAuthRole(authRoleName), groupToRemove);
            // backing model is immutable, force reload
            model.detach();
            groupsListView.updateGroups();
            info(getString("permissions-permission-group-removed", nameModel));
        } catch (RepositoryException e) {
            error(getString("permissions-permission-group-remove-failed"));
            log.error("Failed to remove group", e);
        }
        redraw();
    }

    private final class AddGroupPanel extends Panel {

        private final HippoForm hippoForm;
        private final DropDownChoice<String> groupChoice;
        private String selectedGroup;

        AddGroupPanel(final String id) {
            super(id);
            setOutputMarkupId(true);
            hippoForm = new HippoForm("form");

            final AjaxButton submit = new AjaxButton("submit", hippoForm) {
                @Override
                protected void onSubmit(AjaxRequestTarget target) {
                    // clear old feedbacks prior showing new ones
                    hippoForm.clearFeedbackMessages();
                    final MapModel nameModel = new MapModel<>(Collections.singletonMap("name", selectedGroup));
                    try {
                        AuthRole authRole = model.getObject().getAuthRole(authRoleName);
                        if (authRole != null && !authRole.getGroups().contains(selectedGroup)) {
                            AuthRolesHelper.authRoleAddGroup(authRole, selectedGroup);
                            info(getString("permissions-permission-group-added", nameModel));
                        }
                        // backing model is immutable, force reload
                        model.detach();
                        groupsListView.updateGroups();
                        updateGroupChoice();
                    } catch (RepositoryException e) {
                        error(getString("permissions-permission-group-add-failed", nameModel));
                        log.error("Unable to add group '{}' : ", selectedGroup, e);
                    }
                    redraw();
                }

                @Override
                public boolean isEnabled() {
                    return selectedGroup != null;
                }
            };
            hippoForm.add(submit);
            groupChoice = new DropDownChoice<>("groups-select", new PropertyModel<>(this, "selectedGroup"), getGroupChoices());
            groupChoice.setNullValid(false);
            groupChoice.add(new AjaxFormComponentUpdatingBehavior("change") {
                @Override
                protected void onUpdate(final AjaxRequestTarget target) {
                    target.add(submit);
                }
            });
            hippoForm.add(groupChoice);
            add(hippoForm);
        }

        List<String> getGroupChoices() {
            final AuthRole authRole = model.getObject().getAuthRole(authRoleName);
            return Group.getAllGroups().stream()
                    .map(Group::getGroupname)
                    .filter(r -> !authRole.getGroups().contains(r))
                    .sorted()
                    .collect(Collectors.toList());
        }

        void updateGroupChoice() {
            groupChoice.setChoices(getGroupChoices());
        }

        @SuppressWarnings("unused")
        public void setSelectedGroup(final String selectedGroup) {
            this.selectedGroup = selectedGroup;
        }
    }

    private final class UsersListView extends ListView<String> {

        private final IPluginContext context;

        UsersListView(final String id, final IPluginContext context) {
            super(id, new ArrayList<>(model.getObject().getAuthRole(authRoleName).getUsers()));
            this.context = context;
            setReuseItems(false);
            setOutputMarkupId(true);
        }

        @Override
        protected void populateItem(ListItem<String> item) {
            item.setOutputMarkupId(true);
            final String username = item.getModelObject();
            if (User.userExists(username)) {
                User user = new User(username);
                item.add(new ViewUserLinkLabel("username", new DetachableUser(user), ViewPermissionPanel.this, context));
                item.add(new Label("firstname", user.getFirstName()));
                item.add(new Label("lastname", user.getLastName()));
            } else {
                item.add(new Label("username", username));
                item.add(new Label("firstname"));
                item.add(new Label("lastname"));
            }
            if (isSecurityApplAdmin) {
                item.add(new RemoveUserActionLinkLabel("remove-user",
                        new ResourceModel("permissions-permission-user-remove-action"), username));
            } else {
                item.add(new Label("remove-user"));
            }
        }

        void updateUsers() {
            setModelObject(new ArrayList<>(model.getObject().getAuthRole(authRoleName).getUsers()));
        }

        private final class RemoveUserActionLinkLabel extends AjaxLinkLabel {

            private final String userToRemove;

            private RemoveUserActionLinkLabel(final String id, final IModel<String> model, final String userToRemove) {
                super(id, model);
                this.userToRemove = userToRemove;
            }

            @Override
            public void onClick(final AjaxRequestTarget target) {
                final MapModel nameModel = new MapModel<>(Collections.singletonMap("name", userToRemove));

                final Confirm confirm = new Confirm(
                        getString("permissions-permission-user-remove-title", nameModel),
                        getString("permissions-permission-user-remove-text", nameModel)
                ).ok(() -> {
                    removeUser(userToRemove);
                });
                dialogService.show(confirm);
                target.add(ViewPermissionPanel.this);
            }
        }
    }

    private void removeUser(final String userToRemove) {
        final MapModel nameModel = new MapModel<>(Collections.singletonMap("name", userToRemove));
        try {
            AuthRolesHelper.authRoleRemoveUser(model.getObject().getAuthRole(authRoleName), userToRemove);
            // backing model is immutable, force reload
            model.detach();
            usersListView.updateUsers();
            info(getString("permissions-permission-user-removed", nameModel));
        } catch (RepositoryException e) {
            error(getString("permissions-permission-user-remove-failed"));
            log.error("Failed to remove user", e);
        }
        redraw();
    }
}
