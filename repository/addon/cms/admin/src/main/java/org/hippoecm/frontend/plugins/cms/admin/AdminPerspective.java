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

import org.apache.wicket.extensions.breadcrumb.BreadCrumbBar;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.cms.admin.crumbs.AdminBreadCrumbBar;
import org.hippoecm.frontend.plugins.standards.perspective.Perspective;
import org.hippoecm.frontend.service.PluginRequestTarget;

public class AdminPerspective extends Perspective {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;
 
    public AdminPerspective(IPluginContext context, IPluginConfig config) {
        super(context, config);
        setOutputMarkupId(true);

        // add feedback panel to show errors
        FeedbackPanel feedback = new FeedbackPanel("feedback");
        feedback.setOutputMarkupId(true);
        add(feedback);

        BreadCrumbBar breadCrumbBar = new AdminBreadCrumbBar("breadCrumbBar");
        add(breadCrumbBar);

        AdminPanel adminPanel = new AdminPanel("panel", context, breadCrumbBar);
        add(adminPanel);

        breadCrumbBar.setActive(adminPanel);
    }

    // override to do unconditional redraw
    @Override
    public void render(PluginRequestTarget target) {
        target.addComponent(this);
        super.render(target);
    }
    
    public void showDialog(IDialogService.Dialog dialog) {
        getPluginContext().getService(IDialogService.class.getName(), IDialogService.class).show(dialog);
    }

}
