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
package org.hippoecm.frontend.plugins.cms.admin.groups;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxFallbackLink;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.plugins.cms.admin.AdminPerspective;
import org.hippoecm.frontend.plugins.cms.admin.widgets.AdminDataTable;

public class GroupsPanel extends Panel {

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private AdminDataTable table;

    public GroupsPanel(final String id, final AdminPerspective parent) {
        super(id);
        setOutputMarkupId(true);

        List<IColumn> columns = new ArrayList<IColumn>();

        //        columns.add(new AbstractColumn(new Model("Actions"))
        //        {
        //            public void populateItem(Item cellItem, String componentId,
        //                IModel model)
        //            {
        //                cellItem.add(new ActionPanel(componentId, model));
        //            }
        //        });

        columns.add(new PropertyColumn(new Model("Name"), "groupname", "groupname"));
        columns.add(new PropertyColumn(new Model("Description"), "description", "description"));
        columns.add(new AbstractColumn(new Model("Members")) {
            private static final long serialVersionUID = 1L;

            public void populateItem(Item cellItem, String componentId, IModel model) {
                Group group = (Group) model.getObject();
                StringBuilder sb = new StringBuilder();
                boolean first = true;
                for (String user : group.getMembers()) {
                    if (first) {
                        sb.append(user);
                        first = false;
                    } else {
                        sb.append(',').append(user);
                    }
                }
                cellItem.add(new Label(componentId, sb.toString()));
            }
        });

        table = new AdminDataTable("table", columns, new GroupDataProvider(), 40);
        add(table);
        add(new AjaxFallbackLink("close") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                parent.showConfigPanel();
            }
        });
    }

}