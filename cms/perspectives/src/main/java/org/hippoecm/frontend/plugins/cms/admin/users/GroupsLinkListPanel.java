/*
 *  Copyright 2012 Hippo.
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
package org.hippoecm.frontend.plugins.cms.admin.users;

import java.util.List;

import org.apache.wicket.extensions.breadcrumb.panel.BreadCrumbPanel;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.PropertyModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugins.cms.admin.groups.Group;
import org.hippoecm.frontend.plugins.cms.admin.groups.ViewGroupActionLink;

/**
 * A Panel that creates links to a list of groups
 */
public class GroupsLinkListPanel extends Panel {

    public GroupsLinkListPanel(final String id, final List<Group> groups,
                               final IPluginContext pluginContext,
                               final BreadCrumbPanel breadCrumbPanel) {
        super(id);

        ListView<Group> groupListView = new ListView<Group>("item", groups) {
            @Override
            protected void populateItem(final ListItem<Group> groupListItem) {
                Group group = groupListItem.getModelObject();
                ViewGroupActionLink action = new ViewGroupActionLink(
                        "link",
                        new PropertyModel<String>(group, "groupname"),
                        group,
                        pluginContext,
                        breadCrumbPanel
                );
                groupListItem.setRenderBodyOnly(true);
                groupListItem.add(action);
            }
        };

        add(groupListView);
    }
}
