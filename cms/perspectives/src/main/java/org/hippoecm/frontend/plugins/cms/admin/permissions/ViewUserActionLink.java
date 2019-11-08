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
package org.hippoecm.frontend.plugins.cms.admin.permissions;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.breadcrumb.panel.BreadCrumbPanel;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugins.cms.admin.users.DetachableUser;
import org.hippoecm.frontend.plugins.cms.admin.users.User;
import org.hippoecm.frontend.plugins.cms.admin.users.ViewUserPanel;

public class ViewUserActionLink extends AjaxLink<String> {

    private final User user;
    private final IPluginContext context;
    private final BreadCrumbPanel breadCrumbPanel;

    public ViewUserActionLink(final String id, final IModel<String> labelTextModel, final User user,
                              final IPluginContext context, final BreadCrumbPanel breadCrumbPanel) {
        super(id, labelTextModel);

        this.user = user;
        this.context = context;
        this.breadCrumbPanel = breadCrumbPanel;

        final Label label = new Label("label", labelTextModel);
        label.setRenderBodyOnly(true);
        add(label);
    }

    @Override
    public void onClick(final AjaxRequestTarget target) {
        breadCrumbPanel.activate((componentId, breadCrumbModel) ->
                new ViewUserPanel(componentId, context, breadCrumbModel, new DetachableUser(user)));
    }
}
