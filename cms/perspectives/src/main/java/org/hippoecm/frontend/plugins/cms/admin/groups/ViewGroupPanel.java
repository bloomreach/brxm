/*
 *  Copyright 2008-2016 Hippo B.V. (http://www.onehippo.com)
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

import java.util.List;

import javax.jcr.RepositoryException;

import org.apache.wicket.Component;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.breadcrumb.IBreadCrumbModel;
import org.apache.wicket.extensions.breadcrumb.IBreadCrumbParticipant;
import org.apache.wicket.extensions.breadcrumb.panel.BreadCrumbPanel;
import org.apache.wicket.extensions.breadcrumb.panel.IBreadCrumbPanelFactory;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.model.ReadOnlyModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugins.cms.admin.AdminBreadCrumbPanel;
import org.hippoecm.frontend.plugins.cms.admin.domains.Domain;
import org.hippoecm.frontend.plugins.cms.admin.domains.DomainDataProvider;
import org.hippoecm.frontend.plugins.cms.admin.permissions.PermissionBean;
import org.hippoecm.frontend.plugins.cms.admin.permissions.ViewDomainActionLink;
import org.hippoecm.frontend.plugins.cms.admin.users.DetachableUser;
import org.hippoecm.frontend.plugins.cms.admin.users.User;
import org.hippoecm.frontend.plugins.cms.admin.users.ViewUserLinkLabel;
import org.hippoecm.frontend.plugins.cms.admin.widgets.AjaxLinkLabel;
import org.hippoecm.frontend.plugins.cms.admin.widgets.DeleteDialog;
import org.hippoecm.frontend.plugins.standards.panelperspective.breadcrumb.PanelPluginBreadCrumbLink;
import org.hippoecm.frontend.session.UserSession;
import org.onehippo.cms7.event.HippoEvent;
import org.onehippo.cms7.event.HippoEventConstants;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.eventbus.HippoEventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Panel showing information regarding the groups.
 */
public class ViewGroupPanel extends AdminBreadCrumbPanel {

    private static final Logger log = LoggerFactory.getLogger(ViewGroupPanel.class);

    private final Group group;
    private final GroupMembersListView groupMembersListView;

    public ViewGroupPanel(final String id, final IPluginContext context, final IBreadCrumbModel breadCrumbModel,
                          final Group group) {
        super(id, breadCrumbModel);
        setOutputMarkupId(true);

        this.group = group;

        final Model<Group> groupModel = Model.of(group);
        add(new Label("view-group-panel-title", new StringResourceModel("group-view-title", this, groupModel)));

        // common group properties
        add(new Label("groupname", group.getGroupname())); // groups cannot be renamed, so no model needed
        add(new Label("description", ReadOnlyModel.of(group::getDescription)));

        PermissionsListView permissionsListView = new PermissionsListView(group, "permissions", context);
        add(permissionsListView);

        // actions
        PanelPluginBreadCrumbLink edit = new PanelPluginBreadCrumbLink("edit-group", breadCrumbModel) {
            protected IBreadCrumbParticipant getParticipant(final String componentId) {
                return new EditGroupPanel(componentId, breadCrumbModel, groupModel);
            }
        };
        edit.setVisible(!group.isExternal());
        add(edit);

        PanelPluginBreadCrumbLink members = new PanelPluginBreadCrumbLink("set-group-members", breadCrumbModel) {
            @Override
            protected IBreadCrumbParticipant getParticipant(final String componentId) {
                return new SetMembersPanel(componentId, breadCrumbModel, groupModel);
            }
        };
        members.setVisible(!group.isExternal());
        add(members);

        add(new AjaxLinkLabel("delete-group", new ResourceModel("group-delete")) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                context.getService(IDialogService.class.getName(), IDialogService.class).show(
                        new DeleteDialog<Group>(group, this) {
                            @Override
                            protected void onOk() {
                                deleteGroup(group, context);
                                DomainDataProvider.setDirty();
                            }

                            @Override
                            protected String getTitleKey() {
                                return "group-delete-title";
                            }

                            @Override
                            protected String getTextKey() {
                                return "group-delete-text";
                            }
                        });
            }
        });

        Label groupMembersLabel = new Label("group-members-label",
                new StringResourceModel("group-members-label", this, groupModel));
        add(groupMembersLabel);

        groupMembersListView = new GroupMembersListView(group, "groupmembers", context);
        add(groupMembersListView);
    }

    private void deleteGroup(final Group group, final IPluginContext context) {
        String groupname = group.getGroupname();
        try {
            group.delete();
            Session.get().info(getString("group-removed", Model.of(group)));
            // one up
            List<IBreadCrumbParticipant> l = getBreadCrumbModel().allBreadCrumbParticipants();
            getBreadCrumbModel().setActive(l.get(l.size() - 2));

            activate(new IBreadCrumbPanelFactory() {
                public BreadCrumbPanel create(final String componentId,
                                              final IBreadCrumbModel breadCrumbModel) {
                    return new ListGroupsPanel(componentId, context, breadCrumbModel, new GroupDataProvider());
                }
            });
        } catch (RepositoryException e) {
            Session.get().warn(getString("group-remove-failed", Model.of(group)));
            log.error("Unable to delete group '" + groupname + "' : ", e);
        }
    }

    public IModel<String> getTitle(Component component) {
        return new StringResourceModel("group-view-title", component, Model.of(group));
    }

    /**
     * List view for showing the permissions of the group.
     */
    private final class PermissionsListView extends ListView<PermissionBean> {

        private final Group group;
        private final IPluginContext context;

        /**
         * The listview for the permissions linked to the group.
         *
         * @param group     The group
         * @param id        The id of the listview.
         * @param context   The current context
         */
        public PermissionsListView(final Group group, final String id, final IPluginContext context) {
            super(id, group.getPermissions());
            this.group = group;
            this.context = context;
            setReuseItems(false);
        }

        @Override
        protected void populateItem(final ListItem<PermissionBean> item) {
            final PermissionBean permissionBean = item.getModelObject();
            Domain domain = permissionBean.getDomain().getObject();
            Domain.AuthRole authRole = permissionBean.getAuthRole();
            String roleName = authRole.getRole();

            ViewDomainActionLink action = new ViewDomainActionLink(
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
                    context.getService(IDialogService.class.getName(), IDialogService.class).show(
                            new DeleteDialog<PermissionBean>(permissionBean, this) {
                                @Override
                                protected void onOk() {
                                    deleteRoleDomainCombination(permissionBean);
                                    PermissionsListView.this.setModelObject(group.getPermissions());
                                }

                                @Override
                                protected String getTitleKey() {
                                    return "group-delete-role-domain-title";
                                }

                                @Override
                                protected String getTextKey() {
                                    return "group-delete-role-domain-text";
                                }
                            });
                    target.add(ViewGroupPanel.this);
                }
            });
        }
    }

    /**
     * Delete the link between the group and itś domain and the role.
     *
     * @param permissionBean the permission to remove
     */
    private void deleteRoleDomainCombination(PermissionBean permissionBean) {
        Domain domain = permissionBean.getDomain().getObject();
        Domain.AuthRole authRole = permissionBean.getAuthRole();
        Group groupToChange = permissionBean.getGroup().getObject();

        try {
            domain.removeGroupFromRole(authRole.getRole(), groupToChange.getGroupname());
            HippoEventBus eventBus = HippoServiceRegistry.getService(HippoEventBus.class);
            if (eventBus != null) {
                final UserSession userSession = UserSession.get();
                HippoEvent event = new HippoEvent(userSession.getApplicationName())
                        .user(userSession.getJcrSession().getUserID())
                        .action("remove-group-from-role")
                        .category(HippoEventConstants.CATEGORY_GROUP_MANAGEMENT)
                        .message(
                                "removed group " + groupToChange.getGroupname()
                                        + " from role " + authRole.getRole());
                eventBus.post(event);
            }
            Session.get().info(getString("group-role-domain-combination-removed", Model.of(groupToChange)));
            List<IBreadCrumbParticipant> l = getBreadCrumbModel().allBreadCrumbParticipants();
            getBreadCrumbModel().setActive(l.get(l.size() - 1));
        } catch (RepositoryException e) {
            Session.get().error(getString("group-delete-role-domain-combination-failed", Model.of(groupToChange)));
            log.error("Failed to remove role domain combination", e);
        }
    }

    /**
     * List view for the group members.
     */
    private final class GroupMembersListView extends ListView<DetachableUser> {

        private final Group group;
        private final IPluginContext context;

        public GroupMembersListView(final Group group, final String id, final IPluginContext context) {
            super(id, group.getMembersAsDetachableUsers());
            this.group = group;
            this.context = context;
            setReuseItems(false);
        }

        protected void populateItem(final ListItem<DetachableUser> item) {
            final DetachableUser detachableUser = item.getModelObject();
            final User user = detachableUser.getUser();
            item.add(new ViewUserLinkLabel("username", detachableUser, ViewGroupPanel.this, context));
            item.add(new DeleteGroupMembershipActionLinkLabel(
                    "remove", new ResourceModel("group-member-remove-action"), user
            ));
        }

        void updateMembers() {
            setModelObject(group.getMembersAsDetachableUsers());
        }

        private class DeleteGroupMembershipActionLinkLabel extends AjaxLinkLabel {

            private final User user;

            private DeleteGroupMembershipActionLinkLabel(final String id, final IModel<String> model, final User user) {
                super(id, model);
                this.user = user;
            }

            @Override
            public void onClick(final AjaxRequestTarget target) {
                context.getService(IDialogService.class.getName(), IDialogService.class).show(
                        new DeleteDialog<User>(Model.of(user), this) {
                            @Override
                            protected void onOk() {
                                final String userName = user.getUsername();
                                deleteGroupMemberShip(userName);
                                updateMembers();
                            }

                            @Override
                            protected String getTitleKey() {
                                return "group-delete-member-title";
                            }

                            @Override
                            protected String getTextKey() {
                                return "group-delete-member-text";
                            }
                        });
                target.add(ViewGroupPanel.this);
            }
        }
    }

    /**
     * Delete a member from the list of group members.
     *
     * @param userName The userName of the user which is a member of the Group.
     */
    private void deleteGroupMemberShip(final String userName) {
        try {
            group.removeMembership(userName);
            HippoEventBus eventBus = HippoServiceRegistry.getService(HippoEventBus.class);
            if (eventBus != null) {
                final UserSession userSession = UserSession.get();
                HippoEvent event = new HippoEvent(userSession.getApplicationName())
                        .user(userSession.getJcrSession().getUserID())
                        .action("remove-user-from-group")
                        .category(HippoEventConstants.CATEGORY_GROUP_MANAGEMENT)
                        .message("removed user " + userName + " from group " + group.getGroupname());
                eventBus.post(event);
            }
            Session.get().info(getString("group-member-removed", null));
            List<IBreadCrumbParticipant> l = getBreadCrumbModel().allBreadCrumbParticipants();
            getBreadCrumbModel().setActive(l.get(l.size() - 1));
        } catch (RepositoryException e) {
            Session.get().error(getString("group-member-remove-failed", null));
            log.error("Failed to remove memberships", e);
        }
    }

    @Override
    public void onActivate(IBreadCrumbParticipant previous) {
        super.onActivate(previous);
        groupMembersListView.updateMembers();
    }
}
