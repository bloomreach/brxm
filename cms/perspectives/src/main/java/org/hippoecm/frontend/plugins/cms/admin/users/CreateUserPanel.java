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
package org.hippoecm.frontend.plugins.cms.admin.users;

import java.util.List;

import javax.jcr.RepositoryException;

import org.apache.wicket.Component;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.extensions.breadcrumb.IBreadCrumbModel;
import org.apache.wicket.extensions.breadcrumb.IBreadCrumbParticipant;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IFormSubmitter;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.form.validation.EqualPasswordInputValidator;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.validation.validator.EmailAddressValidator;
import org.apache.wicket.validation.validator.StringValidator;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugins.cms.admin.AdminBreadCrumbPanel;
import org.hippoecm.frontend.plugins.cms.admin.password.validation.IPasswordValidationService;
import org.hippoecm.frontend.plugins.cms.admin.password.validation.PasswordValidationStatus;
import org.hippoecm.frontend.plugins.cms.admin.validators.UsernameValidator;
import org.hippoecm.frontend.session.UserSession;
import org.onehippo.cms7.event.HippoEvent;
import org.onehippo.cms7.event.HippoEventConstants;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.eventbus.HippoEventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreateUserPanel extends AdminBreadCrumbPanel {
    private static final String UNUSED = "unused";


    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(CreateUserPanel.class);

    private String password;
    private String passwordCheck;

    private final IPasswordValidationService passwordValidationService;

    public CreateUserPanel(final String id, final IBreadCrumbModel breadCrumbModel, final IPluginContext context) {
        super(id, breadCrumbModel);
        setOutputMarkupId(true);

        this.passwordValidationService = context.getService(IPasswordValidationService.class.getName(),
                IPasswordValidationService.class);

        // add form with markup id setter so it can be updated via ajax
        final User user = new User();
        final Form<User> form = new Form<User>("form", new CompoundPropertyModel<User>(user)) {

            @Override
            protected void onValidateModelObjects() {
                if (password != null && passwordValidationService != null) {
                    try {
                        List<PasswordValidationStatus> statuses =
                                passwordValidationService.checkPassword(password, user);
                        for (PasswordValidationStatus status : statuses) {
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

        RequiredTextField<String> usernameField = new RequiredTextField<String>("username");
        usernameField.add(StringValidator.minimumLength(2));
        usernameField.add(new UsernameValidator());
        form.add(usernameField);

        TextField<String> firstNameField = new TextField<String>("firstName");
        form.add(firstNameField);

        TextField<String> lastNameField = new TextField<String>("lastName");
        form.add(lastNameField);

        TextField<String> emailField = new TextField<String>("email");
        emailField.add(EmailAddressValidator.getInstance());
        emailField.setRequired(false);
        form.add(emailField);

        final PasswordTextField passwordField =
                new PasswordTextField("password", new PropertyModel<String>(this, "password"));
        passwordField.setResetPassword(false);
        form.add(passwordField);

        final PasswordTextField passwordCheckField =
                new PasswordTextField("password-check", new PropertyModel<String>(this, "passwordCheck"));
        passwordCheckField.setRequired(false);
        passwordCheckField.setResetPassword(false);
        form.add(passwordCheckField);

        form.add(new EqualPasswordInputValidator(passwordField, passwordCheckField));

        form.add(new AjaxButton("create-button", form) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {

                String username = user.getUsername();

                try {
                    user.create();
                    user.savePassword(password);
                    HippoEventBus eventBus = HippoServiceRegistry.getService(HippoEventBus.class);
                    if (eventBus != null) {
                        UserSession userSession = UserSession.get();
                        HippoEvent event = new HippoEvent(userSession.getApplicationName())
                                .user(userSession.getJcrSession().getUserID())
                                .action("create-user")
                                .category(HippoEventConstants.CATEGORY_USER_MANAGEMENT)
                                .message("created user " + username);
                        eventBus.post(event);
                    }
                    Session.get().info(getString("user-created", new Model<User>(user)));
                    // one up
                    List<IBreadCrumbParticipant> l = breadCrumbModel.allBreadCrumbParticipants();
                    breadCrumbModel.setActive(l.get(l.size() - 2));
                } catch (RepositoryException e) {
                    Session.get().warn(getString("user-create-failed", new Model<User>(user)));
                    log.error("Unable to create user '" + username + "' : ", e);
                }
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form form) {
                // make sure the feedback panel is shown
                target.add(CreateUserPanel.this);
            }
        });

        // add a button that can be used to submit the form via ajax
        form.add(new AjaxButton("cancel-button") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                // one up
                List<IBreadCrumbParticipant> l = breadCrumbModel.allBreadCrumbParticipants();
                breadCrumbModel.setActive(l.get(l.size() - 2));
            }
        }.setDefaultFormProcessing(false));
    }


    public IModel<String> getTitle(Component component) {
        return new StringResourceModel("user-create", component, null);
    }

    @SuppressWarnings({UNUSED})
    public String getPassword() {
        return password;
    }

    @SuppressWarnings({UNUSED})
    public void setPassword(String password) {
        this.password = password;
    }

    @SuppressWarnings({UNUSED})
    public String getPasswordCheck() {
        return passwordCheck;
    }

    @SuppressWarnings({UNUSED})
    public void setPasswordCheck(String passwordCheck) {
        this.passwordCheck = passwordCheck;
    }

}
