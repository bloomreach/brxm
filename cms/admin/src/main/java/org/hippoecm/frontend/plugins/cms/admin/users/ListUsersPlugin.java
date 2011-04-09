/*
 *  Copyright 2011 Hippo.
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

import org.apache.wicket.ResourceReference;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.cms.admin.AdminPlugin;

public class ListUsersPlugin extends AdminPlugin {

    public ListUsersPlugin(IPluginContext context, IPluginConfig config) {
        super(context, ListUsersPanel.class);
    }

    @Override
    public ResourceReference getImage() {
        return new ResourceReference(getClass(), "user-48.png");
    }

    @Override
    public IModel<String> getTitle() {
        return new ResourceModel("admin-users-title");
    }

    @Override
    public IModel<String> getHelp() {
        return new ResourceModel("admin-users-title-help");
    }
}
