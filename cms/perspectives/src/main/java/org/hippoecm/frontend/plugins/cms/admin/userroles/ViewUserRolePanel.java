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
package org.hippoecm.frontend.plugins.cms.admin.userroles;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.jcr.AccessDeniedException;
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
import org.hippoecm.frontend.form.PostOnlyForm;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugins.cms.admin.AdminBreadCrumbPanel;
import org.hippoecm.frontend.plugins.cms.admin.SecurityManagerHelper;
import org.hippoecm.frontend.plugins.cms.admin.permissions.PermissionBean;
import org.hippoecm.frontend.plugins.cms.admin.permissions.ViewDomainActionLink;
import org.hippoecm.frontend.plugins.cms.admin.permissions.ViewPermissionLinkLabel;
import org.hippoecm.frontend.plugins.cms.admin.widgets.AjaxLinkLabel;
import org.hippoecm.frontend.plugins.standards.panelperspective.breadcrumb.PanelPluginBreadCrumbLink;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.util.EventBusUtils;
import org.hippoecm.repository.api.HippoSession;
import org.onehippo.repository.security.SecurityConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bloomreach.xm.repository.security.AuthRole;
import com.bloomreach.xm.repository.security.DomainAuth;
import com.bloomreach.xm.repository.security.UserRole;
import com.bloomreach.xm.repository.security.UserRoleBean;

import static org.onehippo.cms7.event.HippoEventConstants.CATEGORY_USERROLE_MANAGEMENT;

/**
 * Panel showing information regarding the userrole.
 */
public class ViewUserRolePanel extends AdminBreadCrumbPanel {

    private static final Logger log = LoggerFactory.getLogger(ViewUserRolePanel.class);

    private final IPluginContext context;
    private final IModel<UserRole> userRoleModel;
    private final UserRolesListView userRolesListView;
    private final AddUserRolePanel addUserRolePanel;
    private final PermissionsListView permissionsListView;
    private final boolean isSecurityApplAdmin;

    private final IDialogService dialogService;

    public ViewUserRolePanel(final String id, final IPluginContext context, final IBreadCrumbModel breadCrumbModel,
                             final IModel<UserRole> userRoleModel) {
        super(id, breadCrumbModel);
        this.context = context;
        final HippoSession session = UserSession.get().getJcrSession();
        isSecurityApplAdmin = session.isUserInRole(SecurityConstants.USERROLE_SECURITY_APPLICATION_ADMIN);

        this.userRoleModel = userRoleModel;
        final UserRole userRole = userRoleModel.getObject();
        dialogService = context.getService(IDialogService.class.getName(), IDialogService.class);

        add(new Label("view-userrole-panel-title", new StringResourceModel("userrole-view-title", this).setModel(userRoleModel)));

        // common userrole properties
        add(new Label("name", userRole.getName())); // userroles cannot be renamed, so no model needed
        add(new Label("description", () -> userRoleModel.getObject().getDescription()));
        add(new Label("system", userRole.isSystem()));

        // actions
        final PanelPluginBreadCrumbLink editLink = new PanelPluginBreadCrumbLink("edit-userrole", breadCrumbModel) {
            protected IBreadCrumbParticipant getParticipant(final String componentId) {
                return new EditUserRolePanel(componentId, breadCrumbModel, userRoleModel);
            }
        };
        editLink.setVisible(isSecurityApplAdmin && !userRole.isSystem());
        add(editLink);

        final AjaxLinkLabel deleteUserRole = new AjaxLinkLabel("delete-userrole", new ResourceModel("userrole-delete")) {
            @Override
            public void onClick(final AjaxRequestTarget target) {
                final Confirm confirm = new Confirm(
                        getString("userrole-delete-title", userRoleModel),
                        getString("userrole-delete-text", userRoleModel)
                ).ok(() -> deleteUserRole());

                dialogService.show(confirm);
            }
        };
        deleteUserRole.setVisible(isSecurityApplAdmin && !userRole.isSystem());
        add(deleteUserRole);

        userRolesListView = new UserRolesListView("userroles", context);
        add(userRolesListView);
        addUserRolePanel = new AddUserRolePanel("add-userroles");
        addUserRolePanel.setVisible(isSecurityApplAdmin && !userRole.isSystem());
        add(addUserRolePanel);

        permissionsListView = new PermissionsListView("permissions");
        add(permissionsListView);


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
    public IModel<String> getTitle(final Component component) {
        return new StringResourceModel("userrole-view-title", component).setModel(userRoleModel);
    }

    @Override
    public void onActivate(final IBreadCrumbParticipant previous) {
        super.onActivate(previous);
        if (!userRoleModel.getObject().isSystem()) {
            userRolesListView.updateUserRoles();
            addUserRolePanel.updateUserRoleChoice();
        }
    }

    private void deleteUserRole() {
        final UserRole userRole = userRoleModel.getObject();
        if (userRole != null) {
            try {
                SecurityManagerHelper.getUserRolesManager().deleteUserRole(userRole.getName());
                EventBusUtils.post("delete-userrole", CATEGORY_USERROLE_MANAGEMENT,
                        String.format("deleted userrole '%s'",userRole.getName()));
                activateParentAndDisplayInfo(getString("userrole-deleted", userRoleModel));
            } catch (AccessDeniedException e) {
                error(getString("userrole-delete-denied", userRoleModel));
                log.error("Not allowed to delete userrole '{}' : ", userRole.getName(), e);
                redraw();
            } catch (RepositoryException e) {
                error(getString("userrole-delete-failed", userRoleModel));
                log.error("Unable to delete userrole '{}' : ", userRole.getName(), e);
                redraw();
            }
        }
    }

    /**
     * Delete a userrole from the list of userroles of the userrole.
     *
     * @param userRoleToRemove The UserRole name to remove from the userrole.
     */
    private void removeUserRole(final String userRoleToRemove) {
        final UserRole userRole = userRoleModel.getObject();
        final MapModel nameModel = new MapModel<>(Collections.singletonMap("name", userRoleToRemove));
        try {
            if (userRole != null && userRole.getRoles().contains(userRoleToRemove)) {
                final UserRoleBean userRoleBean = new UserRoleBean(userRole);
                userRoleBean.getRoles().remove(userRoleToRemove);
                userRoleModel.setObject(SecurityManagerHelper.getUserRolesManager().updateUserRole(userRoleBean));
                info(getString("userrole-removed", nameModel));
                EventBusUtils.post("remove-userrole", CATEGORY_USERROLE_MANAGEMENT,
                        String.format("removed userrole '%s' from userrole '%s'", userRoleToRemove, userRole.getName()));
            }
            addUserRolePanel.updateUserRoleChoice();
            userRolesListView.updateUserRoles();
        } catch (AccessDeniedException e) {
            error(getString("userrole-remove-denied", nameModel));
            log.error("Not allowed to remove userrole '{}': ", userRoleToRemove, e);
        } catch (RepositoryException e) {
            error(getString("userrole-remove-failed", nameModel));
            log.error("Unable to remove userrole '{}' : ", userRoleToRemove, e);
        }
        redraw();
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
                protected void onSubmit(final AjaxRequestTarget target) {
                    // clear old feedbacks prior showing new ones
                    hippoForm.clearFeedbackMessages();
                    final UserRole userRole = userRoleModel.getObject();
                    final MapModel nameModel = new MapModel<>(Collections.singletonMap("name", selectedUserRole));
                    try {
                        if (userRole != null && !userRole.getRoles().contains(selectedUserRole)) {
                            final UserRoleBean userRoleBean = new UserRoleBean(userRole);
                            userRoleBean.getRoles().add(selectedUserRole);
                            userRoleModel.setObject(SecurityManagerHelper.getUserRolesManager().updateUserRole(userRoleBean));
                            info(getString("userrole-added", nameModel));
                            EventBusUtils.post("add-userrole", CATEGORY_USERROLE_MANAGEMENT,
                                    String.format("added userrole '%s' to userrole '%s'", selectedUserRole, userRole.getName()));
                        }
                        userRolesListView.updateUserRoles();
                        addUserRolePanel.updateUserRoleChoice();
                    } catch (AccessDeniedException e) {
                        error(getString("userrole-add-denied", nameModel));
                        log.error("Not allowed to add userrole '{}': ", selectedUserRole, e);
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
            return SecurityManagerHelper.getUserRolesProvider().getRoles().stream()
                    .map(UserRole::getName)
                    .filter(r -> !(r.equals(userRoleModel.getObject().getName()) || userRoleModel.getObject().getRoles().contains(r)))
                    .sorted()
                    .collect(Collectors.toList());
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
     * List view for the userroles.
     */
    private final class UserRolesListView extends ListView<String> {

        private final IPluginContext context;

        UserRolesListView(final String id, final IPluginContext context) {
            super(id, new ArrayList<>(userRoleModel.getObject().getRoles()));
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
                item.add(new ViewUserRoleLinkLabel("name", userRoleModel, ViewUserRolePanel.this, context));
                item.add(new Label("description", Model.of(userRoleModel.getObject().getDescription())));
            }
            if (isSecurityApplAdmin && !userRoleModel.getObject().isSystem()) {
                item.add(new RemoveUserRoleActionLinkLabel("remove-userrole",
                        new ResourceModel("userrole-remove-action"), userRoleName));
            } else {
                item.add(new Label("remove-userrole"));
            }
        }

        void updateUserRoles() {
            setModelObject(new ArrayList<>(userRoleModel.getObject().getRoles()));
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
                ).ok(() -> removeUserRole(userRoleToRemove));
                dialogService.show(confirm);
                target.add(ViewUserRolePanel.this);
            }
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
                        return PermissionBean.forUserRole(userRoleModel.getObject());
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
            item.add(new ViewDomainActionLink("securityDomain",context, ViewUserRolePanel.this, permissionBean.getDomain(),
                    Model.of(domain.getName())));
            item.add(new Label("domain-folder", domain.getFolderPath()));
            item.add(new ViewPermissionLinkLabel("permission", permissionBean.getDomain(), authRole.getName(),
                    ViewUserRolePanel.this, context));
            item.add(new Label("role", roleName));
        }
    }
}
