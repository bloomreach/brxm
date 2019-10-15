/*
 *  Copyright 2008-2019 Hippo B.V. (http://www.onehippo.com)
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

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.extensions.breadcrumb.IBreadCrumbModel;
import org.apache.wicket.extensions.validation.validator.RfcCompliantEmailAddressValidator;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.plugins.cms.admin.AdminBreadCrumbPanel;
import org.hippoecm.frontend.session.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EditUserPanel extends AdminBreadCrumbPanel {

    private static final Logger log = LoggerFactory.getLogger(EditUserPanel.class);

    private final IModel model;

    public EditUserPanel(final String id, final IBreadCrumbModel breadCrumbModel, final IModel<User> model) {
        super(id, breadCrumbModel);
        setOutputMarkupId(true);

        this.model = model;

        // add form with markup id setter so it can be updated via ajax
        final Form<User> form = new Form<>("form", new CompoundPropertyModel<>(model));
        form.setOutputMarkupId(true);
        add(form);

        form.add(new TextField("firstName"));
        form.add(new TextField("lastName"));

        final TextField<String> email = new TextField<>("email");
        email.add(RfcCompliantEmailAddressValidator.getInstance());
        email.setRequired(false);
        form.add(email);

        form.add(new CheckBox("active"));

        // add a button that can be used to submit the form via ajax
        final AjaxButton saveButton = new AjaxButton("save-button", form) {
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                final User user = model.getObject();
                final String username = user.getUsername();
                try {
                    user.save();
                    log.info("User '{}' saved by {}", username, UserSession.get().getJcrSession().getUserID());
                    final String infoMsg = getString("user-saved", model);
                    activateParentAndDisplayInfo(infoMsg);
                } catch (RepositoryException e) {
                    target.add(EditUserPanel.this);
                    warn(getString("user-save-failed", model));
                    log.error("Unable to save user '{}' : ", username, e);
                }
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form form) {
                // make sure the feedback panel is shown
                target.add(EditUserPanel.this);
            }
        };
        form.add(saveButton);
        form.setDefaultButton(saveButton);

        // add a cancel/back button
        form.add(new AjaxButton("cancel-button") {
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                activateParent();
            }
        }.setDefaultFormProcessing(false));
    }

    @Override
    public IModel<String> getTitle(Component component) {
        return new StringResourceModel("user-edit-title", component).setModel(model);
    }
}
