/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.standards.panelperspective;

import org.apache.wicket.extensions.breadcrumb.BreadCrumbBar;
import org.apache.wicket.extensions.breadcrumb.IBreadCrumbModelListener;
import org.apache.wicket.extensions.breadcrumb.IBreadCrumbParticipant;
import org.apache.wicket.markup.html.CSSPackageResource;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.panelperspective.breadcrumb.PanelPluginBreadCrumbBar;
import org.hippoecm.frontend.plugins.standards.perspective.Perspective;
import org.hippoecm.frontend.plugins.yui.layout.WireframeBehavior;
import org.hippoecm.frontend.plugins.yui.layout.WireframeSettings;

public abstract class PanelPluginPerspective extends Perspective {

    private static final long serialVersionUID = 1L;

    private final BreadCrumbBar breadCrumbBar;

    public PanelPluginPerspective(IPluginContext context, IPluginConfig config) {
        super(context, config);
        setOutputMarkupId(true);


        breadCrumbBar = new PanelPluginBreadCrumbBar("bread-crumb-bar");
        add(breadCrumbBar);

        final PanelPluginPanel panelPluginPanel = new PanelPluginPanel("panel", context, breadCrumbBar, getPanelServiceId());
        add(panelPluginPanel);

        breadCrumbBar.setActive(panelPluginPanel);

        breadCrumbBar.addListener(new IBreadCrumbModelListener() {
            private static final long serialVersionUID = 1L;

            public void breadCrumbActivated(IBreadCrumbParticipant previousParticipant,
                    IBreadCrumbParticipant breadCrumbParticipant) {
                redraw();
            }

            public void breadCrumbAdded(IBreadCrumbParticipant breadCrumbParticipant) {
                redraw();
            }

            public void breadCrumbRemoved(IBreadCrumbParticipant breadCrumbParticipant) {
                redraw();
            }
        });

        IPluginConfig wfConfig = config.getPluginConfig("layout.wireframe");
        if (wfConfig != null) {
            WireframeSettings wfSettings = new WireframeSettings(wfConfig);
            add(new WireframeBehavior(wfSettings));
        }

        add(CSSPackageResource.getHeaderContribution(PanelPluginPerspective.class, "panel-plugin-perspective.css"));
    }

    public void showDialog(IDialogService.Dialog dialog) {
        getPluginContext().getService(IDialogService.class.getName(), IDialogService.class).show(dialog);
    }

    public BreadCrumbBar getBreadCrumbBar() {
        return breadCrumbBar;
    }

    public abstract String getPanelServiceId();

}
