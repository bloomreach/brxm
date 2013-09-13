/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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

import java.util.ArrayList;
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
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.dialog.IDialogService;
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
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(ViewGroupPanel.class);

    private final Group group;

    public ViewGroupPanel(final String id, final IPluginContext context, final IBreadCrumbModel breadCrumbModel,
                          final Group group) {
        super(id, breadCrumbModel);
        setOutputMarkupId(true);

        this.group = group;

        add(new Label("view-group-panel-title",
                new StringResourceModel("group-view-title", this, new Model<Group>(group))));

        // common group properties
        add(new Label("groupname", new PropertyModel(group, "groupname")));
        add(new Label("description", new PropertyModel(group, "description")));

        PermissionsListView permissionsListView =
                new PermissionsListView(group, "permissions",
                        new Model<ArrayList<PermissionBean>>(new ArrayList<PermissionBean>(group.getPermissions())),
                        context);
        add(permissionsListView);

        // actions
        PanelPluginBreadCrumbLink edit = new PanelPluginBreadCrumbLink("edit-group", breadCrumbModel) {
            protected IBreadCrumbParticipant getParticipant(final String componentId) {
                return new EditGroupPanel(componentId, breadCrumbModel, new Model<Group>(group));
            }
        };
        edit.setVisible(!group.isExternal());
        add(edit);

        PanelPluginBreadCrumbLink members = new PanelPluginBreadCrumbLink("set-group-members", breadCrumbModel) {
            @Override
            protected IBreadCrumbParticipant getParticipant(final String componentId) {
                return new SetMembersPanel(componentId, breadCrumbModel, new Model<Group>(group));
            }
        };
        members.setVisible(!group.isExternal());
        add(members);

        add(new AjaxLinkLabel("delete-group", new ResourceModel("group-delete")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                context.getService(IDialogService.class.getName(), IDialogService.class).show(
                        new DeleteDialog<Group>(group, this) {
                            private static final long serialVersionUID = 1L;

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
                new StringResourceModel("group-members-label", this, new Model<Group>(group)));
        add(groupMembersLabel);
        ArrayList<DetachableUser> membersOfGroup = new ArrayList<DetachableUser>(group.getMembersAsDetachableUsers());
        Model<ArrayList<DetachableUser>> listModel = new Model<ArrayList<DetachableUser>>(membersOfGroup);
        GroupMembersListView groupMembersListView = new GroupMembersListView(group, "groupmembers", listModel, context);
        add(groupMembersListView);
    }

    private void deleteGroup(final Group group, final IPluginContext context) {
        String groupname = group.getGroupname();
        try {
            group.delete();
            Session.get().info(getString("group-removed", new Model<Group>(group)));
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
            Session.get().warn(getString("group-remove-failed", new Model<Group>(group)));
            log.error("Unable to delete group '" + groupname + "' : ", e);
        }
    }

    public IModel<String> getTitle(Component component) {
        return new StringResourceModel("group-view-title", component, new Model<Group>(group));
    }

    /**
     * List view for showing the permissions of the group.
     */
    private final class PermissionsListView extends ListView<PermissionBean> {
        private static final long serialVersionUID = 1L;
        private Group group;

        private final IPluginContext context;

        /**
         * The listview for the permissions linked to the group.
         *
         * @param group     The group
         * @param id        The id of the listview.
         * @param listModel The list which must be rendered by the listview
         * @param context   The current context
         */
        public PermissionsListView(final Group group, final String id,
                                   final IModel<ArrayList<PermissionBean>> listModel,
                                   final IPluginContext context) {
            super(id, listModel);
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
                    new PropertyModel<String>(domain, "name")
            );
            item.add(action);
            item.add(new Label("role", new Model<String>(roleName)));
            item.add(new AjaxLinkLabel("remove", new ResourceModel("group-delete-role-domain-combination")) {
                private static final long serialVersionUID = 1L;

                @Override
                public void onClick(final AjaxRequestTarget target) {
                    context.getService(IDialogService.class.getName(), IDialogService.class).show(
                            new DeleteDialog<PermissionBean>(permissionBean, this) {
                                private static final long serialVersionUID = 1L;

                                @Override
                                protected void onOk() {
                                    deleteRoleDomainCombination(permissionBean);
                                    PermissionsListView listView = PermissionsListView.this;
                                    listView.setModelObject(new ArrayList<PermissionBean>(group.getPermissions()));
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
            Session.get().info(getString("group-role-domain-combination-removed", new Model<Group>(groupToChange)));
            List<IBreadCrumbParticipant> l = getBreadCrumbModel().allBreadCrumbParticipants();
            getBreadCrumbModel().setActive(l.get(l.size() - 1));
        } catch (RepositoryException e) {
            Session.get().error(getString("group-delete-role-domain-combination-failed", new Model<Group>(groupToChange)));
            log.error("Failed to remove role domain combination", e);
        }
    }

    /**
     * List view for the group members.
     */
    private final class GroupMembersListView extends ListView<DetachableUser> {
        private static final long serialVersionUID = 1L;
        private Group group;

        private final IPluginContext context;
        private IModel<ArrayList<DetachableUser>> listModel;

        public GroupMembersListView(final Group group, final String id,
                                    final IModel<ArrayList<DetachableUser>> listModel,
                                    final IPluginContext context) {
            super(id, listModel);
            this.group = group;
            this.context = context;
            this.listModel = listModel;
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

        private class DeleteGroupMembershipActionLinkLabel extends AjaxLinkLabel {

            private final User user;

            private DeleteGroupMembershipActionLinkLabel(final String id, final IModel model, final User user) {
                super(id, model);
                this.user = user;
            }

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                context.getService(IDialogService.class.getName(), IDialogService.class).show(
                        new DeleteDialog<User>(new Model<User>(user), this) {
                            private static final long serialVersionUID = 1L;

                            @Override
                            protected void onOk() {
                                final String userName = user.getUsername();
                                deleteGroupMemberShip(userName);
                                List<DetachableUser> updatedGroupMembers = group.getMembersAsDetachableUsers();
                                listModel.setObject(new ArrayList<DetachableUser>(updatedGroupMembers));
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
}
