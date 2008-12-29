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
package org.hippoecm.frontend.plugins.cms.admin;

import org.apache.wicket.Component;
import org.apache.wicket.extensions.breadcrumb.IBreadCrumbModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugins.cms.admin.crumbs.AdminBreadCrumbPanel;
import org.hippoecm.frontend.plugins.cms.admin.groups.ListGroupsPanel;
import org.hippoecm.frontend.plugins.cms.admin.permissions.PermissionsPanel;
import org.hippoecm.frontend.plugins.cms.admin.system.SystemInfoPanel;
import org.hippoecm.frontend.plugins.cms.admin.system.SystemPropertiesPanel;
import org.hippoecm.frontend.plugins.cms.admin.users.ListUsersPanel;
import org.hippoecm.frontend.plugins.cms.admin.widgets.AjaxBreadCrumbPanelLink;

public class AdminPanel extends AdminBreadCrumbPanel {
    @SuppressWarnings("unused")
    private static final String SVN_ID = "$Id$";
    private static final long serialVersionUID = 1L;
    
    public AdminPanel(final String id, final IPluginContext context, final IBreadCrumbModel breadCrumbModel) {
        super(id, breadCrumbModel);

        add(new AjaxBreadCrumbPanelLink("users", context, this, ListUsersPanel.class));
        add(new AjaxBreadCrumbPanelLink("groups", context, this, ListGroupsPanel.class));
        add(new AjaxBreadCrumbPanelLink("permissions", context, this, PermissionsPanel.class));
        add(new AjaxBreadCrumbPanelLink("system-info", context, this, SystemInfoPanel.class));
        add(new AjaxBreadCrumbPanelLink("system-properties", context, this, SystemPropertiesPanel.class));
    }

    public IModel getTitle(Component component) {
        return new StringResourceModel("admin-title", component, null);
    }
}
