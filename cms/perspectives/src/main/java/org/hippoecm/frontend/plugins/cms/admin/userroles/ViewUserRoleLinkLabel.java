/*
 *  Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.cms.admin.userroles;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.breadcrumb.panel.BreadCrumbPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugins.cms.admin.users.User;
import org.hippoecm.frontend.plugins.cms.admin.widgets.AjaxLinkLabel;

import com.bloomreach.xm.repository.security.UserRole;

/**
 * Creates a link to the user detail page for a certain {@link User}
 */
public class ViewUserRoleLinkLabel extends AjaxLinkLabel {

    private final BreadCrumbPanel panelToReplace;
    private final IModel<UserRole> userRoleModel;
    private final IPluginContext pluginContext;

    public ViewUserRoleLinkLabel(final String id, final IModel<UserRole> userRoleModel, final BreadCrumbPanel panelToReplace,
                                 final IPluginContext pluginContext) {
        super(id, PropertyModel.of(userRoleModel, "name"));
        this.panelToReplace = panelToReplace;
        this.userRoleModel = userRoleModel;
        this.pluginContext = pluginContext;
    }

    @Override
    public void onClick(final AjaxRequestTarget target) {
        panelToReplace.activate((componentId, breadCrumbModel) ->
                new ViewUserRolePanel(componentId, pluginContext, breadCrumbModel, userRoleModel));
    }
}
