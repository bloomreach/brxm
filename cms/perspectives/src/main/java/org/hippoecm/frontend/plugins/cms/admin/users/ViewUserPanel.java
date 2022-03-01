/*
 *  Copyright 2008-2022 Hippo B.V. (http://www.onehippo.com)
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
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import javax.jcr.RepositoryException;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.extensions.breadcrumb.IBreadCrumbModel;
import org.apache.wicket.extensions.breadcrumb.IBreadCrumbParticipant;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.model.util.MapModel;
import org.hippoecm.frontend.dialog.Confirm;
import org.hippoecm.frontend.dialog.HippoForm;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugins.cms.admin.AdminBreadCrumbPanel;
import org.hippoecm.frontend.plugins.cms.admin.SecurityManagerHelper;
import org.hippoecm.frontend.plugins.cms.admin.permissions.PermissionBean;
import org.hippoecm.frontend.plugins.cms.admin.permissions.ViewDomainActionLink;
import org.hippoecm.frontend.plugins.cms.admin.permissions.ViewPermissionLinkLabel;
import org.hippoecm.frontend.plugins.cms.admin.userroles.DetachableUserRole;
import org.hippoecm.frontend.plugins.cms.admin.userroles.ViewUserRoleLinkLabel;
import org.hippoecm.frontend.plugins.cms.admin.widgets.AjaxLinkLabel;
import org.hippoecm.frontend.plugins.standards.panelperspective.breadcrumb.PanelPluginBreadCrumbLink;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.util.EventBusUtils;
import org.hippoecm.repository.api.HippoSession;
import org.onehippo.cms7.event.HippoEventConstants;
import org.onehippo.repository.security.SecurityConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bloomreach.xm.repository.security.AuthRole;
import com.bloomreach.xm.repository.security.DomainAuth;
import com.bloomreach.xm.repository.security.UserRole;

public class ViewUserPanel extends AdminBreadCrumbPanel {
    private static final Logger log = LoggerFactory.getLogger(ViewUserPanel.class);

    private final IModel<User> model;
    private final boolean isSecurityUserAdmin;
    private final UserRolesListView userRolesListView;
    private final AddUserRolePanel addUserRolePanel;
    private final PermissionsListView permissionsListView;
    private final IDialogService dialogService;
    private final IPluginContext context;

    /**
     * @param id the ID for the Panel
     * @param context the PluginContext
     * @param breadCrumbModel the Model for the page breadcrumb
     * @param userModel the Model for the user to view
     */
    public ViewUserPanel(final String id, final IPluginContext context, final IBreadCrumbModel breadCrumbModel,
                         final IModel<User> userModel) {
        super(id, breadCrumbModel);
        this.context = context;
        model = userModel;
        dialogService = context.getService(IDialogService.class.getName(), IDialogService.class);

        final HippoSession session = UserSession.get().getJcrSession();
        isSecurityUserAdmin = session.isUserInRole(SecurityConstants.USERROLE_SECURITY_USER_ADMIN);

        add(new Label("view-user-panel-title", new StringResourceModel("user-view-title", this).setModel(userModel)));
        // common user properties
        add(new Label("username", new PropertyModel(userModel, "username")));
        add(new Label("firstName", new PropertyModel(userModel, "firstName")));
        add(new Label("lastName", new PropertyModel(userModel, "lastName")));
        add(new Label("email", new PropertyModel(userModel, "email")));
        add(new Label("provider", new PropertyModel(userModel, "provider")));
        add(new Label("active", this::getUserActiveLabel));
        add(new Label("expired", this::getPasswordExpiredResourceModel));

        final User user = userModel.getObject();

        // properties
        add(new Label("properties-label", new ResourceModel("user-properties")) {
            @Override
            public boolean isVisible() {
                return !user.getPropertiesList().isEmpty();
            }
        });
        add(new ListView<Entry<String, String>>("properties", user.getPropertiesList()) {
            @Override
            protected void populateItem(final ListItem<Entry<String, String>> item) {
                final Entry<String, String> entry = item.getModelObject();
                item.add(new Label("key", entry.getKey()));
                item.add(new Label("value", entry.getValue()));
            }
        });

        // actions
        final PanelPluginBreadCrumbLink edit = new PanelPluginBreadCrumbLink("edit-user", breadCrumbModel) {
            @Override
            protected IBreadCrumbParticipant getParticipant(final String componentId) {
                return new EditUserPanel(componentId, breadCrumbModel, userModel);
            }
        };
        edit.setVisible(isSecurityUserAdmin && !user.isExternal());
        add(edit);

        final PanelPluginBreadCrumbLink password = new PanelPluginBreadCrumbLink("set-user-password", breadCrumbModel) {
            @Override
            protected IBreadCrumbParticipant getParticipant(final String componentId) {
                return new SetPasswordPanel(componentId, breadCrumbModel, userModel, context);
            }
        };
        password.setVisible(isSecurityUserAdmin && !user.isExternal());
        add(password);

        final AjaxLinkLabel deleteUserLabel = new AjaxLinkLabel("delete-user", new ResourceModel("user-delete")) {
            @Override
            public void onClick(final AjaxRequestTarget target) {
                final IDialogService dialogService = context.getService(IDialogService.class.getName(), IDialogService.class);
                final Confirm confirm = new Confirm(
                        getString("user-delete-title", userModel),
                        getString("user-delete-text", userModel)
                ).ok(() -> deleteUser(userModel.getObject()));

                dialogService.show(confirm);
            }
        };
        deleteUserLabel.setVisible(isSecurityUserAdmin && !user.isExternal());
        add(deleteUserLabel);

        userRolesListView = new UserRolesListView("userroles", context);
        add(userRolesListView);
        addUserRolePanel = new AddUserRolePanel("add-userroles");
        addUserRolePanel.setVisible(isSecurityUserAdmin);
        add(addUserRolePanel);

        permissionsListView = new PermissionsListView("permissions");
        add(permissionsListView);

        final SetMembershipsPanel setMembershipsPanel =
                new SetMembershipsPanel("set-member-ship-panel", context, breadCrumbModel, userModel);
        add(setMembershipsPanel);
    }

    private String getPasswordExpiredResourceModel() {
        final User user = model.getObject();
        return getString(user.isPasswordExpired() ? "user-password-expired-true" : "user-password-expired-false");
    }

    private String getUserActiveLabel() {
        final User user = model.getObject();
        return getString(user.isActive() ? "user-active-true" : "user-active-false");
    }

    private void deleteUser(final User user) {
        if (user == null) {
            log.info("No user model found when trying to delete user. Probably the Ok button was double clicked.");
            return;
        }
        final String username = user.getUsername();
        try {
            user.delete();

            // Let the outside world know that this user got deleted
            EventBusUtils.post("delete-user", HippoEventConstants.CATEGORY_USER_MANAGEMENT, "deleted user " + username);

            final String infoMsg = getString("user-removed", Model.of(user));
            activateParentAndDisplayInfo(infoMsg);

        } catch (final RepositoryException e) {
            error(getString("user-remove-failed", Model.of(user)));
            log.error("Unable to delete user '{}' : ", username, e);
            redraw();
        }
    }

    @Override
    public IModel<String> getTitle(final Component component) {
        return new StringResourceModel("user-view-title", component).setModel(model);
    }

    private void removeUserRole(final String userRoleToRemove) {
        final MapModel nameModel = new MapModel<>(Collections.singletonMap("name", userRoleToRemove));
        try {
            model.getObject().removeUserRole(userRoleToRemove);
            userRolesListView.updateUserRoles();
            addUserRolePanel.updateUserRoleChoice();
            EventBusUtils.post("remove-userrole", HippoEventConstants.CATEGORY_USER_MANAGEMENT,
                    String.format("removed userrole '%s' from user '%s'", userRoleToRemove, model.getObject().getDisplayName()));
            info(getString("userrole-removed", nameModel));
        } catch (RepositoryException e) {
            error(getString("userrole-remove-failed"));
            log.error("Failed to remove userrole", e);
        }
        redraw();
    }

    /**
     * List view for the userroles.
     */
    private final class UserRolesListView extends ListView<String> {

        private final IPluginContext context;

        UserRolesListView(final String id, final IPluginContext context) {
            super(id, new ArrayList<>(model.getObject().getUserRoles()));
            this.context = context;
            setReuseItems(false);
        }

        @Override
        protected void populateItem(final ListItem<String> item) {
            final String userRoleName = item.getModelObject();
            final UserRole userRole = SecurityManagerHelper.getUserRolesProvider().getRole(userRoleName);
            if (userRole == null) {
                item.add(new Label("name", Model.of(userRoleName)));
                item.add(new Label("description"));
            } else {
                final DetachableUserRole userRoleModel = new DetachableUserRole(userRole);
                item.add(new ViewUserRoleLinkLabel("name", userRoleModel, ViewUserPanel.this, context));
                item.add(new Label("description", Model.of(userRoleModel.getObject().getDescription())));
            }
            if (isSecurityUserAdmin) {
                item.add(new RemoveUserRoleActionLinkLabel("remove-userrole",
                        new ResourceModel("userrole-remove-action"), userRoleName));
            } else {
                item.add(new Label("remove-userrole", ""));
            }
        }

        void updateUserRoles() {
            setModelObject(model.getObject().getUserRoles());
        }

        private final class RemoveUserRoleActionLinkLabel extends AjaxLinkLabel {

            private final String userRoleToRemove;

            private RemoveUserRoleActionLinkLabel(final String id, final IModel<String> model, final String userRoleToRemove) {
                super(id, model);
                this.userRoleToRemove = userRoleToRemove;
            }

            @Override
            public void onClick(final AjaxRequestTarget target) {
                final MapModel nameModel = new MapModel<>(Collections.singletonMap("name", userRoleToRemove));

                final Confirm confirm = new Confirm(
                        getString("userrole-remove-title", nameModel),
                        getString("userrole-remove-text", nameModel)
                ).ok(() -> {
                    removeUserRole(userRoleToRemove);
                });
                dialogService.show(confirm);
                target.add(ViewUserPanel.this);
            }
        }
    }

    private final class AddUserRolePanel extends Panel {

        private final HippoForm hippoForm;
        private final DropDownChoice<String> userRoleChoice;
        private String selectedUserRole;

        AddUserRolePanel(final String id) {
            super(id);
            setOutputMarkupId(true);
            hippoForm = new HippoForm("form");

            final AjaxButton submit = new AjaxButton("submit", hippoForm) {
                @Override
                protected void onSubmit(AjaxRequestTarget target) {
                    // clear old feedbacks prior showing new ones
                    hippoForm.clearFeedbackMessages();
                    final HippoSession hippoSession = UserSession.get().getJcrSession();
                    final MapModel nameModel = new MapModel<>(Collections.singletonMap("name", selectedUserRole));
                    try {
                        final User user = model.getObject();
                        if ( user != null && !user.getUserRoles().contains(selectedUserRole)) {
                            user.addUserRole(selectedUserRole);
                            EventBusUtils.post("add-userrole", HippoEventConstants.CATEGORY_USER_MANAGEMENT,
                                    String.format("added userrole '%s' to user '%s'", selectedUserRole, user.getDisplayName()));
                            info(getString("userrole-added", nameModel));
                        }
                        userRolesListView.updateUserRoles();
                        updateUserRoleChoice();
                    } catch (RepositoryException e) {
                        error(getString("userrole-add-failed", nameModel));
                        log.error("Unable to add userrole '{}' : ", selectedUserRole, e);
                    }
                    redraw();
                }

                @Override
                public boolean isEnabled() {
                    return selectedUserRole != null;
                }
            };
            hippoForm.add(submit);
            userRoleChoice = new DropDownChoice<>("userroles-select", new PropertyModel<>(this, "selectedUserRole"), getUserRoleChoices());
            userRoleChoice.setNullValid(false);
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
            final User user = model.getObject();
            if (user != null) {
                return SecurityManagerHelper.getUserRolesProvider().getRoles().stream()
                        .map(UserRole::getName)
                        .filter(r -> !user.getUserRoles().contains(r))
                        .sorted()
                        .collect(Collectors.toList());
            }
            return Collections.emptyList();
        }

        void updateUserRoleChoice() {
            userRoleChoice.setChoices(getUserRoleChoices());
        }

        @SuppressWarnings("unused")
        public void setSelectedUserRole(final String selectedUserRole) {
            this.selectedUserRole = selectedUserRole;
        }
    }

    /**
     * List view for showing the permissions of the user.
     */
    private final class PermissionsListView extends ListView<PermissionBean> {

        private final IModel<List<PermissionBean>> permissionBeanModel =
                new LoadableDetachableModel<List<PermissionBean>>() {
                    @Override
                    protected List<PermissionBean> load() {
                        return PermissionBean.forUser(model.getObject());
                    }
                };

        PermissionsListView(final String id) {
            super(id);
            setDefaultModel(permissionBeanModel);
            setReuseItems(false);
        }

        @Override
        protected void populateItem(final ListItem<PermissionBean> item) {
            final PermissionBean permissionBean = item.getModelObject();
            final DomainAuth domain = permissionBean.getDomain().getObject();
            final AuthRole authRole = permissionBean.getAuthRole();
            final String roleName = authRole.getRole();
            item.add(new ViewDomainActionLink("securityDomain",context, ViewUserPanel.this, permissionBean.getDomain(),
                    Model.of(domain.getName())));
            item.add(new Label("domain-folder", domain.getFolderPath()));
            item.add(new ViewPermissionLinkLabel("permission", permissionBean.getDomain(), authRole.getName(),
                    ViewUserPanel.this, context));
            item.add(new Label("role", roleName));
        }
    }
}
