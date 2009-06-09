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
package org.hippoecm.frontend.plugins.cms.admin.users;

import java.util.List;

import javax.jcr.RepositoryException;

import org.apache.wicket.Component;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.extensions.breadcrumb.IBreadCrumbModel;
import org.apache.wicket.extensions.breadcrumb.IBreadCrumbParticipant;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.form.validation.EqualPasswordInputValidator;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.validator.EmailAddressValidator;
import org.apache.wicket.validation.validator.StringValidator;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugins.cms.admin.crumbs.AdminBreadCrumbPanel;
import org.hippoecm.frontend.plugins.cms.admin.validators.PasswordStrengthValidator;
import org.hippoecm.frontend.plugins.cms.admin.validators.UsernameValidator;
import org.hippoecm.frontend.session.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreateUserPanel extends AdminBreadCrumbPanel {
    @SuppressWarnings("unused")
    private static final String SVN_ID = "$Id$";
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(CreateUserPanel.class);

    private final Form form;

    private DetachableUser userModel = new DetachableUser();

    public CreateUserPanel(final String id, final IPluginContext context, final IBreadCrumbModel breadCrumbModel) {
        super(id, breadCrumbModel);
        setOutputMarkupId(true);

        // add form with markup id setter so it can be updated via ajax
        form = new Form("form", new CompoundPropertyModel(userModel));
        form.setOutputMarkupId(true);
        add(form);

        FormComponent fc;

        fc = new RequiredTextField("username");
        fc.add(StringValidator.minimumLength(2));
        fc.add(new UsernameValidator());
        form.add(fc);

        fc = new TextField("firstName");
        form.add(fc);

        fc = new TextField("lastName");
        form.add(fc);

        fc = new TextField("email");
        fc.add(EmailAddressValidator.getInstance());
        fc.setRequired(false);
        form.add(fc);

        final PasswordTextField password = new PasswordTextField("password", new Model(""));
        password.setResetPassword(false);
        password.add(new PasswordStrengthValidator());
        form.add(password);

        final PasswordTextField passwordCheck = new PasswordTextField("password-check");
        passwordCheck.setRequired(false);
        passwordCheck.setModel(password.getModel());
        passwordCheck.setResetPassword(false);
        form.add(passwordCheck);

        form.add(new EqualPasswordInputValidator(password, passwordCheck));

        form.add(new AjaxButton("create-button", form) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                User user = userModel.getUser();
                String username = user.getUsername();
                try {
                    user.create();
                    user.savePassword(password.getModelObjectAsString());
                    log.info("User '" + username + "' created by "
                            + ((UserSession) Session.get()).getCredentials().getStringValue("username"));
                    UserDataProvider.setDirty();
                    Session.get().info(getString("user-created", userModel));
                    // one up
                    List<IBreadCrumbParticipant> l = breadCrumbModel.allBreadCrumbParticipants();
                    breadCrumbModel.setActive(l.get(l.size() -2));
                } catch (RepositoryException e) {
                    Session.get().warn(getString("user-create-failed", userModel));
                    log.error("Unable to create user '" + username + "' : ", e);
                }
            }
        });
        
        // add a button that can be used to submit the form via ajax
        form.add(new AjaxButton("cancel-button") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                // one up
                List<IBreadCrumbParticipant> l = breadCrumbModel.allBreadCrumbParticipants();
                breadCrumbModel.setActive(l.get(l.size() -2));
            }
        }.setDefaultFormProcessing(false));
    }

    
    public IModel getTitle(Component component) {
        return new StringResourceModel("user-create", component, null);
    }

}
