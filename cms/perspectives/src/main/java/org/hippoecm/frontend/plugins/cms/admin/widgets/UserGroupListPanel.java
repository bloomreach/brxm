/*
 * Copyright 2012-2017 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.frontend.plugins.cms.admin.widgets;

import java.util.List;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.PropertyModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugins.cms.admin.groups.Group;
import org.hippoecm.frontend.plugins.cms.admin.groups.ViewGroupActionLink;
import org.hippoecm.frontend.plugins.cms.admin.permissions.PermissionsPanel;
import org.hippoecm.frontend.plugins.cms.admin.users.DetachableUser;
import org.hippoecm.frontend.plugins.cms.admin.users.User;
import org.hippoecm.frontend.plugins.cms.admin.users.ViewUserLinkLabel;

public class UserGroupListPanel extends Panel {

    public UserGroupListPanel(final String id, final List<User> users, final List<Group> groups,
                              final IPluginContext pluginContext) {
        super(id);

        final WebMarkupContainer usersLabel = new WebMarkupContainer("usersLabel");
        final boolean atLeastOneUserShown = !users.isEmpty();
        usersLabel.setVisible(atLeastOneUserShown);
        usersLabel.setRenderBodyOnly(true);
        add(usersLabel);

        final ListView<User> userListView = new ListView<User>("users", users) {
            @Override
            protected void populateItem(final ListItem<User> userListItem) {
                final User user = userListItem.getModelObject();
                final ViewUserLinkLabel action =
                        new ViewUserLinkLabel("link", new DetachableUser(user), findParent(PermissionsPanel.class), pluginContext);
                userListItem.add(action);
                userListItem.setRenderBodyOnly(true);
            }
        };
        add(userListView);

        final WebMarkupContainer groupsLabel = new WebMarkupContainer("groupsLabel");
        final boolean atLeastOneGroupShown = !groups.isEmpty();
        groupsLabel.setVisible(atLeastOneGroupShown);
        groupsLabel.setRenderBodyOnly(true);
        add(groupsLabel);

        final ListView<Group> groupListView = new ListView<Group>("groupItem", groups) {
            @Override
            protected void populateItem(final ListItem<Group> groupListItem) {
                final Group group = groupListItem.getModelObject();
                final ViewGroupActionLink action =
                        new ViewGroupActionLink("link", new PropertyModel<>(group, "groupname"), group, pluginContext,
                                findParent(PermissionsPanel.class));
                groupListItem.add(action);
                groupListItem.setRenderBodyOnly(true);
            }
        };
        add(groupListView);
    }
}
