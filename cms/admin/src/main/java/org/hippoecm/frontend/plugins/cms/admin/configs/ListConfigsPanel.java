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
package org.hippoecm.frontend.plugins.cms.admin.configs;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.extensions.breadcrumb.IBreadCrumbModel;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugins.cms.admin.crumbs.AdminBreadCrumbPanel;
import org.hippoecm.frontend.plugins.cms.admin.widgets.AdminDataTable;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This panel displays a pageable list of users.
 */
public class ListConfigsPanel extends AdminBreadCrumbPanel {
    @SuppressWarnings("unused")
    private static final String SVN_ID = "$Id: ListUsersPanel.java 18459 2009-06-09 13:15:41Z bvdschans $";
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(ListConfigsPanel.class);

    private final ConfigDataProvider configDataProvider = new ConfigDataProvider();
    private final AdminDataTable table;


    public ListConfigsPanel(final String id, final IPluginContext context, final IBreadCrumbModel breadCrumbModel) {
        super(id, breadCrumbModel);
        setOutputMarkupId(true);

        //add(new AjaxBreadCrumbPanelLink("create-user", context, this, CreateUserPanel.class));
        
        List<IColumn> columns = new ArrayList<IColumn>();

        columns.add(new PropertyColumn(new ResourceModel("configuration-creationdate"), Config.PROP_BACKUP_CREATION_DATE, "creationDateAsString"));
        columns.add(new PropertyColumn(new ResourceModel("configuration-name"), Config.PROP_BACKUP_NAME, "name"));
        columns.add(new PropertyColumn(new ResourceModel("configuration-createdby"), Config.PROP_BACKUP_CREATED_BY, "createdBy"));

        table = new AdminDataTable("table", columns, configDataProvider, 20);
        table.setOutputMarkupId(true);
        add(table);
    }

    public IModel getTitle(Component component) {
        return new StringResourceModel("admin-configurations-title", component, null);
    }
}
