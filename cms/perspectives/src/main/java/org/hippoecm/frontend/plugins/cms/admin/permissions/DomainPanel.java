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

import javax.jcr.ItemExistsException;
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
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.IModelComparator;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.model.util.MapModel;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;
import org.apache.wicket.validation.validator.StringValidator;
import org.hippoecm.frontend.dialog.Confirm;
import org.hippoecm.frontend.dialog.HippoForm;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.form.PostOnlyForm;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugins.cms.admin.AdminBreadCrumbPanel;
import org.hippoecm.frontend.plugins.cms.admin.SecurityManagerHelper;
import org.hippoecm.frontend.plugins.cms.admin.domains.AuthRolesHelper;
import org.hippoecm.frontend.plugins.cms.admin.groups.Group;
import org.hippoecm.frontend.plugins.cms.admin.userroles.DetachableUserRole;
import org.hippoecm.frontend.plugins.cms.admin.userroles.ViewUserRoleLinkLabel;
import org.hippoecm.frontend.plugins.cms.admin.users.User;
import org.hippoecm.frontend.plugins.cms.admin.widgets.AjaxLinkLabel;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoSession;
import org.onehippo.repository.security.SecurityConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bloomreach.xm.repository.security.AuthRole;
import com.bloomreach.xm.repository.security.DomainAuth;
import com.bloomreach.xm.repository.security.Role;
import com.bloomreach.xm.repository.security.UserRole;

public class DomainPanel extends AdminBreadCrumbPanel {
    private static final Logger log = LoggerFactory.getLogger(DomainPanel.class);

    private final IPluginContext context;
    private final IModel<DomainAuth> model;
    private final AuthRolesListView authRolesListView;
    private final boolean isSecurityApplAdmin;

    public DomainPanel(final String id, final IPluginContext context, final IBreadCrumbModel breadCrumbModel, final IModel<DomainAuth> model) {
        super(id, breadCrumbModel);
        this.context = context;
        this.model = model;

        final HippoSession session = UserSession.get().getJcrSession();
        isSecurityApplAdmin = session.isUserInRole(SecurityConstants.USERROLE_SECURITY_APPLICATION_ADMIN);

        add(new Label("permissions-domain-title", new StringResourceModel("permissions-domain-title", this).setModel(model)));
        add(new Label("domain-folder", model.getObject().getFolderPath()));
        this.authRolesListView = new AuthRolesListView("authrole-row");
        add(authRolesListView);
        AddAuthRolePanel addAuthRolePanel = new AddAuthRolePanel("add-authrole");
        addAuthRolePanel.setVisible(isSecurityApplAdmin);
        add(addAuthRolePanel);
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
    public void onActivate(IBreadCrumbParticipant previous) {
        super.onActivate(previous);
        authRolesListView.updateAuthRoles();
        redraw();
    }

    /**
     * List view for the userroles.
     */
    private final class AuthRolesListView extends ListView<AuthRole> {

        private final IModel<List<AuthRole>> authRolesListModel =
                new LoadableDetachableModel<List<AuthRole>>() {
                    @Override
                    protected List<AuthRole> load() {
                        return new ArrayList<>(model.getObject().getAuthRolesMap().values());
                    }
                };

        AuthRolesListView(final String id) {
            super(id);
            setDefaultModel(authRolesListModel);
            setReuseItems(false);
        }

        @Override
        protected void populateItem(final ListItem<AuthRole> item) {
            AuthRole authRole = item.getModelObject();
            item.add(new ViewPermissionLinkLabel("authrole-name", model, authRole.getName(), DomainPanel.this, context));
            item.add(new Label("role-name", authRole.getRole()));
            UserRole userRole = null;
            if (authRole.getUserRole() != null) {
                userRole = SecurityManagerHelper.getUserRolesProvider().getRole(authRole.getUserRole());
            }
            if (userRole != null) {
                item.add(new ViewUserRoleLinkLabel("userrole", new DetachableUserRole(userRole), DomainPanel.this, context));
            } else {
                item.add(new Label("userrole", authRole.getUserRole()));
            }
            final List<Group> groups = new ArrayList<>();
            for (final String groupName : authRole.getGroups()) {
                Group group = Group.getGroup(groupName);
                if (group != null) {
                    groups.add(group);
                } else {
                    groups.add(Group.newGroup(groupName));
                }
            }
            item.add(new GroupsLinkListPanel("groups", groups, context, DomainPanel.this));

            final List<User> users = new ArrayList<>();
            for (final String userName : authRole.getUsers()) {
                if (User.userExists(userName)) {
                    users.add(new User(userName));
                } else {
                    users.add(User.newUser(userName));
                }
            }
            item.add(new UsersLinkListPanel("users", users, context, DomainPanel.this));
            if (isSecurityApplAdmin) {
                item.add(new DeleteAuthRoleActionLink("action", new ResourceModel("permissions-permission-delete-action"), model, authRole.getName()));
            } else {
                item.add(new Label("action") {
                    @Override
                    public boolean isVisible() {
                        return false;
                    }
                });
            }
        }

        public IModelComparator getModelComparator()
        {
            // force modelChanging()/modelChanged() after setModelObject(), see below
            return IModelComparator.ALWAYS_FALSE;
        }

        void updateAuthRoles() {
            authRolesListModel.detach();
        }
    }

    private class DeleteAuthRoleActionLink extends AjaxLinkLabel {
        private final IModel<DomainAuth> domainModel;
        private final String name;

        private DeleteAuthRoleActionLink(final String id, final IModel<String> model, final IModel<DomainAuth> domainModel, final String name) {
            super(id, model);
            this.domainModel = domainModel;
            this.name = name;
        }

        @Override
        public void onClick(final AjaxRequestTarget target) {
            final IDialogService dialogService = context.getService(IDialogService.class.getName(), IDialogService.class);
            AuthRole authRole = domainModel.getObject().getAuthRole(name);
            final MapModel nameModel = new MapModel<>(Collections.singletonMap("name", authRole.getName()));
            final Confirm confirm = new Confirm(
                    getString("permissions-permission-delete-title", nameModel),
                    getString("permissions-permission-delete-text", nameModel)
            ).ok(() -> deleteAuthRole(name));

            dialogService.show(confirm);
        }
    }

    private final class AddAuthRolePanel extends Panel {

        private final HippoForm hippoForm;
        private final DropDownChoice<String> roleChoice;
        private String selectedRole;
        private String name;

        AddAuthRolePanel(final String id) {
            super(id);
            setOutputMarkupId(true);
            hippoForm = new HippoForm("form");
            hippoForm.setOutputMarkupId(true);
            add(hippoForm);

            final RequiredTextField<String> nameField = new RequiredTextField<>("permission-name", new PropertyModel<>(this, "name"));
            nameField.add(StringValidator.minimumLength(2));
            nameField.add(new AuthRoleNameValidator(model));
            hippoForm.add(nameField);

            roleChoice = new DropDownChoice<>("roles-select", new PropertyModel<>(this, "selectedRole"), getRoleChoices());
            roleChoice.setNullValid(false);
            hippoForm.add(roleChoice);

            final AjaxButton submit = new AjaxButton("submit", hippoForm) {
                @Override
                protected void onSubmit(AjaxRequestTarget target) {
                    // clear old feedbacks prior showing new ones
                    hippoForm.clearFeedbackMessages();
                    final HippoSession hippoSession = UserSession.get().getJcrSession();
                    final DomainAuth domain = model.getObject();
                    final MapModel nameModel = new MapModel<>(Collections.singletonMap("name", name));
                    try {
                        if (domain != null && !domain.getAuthRolesMap().containsKey(name)) {
                            AuthRolesHelper.addAuthRole(name, domain.getPath(), selectedRole);
                            info(getString("permissions-permission-added", nameModel));
                            // backing model is immutable, force reload
                            model.detach();
                            authRolesListView.updateAuthRoles();
                            name = null;
                        } else {
                            error(getString("permissions-permission-add-exists", nameModel));
                        }
                    } catch (ItemExistsException e) {
                        error(getString("permissions-permission-add-exists", nameModel));
                    } catch (RepositoryException e) {
                        error(getString("permissions-permission-add-failed", nameModel));
                        log.error("Unable to add permission '{}' : ", name, e);
                    }
                    redraw();
                }

                @Override
                protected void onError(AjaxRequestTarget target) {
                    // make sure the feedback panel is shown
                    target.add(DomainPanel.this);
                }
                @Override
                public boolean isEnabled() {
                    return selectedRole != null;
                }
            };
            hippoForm.add(submit);

            roleChoice.add(new AjaxFormComponentUpdatingBehavior("change") {
                @Override
                protected void onUpdate(final AjaxRequestTarget target) {
                    target.add(submit);
                }
            });
        }

        List<String> getRoleChoices() {
            return SecurityManagerHelper.getRolesProvider().getRoles().stream()
                    .map(Role::getName)
                    .sorted()
                    .collect(Collectors.toList());
        }

        @SuppressWarnings("unused")
        public void setSelectedRole(final String selectedRole) {
            this.selectedRole = selectedRole;
        }

        @SuppressWarnings("unused")
        public void setName(final String name) {
            this.name = name;
        }
    }



    private void deleteAuthRole(final String authRoleName) {
        final AuthRole authRole = model.getObject().getAuthRole(authRoleName);
        if (authRole == null) {
            log.info("No permission model found when trying to delete permission. Probably the Ok button was double clicked.");
            return;
        }
        final MapModel nameModel = new MapModel<>(Collections.singletonMap("name", authRole.getName()));
        try {
            AuthRolesHelper.deleteAuthRole(authRole);
            info(getString("permissions-permission-deleted", nameModel));
            // backing model is immutable, force reload
            model.detach();
            authRolesListView.updateAuthRoles();
        } catch (final RepositoryException e) {
            error(getString("permissions-permission-delete-failed", nameModel));
            log.error("Unable to delete permission '{}' : ", authRole.getPath(), e);
        }
        redraw();
    }

    @Override
    public IModel<String> getTitle(Component component) {
        return new StringResourceModel("permissions-domain-title", component).setModel(model);
    }

    public static final class AuthRoleNameValidator implements IValidator<String> {

        final private IModel<DomainAuth> model;

        public AuthRoleNameValidator(final IModel<DomainAuth> model) {
            this.model = model;
        }

        @Override
        public void validate(final IValidatable<String> validatable) {
            final String name = validatable.getValue();
            final DomainAuth domain = model.getObject();
            if (domain.getAuthRolesMap().containsKey(name)) {
                final ValidationError validationError = new ValidationError(this, "exists");
                validatable.error(validationError);
            }
        }
    }

}
