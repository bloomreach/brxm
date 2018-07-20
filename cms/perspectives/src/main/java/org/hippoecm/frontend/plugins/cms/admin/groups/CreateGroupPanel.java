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
package org.hippoecm.frontend.plugins.cms.admin.groups;

import javax.jcr.RepositoryException;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.extensions.breadcrumb.IBreadCrumbModel;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.ValidationError;
import org.apache.wicket.validation.validator.StringValidator;
import org.hippoecm.frontend.plugins.cms.admin.AdminBreadCrumbPanel;
import org.hippoecm.frontend.util.EventBusUtils;
import org.onehippo.cms7.event.HippoEventConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreateGroupPanel extends AdminBreadCrumbPanel {
    private static final Logger log = LoggerFactory.getLogger(CreateGroupPanel.class);

    private final DetachableGroup groupModel = new DetachableGroup();

    public CreateGroupPanel(final String id, final IBreadCrumbModel breadCrumbModel) {
        super(id, breadCrumbModel);
        setOutputMarkupId(true);

        // add form with markup id setter so it can be updated via ajax
        final CompoundPropertyModel<Group> formModel = new CompoundPropertyModel<>(groupModel);
        final Form<Group> form = new Form<>("form", formModel);
        form.setOutputMarkupId(true);
        add(form);

        final RequiredTextField<String> groupNameField = new RequiredTextField<>("groupname");
        groupNameField.add(StringValidator.minimumLength(2));
        groupNameField.add(new GroupNameValidator());
        form.add(groupNameField);

        form.add(new TextField("description"));

        form.add(new AjaxButton("create-button", form) {
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                final Group group = groupModel.getGroup();
                final String groupName = group.getGroupname();
                try {
                    group.create();
                    EventBusUtils.post(
                        "create-group",
                        HippoEventConstants.CATEGORY_GROUP_MANAGEMENT,
                        "added group " + groupName
                    );
                    activateParentAndDisplayInfo(getString("group-created", groupModel));
                } catch (RepositoryException e) {
                    target.add(CreateGroupPanel.this);
                    error(getString("group-create-failed", groupModel));
                    log.error("Unable to create group '{}' : ", groupName, e);
                }
            }
            @Override
            protected void onError(AjaxRequestTarget target, Form form) {
                // make sure the feedback panel is shown
                target.add(CreateGroupPanel.this);
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
        return new StringResourceModel("group-create", component);
    }

    private static final class GroupNameValidator extends StringValidator {

        @Override
        public void validate(final IValidatable<String> validatable) {
            super.validate(validatable);

            final String groupName = validatable.getValue();
            if (Group.exists(groupName)) {
                validatable.error(new ValidationError(this, "exists"));
            }
        }

    }

}
