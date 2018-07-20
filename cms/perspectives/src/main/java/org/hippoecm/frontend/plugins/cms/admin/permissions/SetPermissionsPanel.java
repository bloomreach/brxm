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
package org.hippoecm.frontend.plugins.cms.admin.permissions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.jcr.RepositoryException;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.extensions.breadcrumb.IBreadCrumbModel;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.dialog.HippoForm;
import org.hippoecm.frontend.plugins.cms.admin.AdminBreadCrumbPanel;
import org.hippoecm.frontend.plugins.cms.admin.domains.Domain;
import org.hippoecm.frontend.plugins.cms.admin.domains.Domain.AuthRole;
import org.hippoecm.frontend.plugins.cms.admin.groups.Group;
import org.hippoecm.frontend.plugins.cms.admin.widgets.AjaxLinkLabel;
import org.hippoecm.frontend.util.EventBusUtils;
import org.onehippo.cms7.event.HippoEventConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SetPermissionsPanel extends AdminBreadCrumbPanel {
    private static final Logger log = LoggerFactory.getLogger(SetPermissionsPanel.class);

    private final HippoForm hippoForm;
    private final IModel<Domain> model;
    private final Domain domain;

    private Group selectedGroup;
    private String selectedRole;

    @SuppressWarnings("unused")
    public Group getSelectedGroup() {
        return selectedGroup;
    }

    @SuppressWarnings("unused")
    public String getSelectedRole() {
        return selectedRole;
    }

    /**
     * Constructs a new SetPermissionsPanel.
     *
     * @param id              the id
     * @param breadCrumbModel the breadCrumbModel
     * @param model           the model
     */
    public SetPermissionsPanel(final String id, final IBreadCrumbModel breadCrumbModel, final IModel<Domain> model) {
        super(id, breadCrumbModel);
        setOutputMarkupId(true);
        this.model = model;
        this.domain = model.getObject();

        add(new Label("permissions-set-title", new StringResourceModel("permissions-set-title", this).setModel(model)));

        // All local groups
        hippoForm = new HippoForm("form");

        final AjaxButton submit = new AjaxButton("submit", hippoForm) {
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                // clear old feedbacks prior showing new ones
                hippoForm.clearFeedbackMessages();

                try {
                    domain.addGroupToRole(selectedRole, selectedGroup.getGroupname());
                    showInfo(getString("permissions-group-added", model));

                    EventBusUtils.post("grant-role", HippoEventConstants.CATEGORY_PERMISSIONS_MANAGEMENT,
                            String.format("grant %s role to group %s for domain %s",
                                    selectedRole, selectedGroup.getGroupname(), domain.getName()));

                    removeAll();
                    target.add(SetPermissionsPanel.this);
                } catch (RepositoryException e) {
                    showError(getString("permissions-group-add-failed", model));
                    log.error("Failed to add permission", e);
                }
            }

            @Override
            public boolean isEnabled() {
                return selectedGroup != null && selectedRole != null;
            }
        };
        hippoForm.add(submit);

        final List<String> allRoles = Group.getAllRoles();
        final DropDownChoice<String> roleChoice = new DropDownChoice<>("roles-select",
                new PropertyModel<>(this, "selectedRole"), allRoles);
        roleChoice.setNullValid(false);
        roleChoice.add(new AjaxFormComponentUpdatingBehavior("onchange") {
            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                target.add(submit);
            }
        });
        hippoForm.add(roleChoice);

        final List<Group> allGroups = Group.getAllGroups();
        final DropDownChoice<Group> groupChoice = new DropDownChoice<>("groups-select",
                new PropertyModel<>(this, "selectedGroup"), allGroups, new ChoiceRenderer<>("groupname"));
        groupChoice.setNullValid(false);
        groupChoice.add(new AjaxFormComponentUpdatingBehavior("onchange") {
            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                target.add(submit);
            }
        });
        hippoForm.add(groupChoice);

        add(hippoForm);

        final ListView roleList = new RoleListView("role-row");
        add(roleList);
    }

    private void showError(final String s) {
        hippoForm.error(s);
    }

    @SuppressWarnings("unused")
    public void setSelectedGroup(final Group selectedGroup) {
        this.selectedGroup = selectedGroup;
    }

    @SuppressWarnings("unused")
    public void setSelectedRole(final String selectedRole) {
        this.selectedRole = selectedRole;
    }

    /**
     * list view to be nested in the form.
     */
    private final class DomainRoleListView extends ListView<String> {
        private final String role;

        public DomainRoleListView(final String id, final String role, final List<String> groups) {
            super(id, groups);
            setReuseItems(false);
            setOutputMarkupId(true);
            this.role = role;
        }

        @Override
        protected void populateItem(final ListItem<String> item) {
            item.setOutputMarkupId(true);
            final String groupName = item.getModelObject();
            item.add(new Label("group-label", groupName));
            item.add(new AjaxLinkLabel("group-remove", new ResourceModel("permissions-remove-action")) {
                @Override
                public void onClick(AjaxRequestTarget target) {
                    // clear old feedbacks prior showing new ones
                    hippoForm.clearFeedbackMessages();
                    try {
                        domain.removeGroupFromRole(role, groupName);

                        final String message = String.format("revoke %s role from group %s for domain %s",
                                selectedRole, groupName, domain.getName());

                        EventBusUtils.post("revoke-role", HippoEventConstants.CATEGORY_PERMISSIONS_MANAGEMENT, message);
                        showInfo(getString("permissions-group-removed", model));
                        log.info(message);
                        removeAll();
                        target.add(SetPermissionsPanel.this);
                    } catch (RepositoryException e) {
                        showError(getString("permissions-group-remove-failed", model));
                        log.error("Failed to revoke permission", e);
                    }
                }
            });
        }
    }

    private void showInfo(final String s) {
        hippoForm.info(s);
    }

    /**
     * list view to be nested in the form.
     */
    private final class RoleListView extends ListView<String> {

        RoleListView(final String id) {
            super(id, Group.getAllRoles());
            setReuseItems(false);
        }

        @Override
        protected void populateItem(final ListItem<String> item) {
            final String role = item.getModelObject();
            item.add(new Label("role", role));

            final AuthRole authRole = domain.getAuthRoles().get(role);
            final List<String> groups = authRole != null ?
                    new ArrayList<>(authRole.getGroupnames()) : Collections.emptyList();

            item.add(new DomainRoleListView("groups", role, groups));
        }
    }

    @Override
    public IModel<String> getTitle(Component component) {
        return new StringResourceModel("permissions-set-title", component).setModel(model);
    }

}
