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
package org.hippoecm.frontend.plugins.cms.admin.system;

import org.apache.wicket.extensions.breadcrumb.IBreadCrumbModel;
import org.apache.wicket.extensions.breadcrumb.panel.BreadCrumbPanel;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugins.cms.admin.widgets.AdminDataTable;

public class SystemPropertiesPanel extends BreadCrumbPanel {

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";
    
    private static final long serialVersionUID = 1L;
    
    public SystemPropertiesPanel(final String id, final IPluginContext context, final IBreadCrumbModel breadCrumbModel) {
        super(id, breadCrumbModel);

        IColumn[] columns = new IColumn[2];
        columns[0] = new PropertyColumn(new Model("Key"), "key");
        columns[1] = new PropertyColumn(new Model("Value"), "value");
        
        add(new AdminDataTable("table", columns, new SystemPropertiesDataProvider(), 25));
    }

    public String getTitle() {
        return getString("admin-system-properties-title");
    }

    
}
