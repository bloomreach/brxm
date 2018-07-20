/*
 *  Copyright 2008-2018 Hippo B.V. (http://www.onehippo.com)
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

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.extensions.breadcrumb.IBreadCrumbModel;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.validation.EqualPasswordInputValidator;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.dialog.HippoForm;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugins.cms.admin.AdminBreadCrumbPanel;
import org.hippoecm.frontend.plugins.cms.admin.validators.PasswordStrengthValidator;
import org.hippoecm.frontend.session.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SetPasswordPanel extends AdminBreadCrumbPanel {
    private static final Logger log = LoggerFactory.getLogger(SetPasswordPanel.class);

    private final IModel model;
    private String password;
    private String checkPassword;

    public SetPasswordPanel(final String id, final IBreadCrumbModel breadCrumbModel, final IModel<User> model,
                            final IPluginContext context) {
        super(id, breadCrumbModel);
        setOutputMarkupId(true);

        this.model = model;
        final User user = model.getObject();

        // add form with markup id setter so it can be updated via ajax
        final Form form = new HippoForm("form");
        form.setOutputMarkupId(true);
        add(form);

        final PropertyModel<String> passwordModel = new PropertyModel<>(this, "password");
        final PasswordTextField passwordField = new PasswordTextField("password", passwordModel);
        passwordField.setResetPassword(false);
        passwordField.add(new PasswordStrengthValidator(form, context, model));
        form.add(passwordField);

        final PropertyModel<String> checkPasswordModel = new PropertyModel<>(this, "checkPassword");
        final PasswordTextField passwordCheckField = new PasswordTextField("password-check", checkPasswordModel);
        passwordCheckField.setModel(passwordField.getModel());
        passwordCheckField.setRequired(false);
        passwordCheckField.setResetPassword(false);
        form.add(passwordCheckField);

        form.add(new EqualPasswordInputValidator(passwordField, passwordCheckField));

        // add a button that can be used to submit the form via ajax
        form.add(new AjaxButton("set-button", form) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                final String username = user.getUsername();
                try {
                    user.savePassword(password);
                    final Session jcrSession = UserSession.get().getJcrSession();
                    log.info("User '{}' password set by '{}'", username, jcrSession.getUserID());
                    activateParentAndDisplayInfo(getString("user-password-set", model));
                } catch (RepositoryException e) {
                    target.add(SetPasswordPanel.this);
                    warn(getString("user-save-failed", model));
                    log.error("Unable to set password for user '{}' : ", username, e);
                }
            }
            @Override
            protected void onError(AjaxRequestTarget target, Form form) {
                // make sure the feedback panel is shown
                target.add(SetPasswordPanel.this);
            }
        });

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
        return new StringResourceModel("user-set-password-title", component).setModel(model);
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getCheckPassword() {
        return checkPassword;
    }

    public void setCheckPassword(String checkPassword) {
        this.checkPassword = checkPassword;
    }
}
