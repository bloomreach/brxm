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

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxFallbackLink;
import org.apache.wicket.markup.html.panel.Fragment;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.cms.admin.groups.GroupsPanel;
import org.hippoecm.frontend.plugins.cms.admin.permissions.PermissionsPanel;
import org.hippoecm.frontend.plugins.cms.admin.system.SystemInfoPanel;
import org.hippoecm.frontend.plugins.cms.admin.system.SystemPropertiesPanel;
import org.hippoecm.frontend.plugins.cms.admin.users.UsersPanel;
import org.hippoecm.frontend.plugins.standards.perspective.Perspective;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AdminPerspective extends Perspective {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(AdminPerspective.class);

    private Fragment adminFragment;
    private Fragment usersFragment;
    private Fragment groupsFragment;
    private Fragment permissionsFragment;
    private Fragment propertiesFragment;
    private Fragment systemInfoFragment;
    
    
    public AdminPerspective(IPluginContext context, IPluginConfig config) {
        super(context, config);
        setOutputMarkupId(true);
        
        // the admin menu
        adminFragment = new Fragment("content", "admin-panel", this);


        // add admin components
        usersFragment = new Fragment("content", "users-fragment", this);
        usersFragment.add(new UsersPanel("user-panel", this));

        groupsFragment = new Fragment("content", "groups-fragment", this);
        groupsFragment.add(new GroupsPanel("groups-close", this));

        permissionsFragment = new Fragment("content", "permissions-fragment", this);
        permissionsFragment.add(new PermissionsPanel("permissions-panel", this));

        propertiesFragment = new Fragment("content", "properties-fragment", this);
        propertiesFragment.add(new SystemPropertiesPanel("properties-panel", this));

        systemInfoFragment = new Fragment("content", "system-info-fragment", this);
        systemInfoFragment.add(new SystemInfoPanel("system-info-panel", this));

        // add menu links
        addFragmentLink("users", usersFragment);
        addFragmentLink("groups", groupsFragment);
        addFragmentLink("permissions", permissionsFragment);
        addFragmentLink("properties", propertiesFragment);
        addFragmentLink("system-info", systemInfoFragment);
        
        add(adminFragment);
    }
    
    public void showConfigPanel() {
        AdminPerspective.this.replace(adminFragment);
        AdminPerspective.this.redraw();
    }
    
    public void refresh() {
        AdminPerspective.this.redraw();
    }

    private void addFragmentLink(final String id, final Fragment fragment) {
        adminFragment.add(new AjaxFallbackLink(id) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                AdminPerspective.this.replace(fragment);
                AdminPerspective.this.redraw();
            }
        });
        
    }
    
    public void showDialog(IDialogService.Dialog dialog) {
        getPluginContext().getService(IDialogService.class.getName(), IDialogService.class).show(dialog);
    }
}
