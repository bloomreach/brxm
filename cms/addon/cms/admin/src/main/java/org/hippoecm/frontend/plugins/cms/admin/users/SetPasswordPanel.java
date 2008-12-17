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

import javax.jcr.RepositoryException;

import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.validation.EqualPasswordInputValidator;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.validation.validator.StringValidator;
import org.hippoecm.frontend.session.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SetPasswordPanel extends Panel {
    @SuppressWarnings("unused")
    private static final String SVN_ID = "$Id$";
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(SetPasswordPanel.class);

    private final Form form;

    public SetPasswordPanel(final String id, final IModel userModel, final UsersPanel panel) {
        super(id);
        setOutputMarkupId(true);

        // title
        add(new Label("title", new StringResourceModel("user-set-password-title", userModel)));

        // add form with markup id setter so it can be updated via ajax
        form = new Form("form");
        form.setOutputMarkupId(true);
        add(form);

        final PasswordTextField password = new PasswordTextField("password", new Model(""));
        password.setResetPassword(false);
        password.add(StringValidator.minimumLength(4));
        form.add(password);

        final PasswordTextField passwordCheck = new PasswordTextField("password-check");
        passwordCheck.setModel(password.getModel());
        passwordCheck.setResetPassword(false);
        passwordCheck.add(StringValidator.minimumLength(4));
        form.add(passwordCheck);

        form.add(new EqualPasswordInputValidator(password, passwordCheck));

        // add a button that can be used to submit the form via ajax
        form.add(new AjaxButton("set-button", form) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                User user = (User) userModel.getObject();
                String username = user.getUsername();
                try {
                    user.savePassword(password.getModelObjectAsString());
                    log.info("User '" + username + "' password set by "
                            + ((UserSession) Session.get()).getCredentials().getStringValue("username"));
                    Session.get().info(getString("user-password-set", userModel));
                    panel.showView(target, userModel);
                } catch (RepositoryException e) {
                    Session.get().warn(getString("user-save-failed", userModel));
                    log.error("Unable to set password for user '" + username + "' : ", e);
                    panel.refresh();
                }
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form form) {
                panel.refresh();
            }
        });

        // add a button that can be used to submit the form via ajax
        form.add(new AjaxButton("cancel-button") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                panel.showView(target, userModel);
            }
        }.setDefaultFormProcessing(false));
    }
}
