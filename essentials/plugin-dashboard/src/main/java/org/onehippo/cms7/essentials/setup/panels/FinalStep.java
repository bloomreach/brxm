/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cms7.essentials.setup.panels;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.onehippo.cms7.essentials.dashboard.wizard.EssentialsWizardStep;
import org.onehippo.cms7.essentials.setup.SetupPage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id$"
 */
public class FinalStep extends EssentialsWizardStep {

    private static final long serialVersionUID = 1L;
    private static Logger log = LoggerFactory.getLogger(FinalStep.class);
    private final EventsPanel eventsPanel;

    public FinalStep(final SetupPage components, final String title) {
        super(title);
        eventsPanel = new EventsPanel("events");
        add(eventsPanel);
        setOutputMarkupId(true);
    }

    @Override
    public void applyState() {
    }

    public EventsPanel getEventsPanel() {
        return eventsPanel;
    }

    public void displayEvents(final AjaxRequestTarget target) {
        eventsPanel.repaint(target);
        target.add(this);

    }
}
