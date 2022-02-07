/*
 *  Copyright 2019-2020 Hippo B.V. (http://www.onehippo.com)
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

import java.util.Collections;

import javax.jcr.AccessDeniedException;
import javax.jcr.RepositoryException;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.extensions.breadcrumb.IBreadCrumbModel;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.model.util.MapModel;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;
import org.apache.wicket.validation.validator.StringValidator;
import org.hippoecm.frontend.form.PostOnlyForm;
import org.hippoecm.frontend.plugins.cms.admin.AdminBreadCrumbPanel;
import org.hippoecm.frontend.plugins.cms.admin.SecurityManagerHelper;
import org.hippoecm.frontend.util.EventBusUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bloomreach.xm.repository.security.UserRoleBean;

import static org.onehippo.cms7.event.HippoEventConstants.CATEGORY_USERROLE_MANAGEMENT;

public class CreateUserRolePanel extends AdminBreadCrumbPanel {
    private static final Logger log = LoggerFactory.getLogger(CreateUserRolePanel.class);

    public CreateUserRolePanel(final String id, final IBreadCrumbModel breadCrumbModel) {
        super(id, breadCrumbModel);

        // add form with markup id setter so it can be updated via ajax
        final Form<?> form = new PostOnlyForm<>("form");
        form.setOutputMarkupId(true);
        add(form);

        final RequiredTextField<String> userRoleNameField = new RequiredTextField<>("name", Model.of(""));
        userRoleNameField.add(StringValidator.minimumLength(2));
        userRoleNameField.add(new UserRoleNameValidator());
        form.add(userRoleNameField);
        final TextField<String> descriptionField = new TextField<>("description", Model.of(""));
        form.add(descriptionField);

        final AjaxButton createButton = new AjaxButton("create-button", form) {
            @Override
            protected void onSubmit(final AjaxRequestTarget target) {
                final String name = userRoleNameField.getDefaultModelObjectAsString();
                String description = descriptionField.getDefaultModelObjectAsString();
                final MapModel nameModel = new MapModel<>(Collections.singletonMap("name", name));

                if (StringUtils.isBlank(description)) {
                    description = null;
                }
                try {
                    SecurityManagerHelper.getUserRolesManager().addUserRole(new UserRoleBean(name, description));
                    EventBusUtils.post("create-userrole", CATEGORY_USERROLE_MANAGEMENT, "added userrole " + name);
                    activateParentAndDisplayInfo(getString("userrole-created", nameModel));
                } catch (AccessDeniedException e) {
                    target.add(CreateUserRolePanel.this);
                    error(getString("userrole-create-denied", nameModel));
                    log.error("Not allowed to create userrole '{}': ", name, e);
                } catch (RepositoryException e) {
                    target.add(CreateUserRolePanel.this);
                    error(getString("userrole-create-failed", nameModel));
                    log.error("Unable to create userrole '{}' : ", name, e);
                }
            }

            @Override
            protected void onError(final AjaxRequestTarget target) {
                // make sure the feedback panel is shown
                target.add(CreateUserRolePanel.this);
            }
        };
        form.add(createButton);
        form.setDefaultButton(createButton);

        // add a button that can be used to submit the form via ajax
        form.add(new AjaxButton("cancel-button") {
            @Override
            protected void onSubmit(final AjaxRequestTarget target) {
                activateParent();
            }
        }.setDefaultFormProcessing(false));
    }

    @Override
    public IModel<String> getTitle(final Component component) {
        return new StringResourceModel("userrole-create", component);
    }

    private static final class UserRoleNameValidator implements IValidator<String> {

        @Override
        public void validate(final IValidatable<String> validatable) {
            final String name = validatable.getValue();
            if (SecurityManagerHelper.getUserRolesProvider().hasRole(name)) {
                validatable.error(new ValidationError(this, "exists"));
            }
        }

    }

}
