/*
 *  Copyright 2008-2018 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.frontend.plugins.standards.wizard;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.wizard.IWizardModel;
import org.apache.wicket.extensions.wizard.IWizardStep;
import org.apache.wicket.extensions.wizard.Wizard;
import org.apache.wicket.extensions.wizard.WizardButtonBar;
import org.apache.wicket.markup.html.form.Form;

import wicket.contrib.input.events.EventType;
import wicket.contrib.input.events.InputBehavior;
import wicket.contrib.input.events.key.KeyType;

public class AjaxWizardButtonBar extends WizardButtonBar {

    private final Wizard wizard;

    public AjaxWizardButtonBar(String id, final Wizard wizard) {
        super(id, wizard);
        this.wizard = wizard;

        setOutputMarkupId(true);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        addOrReplace(new CancelButton(wizard));
        addOrReplace(new NextButton(wizard));
        addOrReplace(new PreviousButton(wizard));
        addOrReplace(new FinishButton(wizard));
    }

    private class CancelButton extends AjaxWizardButton {

        public CancelButton(final Wizard wizard) {
            super("cancel", wizard, "cancel");

            add(new InputBehavior(new KeyType[] {KeyType.Escape}, EventType.click));

            // Skip form validation on cancel button
            setDefaultFormProcessing(false);
        }

        @Override
        public final boolean isEnabled() {
            return true;
        }

        @Override
        protected void onClick(AjaxRequestTarget target, Form form) {
            IWizardModel wizardModel = getWizardModel();
            wizardModel.cancel();
        }
    }

    private class NextButton extends AjaxWizardButton {

        private final Wizard wizard;

        public NextButton(final Wizard wizard) {
            super("next", wizard, "next");
            this.wizard = wizard;
        }

        @Override
        public final boolean isVisible() {
            return getWizardModel().isNextAvailable();
        }

        @Override
        protected void onClick(AjaxRequestTarget target, Form form) {
            IWizardModel wizardModel = getWizardModel();
            IWizardStep step = wizardModel.getActiveStep();

            // let the step apply any state
            step.applyState();

            // if the step completed after applying the state, move the model onward
            if (step.isComplete()) {
                wizardModel.next();
            } else {
                error(getLocalizer().getString(
                        "org.apache.wicket.extensions.wizard.NextButton.step.did.not.complete", this));
            }

            target.add(wizard);
        }
    }

    private static class PreviousButton extends AjaxWizardButton {

        private final Wizard wizard;

        public PreviousButton(final Wizard wizard) {
            super("previous", wizard, "prev");
            this.wizard = wizard;

            // Skip form validation on previous button
            setDefaultFormProcessing(false);
        }

        @Override
        public final boolean isVisible() {
            return getWizardModel().isPreviousAvailable();
        }

        @Override
        protected void onClick(AjaxRequestTarget target, Form form) {
            getWizardModel().previous();
            target.add(wizard);
        }
    }

    private class FinishButton extends AjaxWizardButton {

        public FinishButton(final Wizard wizard) {
            super("finish", wizard, "finish");
        }

        @Override
        public final boolean isVisible() {
            IWizardStep activeStep = getWizardModel().getActiveStep();
            return activeStep != null && getWizardModel().isLastStep(activeStep);
        }

        @Override
        protected void onClick(AjaxRequestTarget target, Form form) {
            IWizardModel wizardModel = getWizardModel();
            IWizardStep step = wizardModel.getActiveStep();

            // let the step apply any state
            step.applyState();

            // if the step completed after applying the state, notify the wizard
            if (step.isComplete()) {
                getWizardModel().finish();
            } else {
                error(getLocalizer().getString(
                        "org.apache.wicket.extensions.wizard.FinishButton.step.did.not.complete", this));
            }
        }
    }
}
