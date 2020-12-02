/*
 *  Copyright 2011-2020 Hippo B.V. (http://www.onehippo.com)
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

import org.apache.wicket.extensions.breadcrumb.IBreadCrumbModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.cms.admin.AdminPanelPlugin;
import org.hippoecm.frontend.plugins.standards.panelperspective.breadcrumb.PanelPluginBreadCrumbPanel;

public class ListUsersPlugin extends AdminPanelPlugin {

    /**
     * Configuration key for Boolean property to determine whether user creation is enabled.
     */
    public static final String USER_CREATION_ENABLED_KEY = "user.creation.enabled";

    public static final String DEFAULT_USER_SECURITY_PROVIDER_KEY = "default.security.provider";
    public static final String SELECTABLE_USER_SECURITY_PROVIDERS_KEY = "selectable.security.providers";

    private final UserDataProvider userDataProvider;

    public ListUsersPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        userDataProvider = new UserDataProvider();
    }

    @Override
    public IModel<String> getTitle() {
        return new ResourceModel("admin-users-title");
    }

    @Override
    public IModel<String> getHelp() {
        return new ResourceModel("admin-users-title-help");
    }

    @Override
    public PanelPluginBreadCrumbPanel create(final String componentId, final IBreadCrumbModel breadCrumbModel) {
        return new ListUsersPanel(componentId, getPluginContext(), getPluginConfig(), breadCrumbModel, userDataProvider);
    }

}
