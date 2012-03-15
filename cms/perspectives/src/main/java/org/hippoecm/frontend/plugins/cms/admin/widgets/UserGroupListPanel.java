package org.hippoecm.frontend.plugins.cms.admin.widgets;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.PropertyModel;
import org.hippoecm.frontend.plugins.cms.admin.groups.Group;
import org.hippoecm.frontend.plugins.cms.admin.groups.ViewGroupActionLink;
import org.hippoecm.frontend.plugins.cms.admin.permissions.PermissionsPanel;
import org.hippoecm.frontend.plugins.cms.admin.users.DetachableUser;
import org.hippoecm.frontend.plugins.cms.admin.users.User;
import org.hippoecm.frontend.plugins.cms.admin.users.ViewUserLinkLabel;

import java.util.List;

public class UserGroupListPanel extends Panel {

    public UserGroupListPanel(String id, List<User> users, List<Group> groups) {
        super(id);

        WebMarkupContainer usersLabel = new WebMarkupContainer("usersLabel");
        boolean atLeastOneUserShown = !users.isEmpty();
        usersLabel.setVisible(atLeastOneUserShown);
        usersLabel.setRenderBodyOnly(true);
        add(usersLabel);

        ListView<User> userListView = new ListView<User>("users", users) {
            @Override
            protected void populateItem(final ListItem<User> userListItem) {
                User user = userListItem.getModelObject();
                ViewUserLinkLabel action =
                        new ViewUserLinkLabel("link", new DetachableUser(user), findParent(PermissionsPanel.class), null);
                userListItem.add(action);
                userListItem.setRenderBodyOnly(true);
            }
        };
        add(userListView);

        WebMarkupContainer groupsLabel = new WebMarkupContainer("groupsLabel");
        boolean atLeastOneGroupShown = !groups.isEmpty();
        groupsLabel.setVisible(atLeastOneGroupShown);
        groupsLabel.setRenderBodyOnly(true);
        add(groupsLabel);

        ListView<Group> groupListView = new ListView<Group>("groupItem", groups) {
            @Override
            protected void populateItem(final ListItem<Group> groupListItem) {
                Group group = groupListItem.getModelObject();
                ViewGroupActionLink action =
                        new ViewGroupActionLink("link", new PropertyModel<String>(group, "groupname"), group, null,
                                findParent(PermissionsPanel.class));
                groupListItem.add(action);
                groupListItem.setRenderBodyOnly(true);
            }
        };
        add(groupListView);
    }
}