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
import org.apache.wicket.extensions.breadcrumb.IBreadCrumbModel;
import org.apache.wicket.extensions.breadcrumb.IBreadCrumbParticipant;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.dialog.Confirm;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.model.ReadOnlyModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugins.cms.admin.AdminBreadCrumbPanel;
import org.hippoecm.frontend.plugins.cms.admin.domains.Domain;
import org.hippoecm.frontend.plugins.cms.admin.domains.Domain.AuthRole;
import org.hippoecm.frontend.plugins.cms.admin.domains.DomainDataProvider;
import org.hippoecm.frontend.plugins.cms.admin.permissions.PermissionBean;
import org.hippoecm.frontend.plugins.cms.admin.permissions.ViewDomainActionLink;
import org.hippoecm.frontend.plugins.cms.admin.users.DetachableUser;
import org.hippoecm.frontend.plugins.cms.admin.users.User;
import org.hippoecm.frontend.plugins.cms.admin.users.ViewUserLinkLabel;
import org.hippoecm.frontend.plugins.cms.admin.widgets.AjaxLinkLabel;
import org.hippoecm.frontend.plugins.standards.panelperspective.breadcrumb.PanelPluginBreadCrumbLink;
import org.hippoecm.frontend.util.EventBusUtils;
import org.onehippo.cms7.event.HippoEventConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Panel showing information regarding the groups.
 */
public class ViewGroupPanel extends AdminBreadCrumbPanel {

    private static final Logger log = LoggerFactory.getLogger(ViewGroupPanel.class);

    private final Group group;
    private final GroupMembersListView groupMembersListView;
    private final IDialogService dialogService;

    public ViewGroupPanel(final String id, final IPluginContext context, final IBreadCrumbModel breadCrumbModel,
                          final Group group) {
        super(id, breadCrumbModel);
        setOutputMarkupId(true);

        this.group = group;
        dialogService = context.getService(IDialogService.class.getName(), IDialogService.class);

        final IModel<Group> groupModel = Model.of(group);
        add(new Label("view-group-panel-title", new StringResourceModel("group-view-title", this).setModel(groupModel)));

        // common group properties
        add(new Label("groupname", group.getGroupname())); // groups cannot be renamed, so no model needed
        add(new Label("description", ReadOnlyModel.of(group::getDescription)));

        final PermissionsListView permissionsListView = new PermissionsListView("permissions");
        add(permissionsListView);

        // actions
        final PanelPluginBreadCrumbLink edit = new PanelPluginBreadCrumbLink("edit-group", breadCrumbModel) {
            protected IBreadCrumbParticipant getParticipant(final String componentId) {
                return new EditGroupPanel(componentId, breadCrumbModel, groupModel);
            }
        };
        edit.setVisible(!group.isExternal());
        add(edit);

        final PanelPluginBreadCrumbLink members = new PanelPluginBreadCrumbLink("set-group-members", breadCrumbModel) {
            @Override
            protected IBreadCrumbParticipant getParticipant(final String componentId) {
                return new SetMembersPanel(componentId, breadCrumbModel, groupModel);
            }
        };
        members.setVisible(!group.isExternal());
        add(members);

        add(new AjaxLinkLabel("delete-group", new ResourceModel("group-delete")) {
            @Override
            public void onClick(final AjaxRequestTarget target) {
                final Confirm confirm = new Confirm(
                        getString("group-delete-title", groupModel),
                        getString("group-delete-text", groupModel)
                ).ok(() -> {
                    deleteGroup(group);
                    DomainDataProvider.setDirty();
                });

                dialogService.show(confirm);
            }
        });

        final Label groupMembersLabel = new Label("group-members-label",
                new StringResourceModel("group-members-label", this).setModel(groupModel));
        add(groupMembersLabel);

        groupMembersListView = new GroupMembersListView("groupmembers", context);
        add(groupMembersListView);
    }

    @Override
    public IModel<String> getTitle(Component component) {
        return new StringResourceModel("group-view-title", component).setModel(Model.of(group));
    }

    @Override
    public void onActivate(IBreadCrumbParticipant previous) {
        super.onActivate(previous);
        groupMembersListView.updateMembers();
    }

    private void deleteGroup(final Group groupToDelete) {
        final String groupName = groupToDelete.getGroupname();
        try {
            groupToDelete.delete();
            activateParentAndDisplayInfo(getString("group-removed", Model.of(groupToDelete)));
        } catch (RepositoryException e) {
            error(getString("group-remove-failed", Model.of(groupToDelete)));
            log.error("Unable to delete group '{}' : ", groupName, e);
            redraw();
        }
    }

    /**
     * Delete the link between the group and it's domain and the role.
     *
     * @param permissionBean the permission to remove
     */
    private void deleteRoleDomainCombination(final PermissionBean permissionBean) {
        final Domain domain = permissionBean.getDomain().getObject();
        final AuthRole authRole = permissionBean.getAuthRole();
        final Group groupToChange = permissionBean.getGroup().getObject();

        try {
            domain.removeGroupFromRole(authRole.getRole(), groupToChange.getGroupname());
            EventBusUtils.post("remove-group-from-role", HippoEventConstants.CATEGORY_GROUP_MANAGEMENT,
                    "removed group " + groupToChange.getGroupname() + " from role " + authRole.getRole());

            final String infoMsg = getString("group-role-domain-combination-removed", Model.of(groupToChange));
            activateParentAndDisplayInfo(infoMsg);
        } catch (RepositoryException e) {
            error(getString("group-delete-role-domain-combination-failed", Model.of(groupToChange)));
            log.error("Failed to remove role domain combination", e);
        }
    }

    /**
     * Delete a member from the list of group members.
     *
     * @param userName The userName of the user which is a member of the Group.
     */
    private void deleteGroupMembership(final String userName) {
        try {
            group.removeMembership(userName);
            EventBusUtils.post("remove-user-from-group", HippoEventConstants.CATEGORY_GROUP_MANAGEMENT,
                    "removed user " + userName + " from group " + group.getGroupname());

            info(getString("group-member-removed"));
        } catch (RepositoryException e) {
            error(getString("group-member-remove-failed"));
            log.error("Failed to remove memberships", e);
        }
        redraw();
    }

    /**
     * List view for showing the permissions of the group.
     */
    private final class PermissionsListView extends ListView<PermissionBean> {

        /**
         * The listview for the permissions linked to the group.
         *
         * @param id        The id of the listview.
         */
        PermissionsListView(final String id) {
            super(id, group.getPermissions());
            setReuseItems(false);
        }

        @Override
        protected void populateItem(final ListItem<PermissionBean> item) {
            final PermissionBean permissionBean = item.getModelObject();
            final Domain domain = permissionBean.getDomain().getObject();
            final AuthRole authRole = permissionBean.getAuthRole();
            final String roleName = authRole.getRole();

            final ViewDomainActionLink action = new ViewDomainActionLink(
                    "securityDomain",
                    ViewGroupPanel.this,
                    permissionBean.getDomain(),
                    Model.of(domain.getName())
            );
            item.add(action);
            item.add(new Label("role", roleName));
            item.add(new AjaxLinkLabel("remove", new ResourceModel("group-delete-role-domain-combination")) {
                @Override
                public void onClick(final AjaxRequestTarget target) {
                    final Confirm confirm = new Confirm(
                            getString("group-delete-role-domain-title", item.getModel()),
                            getString("group-delete-role-domain-text", item.getModel())
                    ).ok(() -> {
                        deleteRoleDomainCombination(permissionBean);
                        setModelObject(group.getPermissions());
                    });

                    dialogService.show(confirm);
                    target.add(ViewGroupPanel.this);
                }
            });
        }
    }

    /**
     * List view for the group members.
     */
    private final class GroupMembersListView extends ListView<DetachableUser> {

        private final IPluginContext context;

        GroupMembersListView(final String id, final IPluginContext context) {
            super(id, group.getMembersAsDetachableUsers());
            this.context = context;
            setReuseItems(false);
        }

        @Override
        protected void populateItem(final ListItem<DetachableUser> item) {
            final DetachableUser detachableUser = item.getModelObject();
            final User user = detachableUser.getUser();
            item.add(new ViewUserLinkLabel("username", detachableUser, ViewGroupPanel.this, context));
            item.add(new DeleteGroupMembershipActionLinkLabel("remove",
                    new ResourceModel("group-member-remove-action"), user));
        }

        void updateMembers() {
            setModelObject(group.getMembersAsDetachableUsers());
        }

        private final class DeleteGroupMembershipActionLinkLabel extends AjaxLinkLabel {

            private final User user;

            private DeleteGroupMembershipActionLinkLabel(final String id, final IModel<String> model, final User user) {
                super(id, model);
                this.user = user;
            }

            @Override
            public void onClick(final AjaxRequestTarget target) {
                final Model<User> userModel = Model.of(user);
                final Confirm confirm = new Confirm(
                        getString("group-delete-member-title", userModel),
                        getString("group-delete-member-text", userModel)
                ).ok(() -> {
                    deleteGroupMembership(user.getUsername());
                    updateMembers();
                });
                dialogService.show(confirm);
                target.add(ViewGroupPanel.this);
            }
        }
    }
}
