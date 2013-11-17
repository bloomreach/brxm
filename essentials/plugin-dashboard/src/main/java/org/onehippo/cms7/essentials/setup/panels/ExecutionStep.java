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

import java.util.Collections;
import java.util.Set;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.event.DisplayEvent;
import org.onehippo.cms7.essentials.dashboard.instructions.InstructionSet;
import org.onehippo.cms7.essentials.dashboard.instructions.InstructionStatus;
import org.onehippo.cms7.essentials.dashboard.instructions.Instructions;
import org.onehippo.cms7.essentials.dashboard.packaging.PowerpackPackage;
import org.onehippo.cms7.essentials.dashboard.wizard.EssentialsWizardStep;
import org.onehippo.cms7.essentials.powerpack.BasicPowerpack;
import org.onehippo.cms7.essentials.powerpack.BasicPowerpackWithSamples;
import org.onehippo.cms7.essentials.setup.SetupPage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;
import com.google.inject.Inject;

/**
 * @version "$Id$"
 */
public class ExecutionStep extends EssentialsWizardStep {

    private static final long serialVersionUID = 1L;
    private static Logger log = LoggerFactory.getLogger(ExecutionStep.class);
    private final SetupPage myParent;
    private final EventsPanel eventsPanel;
    @Inject
    private EventBus eventBus;

    public ExecutionStep(final SetupPage components, final String title) {
        super(title);
        myParent = components;

        eventsPanel = new EventsPanel("events");
        add(eventsPanel);
        setOutputMarkupId(true);
    }

    @Override
    public void applyState(final AjaxRequestTarget target) {

        final SelectPowerpackStep selectStep = myParent.getSelectStep();
        log.debug("Selected Power Pack is {}", selectStep.getSelectedPowerpack());
        final PowerpackPackage powerpackPackage;
        switch (selectStep.getSelectedPowerpack()) {
            case SelectPowerpackStep.POWERPACK_NEWS_AND_EVENT_LABEL:
                if (selectStep.isInstallSampleContentChecked()) {
                    powerpackPackage = new BasicPowerpackWithSamples();
                } else {
                    powerpackPackage = new BasicPowerpack();
                }
                break;
            default:
                powerpackPackage = new EmptyPowerPack();
                break;
        }
        final InstructionStatus status = powerpackPackage.execute(myParent.getDashboardPluginContext());
        switch (status) {
            case SUCCESS:
                eventBus.post(new DisplayEvent("Installation finished successfully (" + status + ')'));
                break;
            case FAILED:
                eventBus.post(new DisplayEvent("Installation (" + status + ')'));

        }

        myParent.getFinalStep().displayEvents(target);

    }

    public EventsPanel getEventsPanel() {
        return eventsPanel;
    }

    public void displayEvents(final AjaxRequestTarget target) {
        eventsPanel.repaint(target);
        target.add(this);

    }

    private static class EmptyPowerPack implements PowerpackPackage {
        @Override
        public Instructions getInstructions() {
            return new Instructions() {
                @Override
                public Set<InstructionSet> getInstructionSets() {
                    return Collections.emptySet();
                }

                @Override
                public void setInstructionSets(Set<InstructionSet> instructionSets) {
                }
            };
        }

        @Override
        public InstructionStatus execute(PluginContext context) {
            return InstructionStatus.SUCCESS;
        }
    }

}
