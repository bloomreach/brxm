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
package org.hippoecm.frontend.plugins.cms.admin.permissions;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.RepositoryException;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
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
import org.hippoecm.frontend.plugins.cms.admin.AdminBreadCrumbPanel;
import org.hippoecm.frontend.plugins.cms.admin.domains.Domain;
import org.hippoecm.frontend.plugins.cms.admin.groups.Group;
import org.hippoecm.frontend.plugins.cms.admin.widgets.AjaxLinkLabel;
import org.hippoecm.frontend.session.UserSession;
import org.onehippo.event.HippoEventBus;
import org.onehippo.event.audit.HippoAuditEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SetPermissionsPanel extends AdminBreadCrumbPanel {
    @SuppressWarnings("unused")
    private static final String SVN_ID = "$Id$";
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(SetPermissionsPanel.class);

    private Group selectedGroup;
    private String selectedRole;
    private final IModel model;
    private ListView roleList;
    //private final ListView externalList;
    private final Domain domain;

    public SetPermissionsPanel(final String id, final IBreadCrumbModel breadCrumbModel, final IModel model) {
        super(id, breadCrumbModel);
        setOutputMarkupId(true);
        this.model = model;
        this.domain = (Domain) model.getObject();

        // All local groups
        Form form = new Form("form");

        AjaxButton submit = new AjaxButton("submit", form) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                try {
                    domain.addGroupToRole(selectedRole, selectedGroup.getGroupname());
                    info(getString("permissions-group-added", model));
                    final UserSession userSession = UserSession.get();
                    HippoAuditEvent event = new HippoAuditEvent(userSession.getApplicationName())
                            .user(userSession.getJcrSession().getUserID())
                            .action("grant-role")
                            .category(HippoAuditEvent.CATEGORY_PERMISSIONS_MANAGEMENT)
                            .message(
                                    "grant " + selectedRole + " role to group " + selectedGroup.getGroupname() + " for domain " + domain.getName());
                    HippoEventBus.post(event);
                    this.removeAll();
                    target.addComponent(SetPermissionsPanel.this);
                } catch (RepositoryException e) {
                    error(getString("permissions-group-add-failed", model));
                    log.error("Failed to add permission", e);
                }
            }

        };
        form.add(submit);

        List<String> allRoles = Group.getAllRoles();
        DropDownChoice roleChoice = new DropDownChoice("roles-select", new PropertyModel(this, "selectedRole"), allRoles);
        roleChoice.setNullValid(false);
        roleChoice.setRequired(true);
        form.add(roleChoice);
        
        List<Group> allGroups = Group.getAllGroups();
        DropDownChoice groupChoice = new DropDownChoice("groups-select", new PropertyModel(this, "selectedGroup"), allGroups,
                new ChoiceRenderer("groupname"));
        groupChoice.setNullValid(false);
        groupChoice.setRequired(true);
        form.add(groupChoice);
        
        
        add(form);

        // local memberships
        Label currentPerms = new Label("current-permissions-label", new ResourceModel("permissions-current"));
        add(currentPerms);
        roleList = new RoleListView("role-row");
        add(roleList);
    }

    /** list view to be nested in the form. */
    private final class DomainRoleListView extends ListView {
        private static final long serialVersionUID = 1L;
        private final String role;

        public DomainRoleListView(final String id, final String role, final List<String> groups) {
            super(id, groups);
            setReuseItems(false);
            setOutputMarkupId(true);
            this.role = role;
        }

        protected void populateItem(final ListItem item) {
            item.setOutputMarkupId(true);
            final String group = (String) item.getDefaultModelObjectAsString();
            item.add(new Label("group-label", group));
            item.add(new AjaxLinkLabel("group-remove", new ResourceModel("permissions-remove-action")) {
                private static final long serialVersionUID = 1L;

                @Override
                public void onClick(AjaxRequestTarget target) {
                    try {
                        domain.removeGroupFromRole(role, group);
                        final UserSession userSession = UserSession.get();
                        HippoAuditEvent event = new HippoAuditEvent(userSession.getApplicationName())
                                .user(userSession.getJcrSession().getUserID())
                                .action("revoke-role")
                                .category(HippoAuditEvent.CATEGORY_PERMISSIONS_MANAGEMENT)
                                .message(
                                        "revoke " + selectedRole + " role from group " + group + " for domain " + domain.getName());
                        HippoEventBus.post(event);
                        info(getString("permissions-group-removed", model));
                        log.info("Revoke " + selectedRole + " role from group " + group + " for domain " + domain.getName());
                        this.removeAll();
                        target.addComponent(SetPermissionsPanel.this);
                    } catch (RepositoryException e) {
                        error(getString("permissions-group-remove-failed", model));
                        log.error("Failed to revoke permission", e);
                    }
                }
            });
        }
    }

    
    
    /** list view to be nested in the form. */
    private final class RoleListView extends ListView {
        private static final long serialVersionUID = 1L;
        
        public RoleListView(final String id) {
            super(id, Group.getAllRoles());
            setReuseItems(false);
        }

        protected void populateItem(final ListItem item) {
            String role = item.getDefaultModelObjectAsString();
            item.add(new Label("role", role));
            List<String> groups = new ArrayList<String>();
            for (Domain.AuthRole authRole : domain.getAuthRoles().values()) {
                if (role.equals(authRole.getRole())) {
                    groups.addAll(authRole.getGroupnames());
                }
            }
            
            item.add(new DomainRoleListView("groups", role, groups));
            
        }
    }

    public IModel<String> getTitle(Component component) {
        return new StringResourceModel("permissions-set-title", component, model);
    }

}
