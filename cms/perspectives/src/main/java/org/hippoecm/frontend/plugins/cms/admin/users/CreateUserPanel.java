/*
 *  Copyright 2008-2020 Hippo B.V. (http://www.onehippo.com)
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
import java.util.Arrays;
import java.util.List;

import javax.jcr.RepositoryException;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.extensions.breadcrumb.IBreadCrumbModel;
import org.apache.wicket.extensions.validation.validator.RfcCompliantEmailAddressValidator;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.form.validation.EqualPasswordInputValidator;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.validation.validator.StringValidator;
import org.hippoecm.frontend.dialog.HippoForm;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.cms.admin.AdminBreadCrumbPanel;
import org.hippoecm.frontend.plugins.cms.admin.password.validation.IPasswordValidationService;
import org.hippoecm.frontend.plugins.cms.admin.password.validation.PasswordValidationStatus;
import org.hippoecm.frontend.plugins.cms.admin.validators.UsernameValidator;
import org.hippoecm.frontend.util.EventBusUtils;
import org.onehippo.cms7.event.HippoEventConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreateUserPanel extends AdminBreadCrumbPanel {

    private static final Logger log = LoggerFactory.getLogger(CreateUserPanel.class);

    private String password;
    private String passwordCheck;

    private final String defaultUserSecurityProviderName;

    private final List<String> selectableUserSecurityProviderNames;
    private String selectedUserSecurityProviderName;

    private final IPasswordValidationService passwordValidationService;

    public CreateUserPanel(final String id, final IBreadCrumbModel breadCrumbModel, final IPluginContext context, final IPluginConfig config) {
        super(id, breadCrumbModel);

        passwordValidationService = context.getService(IPasswordValidationService.class.getName(),
                IPasswordValidationService.class);

        defaultUserSecurityProviderName = config.getString(ListUsersPlugin.DEFAULT_USER_SECURITY_PROVIDER_KEY);

        final String[] configuredNames =  config.getStringArray(ListUsersPlugin.SELECTABLE_USER_SECURITY_PROVIDERS_KEY);
        selectableUserSecurityProviderNames = (configuredNames != null) ? Arrays.asList(configuredNames) : new ArrayList<>();

        // add form with markup id setter so it can be updated via ajax
        final User user = new User();
        final IModel<User> userModel = new CompoundPropertyModel<>(user);
        final Form<User> form = new HippoForm<User>("form", userModel) {

            @Override
            protected void onValidateModelObjects() {
                if (password != null && passwordValidationService != null) {
                    try {
                        final List<PasswordValidationStatus> statuses =
                                passwordValidationService.checkPassword(password, user);
                        for (final PasswordValidationStatus status : statuses) {
                            if (!status.accepted()) {
                                error(status.getMessage());
                            }
                        }
                    } catch (RepositoryException e) {
                        log.error("Failed to validate password using password validation service", e);
                    }
                }
            }

            @Override
            protected void onError() {
                password = null;
                passwordCheck = null;
            }
        };
        form.setOutputMarkupId(true);
        add(form);

        final RequiredTextField<String> usernameField = new RequiredTextField<>("username");
        usernameField.add(StringValidator.minimumLength(2));
        usernameField.add(new UsernameValidator());
        form.add(usernameField);

        final TextField<String> firstNameField = new TextField<>("firstName");
        form.add(firstNameField);

        final TextField<String> lastNameField = new TextField<>("lastName");
        form.add(lastNameField);

        final TextField<String> emailField = new TextField<>("email");
        emailField.add(RfcCompliantEmailAddressValidator.getInstance());
        emailField.setRequired(false);
        form.add(emailField);

        final PasswordTextField passwordField =
                new PasswordTextField("password", new PropertyModel<>(this, "password"));
        passwordField.setResetPassword(false);
        form.add(passwordField);

        final PasswordTextField passwordCheckField =
                new PasswordTextField("password-check", new PropertyModel<>(this, "passwordCheck"));
        passwordCheckField.setRequired(false);
        passwordCheckField.setResetPassword(false);
        form.add(passwordCheckField);

        final DropDownChoice providers = new DropDownChoice<>("provider", new PropertyModel<>(this, "selectedProvider"),
                selectableUserSecurityProviderNames);
        providers.setVisible(!selectableUserSecurityProviderNames.isEmpty());
        form.add(providers);

        form.add(new EqualPasswordInputValidator(passwordField, passwordCheckField));

        final AjaxButton createButton = new AjaxButton("create-button", form) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {

                final String username = user.getUsername();
                try {
                    final String userSecurityProviderName =
                            StringUtils.isNotBlank(selectedUserSecurityProviderName) ? selectedUserSecurityProviderName
                                    : defaultUserSecurityProviderName;
                    user.create(userSecurityProviderName);
                    user.savePassword(password);

                    EventBusUtils.post("create-user", HippoEventConstants.CATEGORY_USER_MANAGEMENT,
                            "created user " + username);

                    final String infoMsg = getString("user-created", new Model<>(user));
                    activateParentAndDisplayInfo(infoMsg);
                } catch (RepositoryException e) {
                    target.add(CreateUserPanel.this);
                    error(getString("user-create-failed", new Model<>(user)));
                    log.error("Unable to create user '{}' : ", username, e);
                }
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form form) {
                // make sure the feedback panel is shown
                target.add(CreateUserPanel.this);
            }
        };
        form.add(createButton);
        form.setDefaultButton(createButton);

        // add a button that can be used to submit the form via ajax
        form.add(new AjaxButton("cancel-button") {
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                activateParent();
            }
        }.setDefaultFormProcessing(false));
    }

    @Override
    public IModel<String> getTitle(Component component) {
        return new StringResourceModel("user-create", component);
    }

    @SuppressWarnings("unused")
    public String getPassword() {
        return password;
    }

    @SuppressWarnings("unused")
    public void setPassword(String password) {
        this.password = password;
    }

    @SuppressWarnings("unused")
    public String getPasswordCheck() {
        return passwordCheck;
    }

    @SuppressWarnings("unused")
    public void setPasswordCheck(String passwordCheck) {
        this.passwordCheck = passwordCheck;
    }

    @SuppressWarnings("unused")
    public void setSelectedProvider(String selectedUserSecurityProviderName) {
        this.selectedUserSecurityProviderName = selectedUserSecurityProviderName;
    }

    @SuppressWarnings("unused")
    public String getSelectedProvider() {
        return selectedUserSecurityProviderName;
    }

}
