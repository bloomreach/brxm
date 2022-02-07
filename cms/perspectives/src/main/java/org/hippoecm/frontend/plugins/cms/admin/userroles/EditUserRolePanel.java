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

import javax.jcr.AccessDeniedException;
import javax.jcr.RepositoryException;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.extensions.breadcrumb.IBreadCrumbModel;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.form.PostOnlyForm;
import org.hippoecm.frontend.plugins.cms.admin.AdminBreadCrumbPanel;
import org.hippoecm.frontend.plugins.cms.admin.SecurityManagerHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bloomreach.xm.repository.security.UserRole;
import com.bloomreach.xm.repository.security.UserRoleBean;

public class EditUserRolePanel extends AdminBreadCrumbPanel {
    private static final Logger log = LoggerFactory.getLogger(EditUserRolePanel.class);

    private final IModel<UserRole> userRoleModel;

    public EditUserRolePanel(final String id, final IBreadCrumbModel breadCrumbModel, final IModel<UserRole> userRoleModel) {
        super(id, breadCrumbModel);

        this.userRoleModel = userRoleModel;

        // add form with markup id setter so it can be updated via ajax
        final Form<?> form = new PostOnlyForm<>("form");
        form.setOutputMarkupId(true);
        add(form);

        final TextField<String> descriptionField = new TextField<>("description", Model.of(userRoleModel.getObject().getDescription()));
        form.add(descriptionField);
        form.add(new Label("name", userRoleModel.getObject().getName())); // userroles cannot be renamed, so no model needed

        // add a button that can be used to submit the form via ajax
        final AjaxButton saveButton = new AjaxButton("save-button", form) {

            @Override
            protected void onSubmit(final AjaxRequestTarget target) {
                String description = descriptionField.getDefaultModelObjectAsString();
                if (StringUtils.isBlank(description)) {
                    description = null;
                }
                final UserRole userRole = userRoleModel.getObject();
                try {
                    // create a userRoleTemplate from the current backend UserRole
                    final UserRoleBean userRoleTemplate =
                            new UserRoleBean(SecurityManagerHelper.getUserRolesProvider().getRole(userRole.getName()));
                    userRoleTemplate.setDescription(description);
                    userRoleModel.setObject(SecurityManagerHelper.getUserRolesManager().updateUserRole(userRoleTemplate));
                    activateParentAndDisplayInfo(getString("userrole-saved", userRoleModel));
                } catch (AccessDeniedException e) {
                    target.add(EditUserRolePanel.this);
                    error(getString("userrole-save-denied", userRoleModel));
                    log.error("Not allowed to save userrole '{}': ", userRole.getName(), e);
                } catch (RepositoryException e) {
                    target.add(EditUserRolePanel.this);
                    error(getString("userrole-save-failed", userRoleModel));
                    log.error("Unable to save userrole '{}' : ", userRole.getName(), e);
                }
            }
        };
        form.add(saveButton);
        form.setDefaultButton(saveButton);

        form.add(new AjaxButton("cancel-button") {
            @Override
            protected void onSubmit(final AjaxRequestTarget target) {
                activateParent();
            }
        }.setDefaultFormProcessing(false));
    }

    @Override
    public IModel<String> getTitle(final Component component) {
        return new StringResourceModel("userrole-edit-title", component).setModel(userRoleModel);
    }

}
