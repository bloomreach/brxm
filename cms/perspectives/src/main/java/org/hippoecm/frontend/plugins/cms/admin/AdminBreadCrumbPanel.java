/**
 * Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.frontend.plugins.cms.admin;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.breadcrumb.IBreadCrumbModel;
import org.apache.wicket.extensions.breadcrumb.IBreadCrumbModelListener;
import org.apache.wicket.extensions.breadcrumb.IBreadCrumbParticipant;
import org.apache.wicket.request.cycle.RequestCycle;
import org.hippoecm.frontend.plugins.standards.panelperspective.breadcrumb.PanelPluginBreadCrumbPanel;

/**
 * TODO the layout of the admin perspective should be refactored so the html in AdminBreadCrumbPanel.html is not needed anymore.
 */
public abstract class AdminBreadCrumbPanel extends PanelPluginBreadCrumbPanel {

    public AdminBreadCrumbPanel(final String id, final IBreadCrumbModel breadCrumbModel) {
        super(id, breadCrumbModel);

        setOutputMarkupId(true);

        breadCrumbModel.addListener(new IBreadCrumbModelListener() {
            @Override
            public void breadCrumbActivated(final IBreadCrumbParticipant previousParticipant,
                    final IBreadCrumbParticipant breadCrumbParticipant) {
            }

            @Override
            public void breadCrumbAdded(final IBreadCrumbParticipant breadCrumbParticipant) {
                if (breadCrumbParticipant == AdminBreadCrumbPanel.this) {
                    AdminBreadCrumbPanel.this.onAddedToBreadCrumbsBar();
                }
            }

            @Override
            public void breadCrumbRemoved(final IBreadCrumbParticipant breadCrumbParticipant) {
                if (breadCrumbParticipant == AdminBreadCrumbPanel.this) {
                    breadCrumbModel.removeListener(this);
                    AdminBreadCrumbPanel.this.onRemovedFromBreadCrumbsBar();
                }
            }
        });
    }

    protected void redraw() {
        AjaxRequestTarget target = RequestCycle.get().find(AjaxRequestTarget.class);
        if (target != null) {
            target.add(this);
        }
    }

    protected void onAddedToBreadCrumbsBar() {
    }

    /**
     * Panel has been removed from list of breadcrumbs
     */
    protected void onRemovedFromBreadCrumbsBar() {
    }
}
