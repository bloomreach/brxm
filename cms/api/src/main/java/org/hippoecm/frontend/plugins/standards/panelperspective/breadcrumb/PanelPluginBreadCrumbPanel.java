/*
 *  Copyright 2008-2017 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.standards.panelperspective.breadcrumb;

import java.util.List;

import org.apache.wicket.event.IEvent;
import org.apache.wicket.extensions.breadcrumb.IBreadCrumbModel;
import org.apache.wicket.extensions.breadcrumb.IBreadCrumbParticipant;
import org.apache.wicket.extensions.breadcrumb.panel.BreadCrumbPanel;
import org.apache.wicket.feedback.ContainerFeedbackMessageFilter;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.PluginRequestTarget;
import org.hippoecm.frontend.widgets.UpdateFeedbackInfo;

public abstract class PanelPluginBreadCrumbPanel extends BreadCrumbPanel implements IPanelPluginParticipant {

    private final FeedbackPanel feedback;

    public PanelPluginBreadCrumbPanel(final String id, final IBreadCrumbModel breadCrumbModel) {
        super(id, breadCrumbModel);
        // add feedback panel to show errors
        feedback = new FeedbackPanel("feedback", new ContainerFeedbackMessageFilter(this) {
            @Override
            public boolean accept(final FeedbackMessage message) {
                return !message.isRendered() && super.accept(message);
            }
        });
        feedback.setOutputMarkupId(true);
        add(feedback);
    }

    @Override
    public IModel<String> getTitle() {
        return getTitle(this);
    }

    /**
     * get the feedback panel, might be null
     * @return the feedback panel or null if not set
     */
    public FeedbackPanel getFeedbackPanel() {
        return feedback;
    }

    @Override
    public void render(PluginRequestTarget target) {
    }

    @Override
    public void onEvent(IEvent event) {
        // handle notified validation events from wicket fields
        if(event.getPayload() instanceof UpdateFeedbackInfo) {
            final UpdateFeedbackInfo ufi = (UpdateFeedbackInfo) event.getPayload();
            final FeedbackPanel feedbackPanel = getFeedbackPanel();
            if (feedbackPanel.anyMessage()) {
                // refresh feedbackpanel
                ufi.getTarget().add(feedbackPanel);
            }
        }
    }

    protected IBreadCrumbParticipant activateParent() {
        final IBreadCrumbModel breadCrumbModel = getBreadCrumbModel();
        final List<IBreadCrumbParticipant> allBreadCrumbs = breadCrumbModel.allBreadCrumbParticipants();
        final IBreadCrumbParticipant parentBreadCrumb = allBreadCrumbs.get(allBreadCrumbs.size() - 2);
        breadCrumbModel.setActive(parentBreadCrumb);
        return parentBreadCrumb;
    }

    protected void activateParentAndDisplayInfo(final String infoMsg) {
        final IBreadCrumbParticipant parentBreadcrumb = activateParent();
        parentBreadcrumb.getComponent().info(infoMsg);
    }

}
