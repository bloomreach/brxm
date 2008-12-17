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
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.SimpleFormComponentLabel;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.validation.validator.EmailAddressValidator;
import org.hippoecm.frontend.session.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EditUserPanel extends Panel {
    @SuppressWarnings("unused")
    private static final String SVN_ID = "$Id$";
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(EditUserPanel.class);

    private final Form form;

    public EditUserPanel(final String id, final IModel userModel, final UsersPanel panel) {
        super(id);
        setOutputMarkupId(true);

        // title
        add(new Label("title", new StringResourceModel("user-edit-title", userModel)));

        // add form with markup id setter so it can be updated via ajax
        form = new Form("form", new CompoundPropertyModel(userModel));
        form.setOutputMarkupId(true);
        add(form);

        FormComponent fc;

        fc = new TextField("firstName");
        fc.setLabel(new ResourceModel("label.firstname"));
        form.add(fc);
        form.add(new SimpleFormComponentLabel("firstname-label", fc));

        fc = new TextField("lastName");
        fc.setLabel(new ResourceModel("label.lastname"));
        form.add(fc);
        form.add(new SimpleFormComponentLabel("lastname-label", fc));

        fc = new TextField("email");
        fc.add(EmailAddressValidator.getInstance());
        fc.setRequired(false);
        fc.setLabel(new ResourceModel("label.email"));
        form.add(fc);
        form.add(new SimpleFormComponentLabel("email-label", fc));


        fc = new CheckBox("active");
        fc.setLabel(new ResourceModel("label.active"));
        form.add(fc);
        form.add(new SimpleFormComponentLabel("active-label", fc));

        
        // add a button that can be used to submit the form via ajax
        form.add(new AjaxButton("save-button", form) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                User user = (User) userModel.getObject();
                String username = user.getUsername();
                try {
                    user.save();
                    log.info("User '" + username + "' saved by "
                            + ((UserSession) Session.get()).getCredentials().getStringValue("username"));
                    UserDataProvider.countMinusOne();
                    Session.get().info(getString("user-saved", userModel));
                    panel.showView(target, userModel);
                } catch (RepositoryException e) {
                    Session.get().warn(getString("user-save-failed", userModel));
                    log.error("Unable to save user '" + username + "' : ", e);
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
