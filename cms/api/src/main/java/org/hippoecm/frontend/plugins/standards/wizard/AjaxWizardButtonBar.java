/*
 *  Copyright 2008-2022 Hippo B.V. (http://www.onehippo.com)
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

import org.apache.wicket.extensions.wizard.IWizard;
import org.apache.wicket.extensions.wizard.IWizardModel;
import org.apache.wicket.extensions.wizard.IWizardStep;
import org.apache.wicket.extensions.wizard.Wizard;
import org.apache.wicket.extensions.wizard.WizardButton;

import org.hippoecm.frontend.attributes.ClassAttribute;
import wicket.contrib.input.events.EventType;
import wicket.contrib.input.events.InputBehavior;
import wicket.contrib.input.events.key.KeyType;

public class AjaxWizardButtonBar extends org.apache.wicket.extensions.wizard.AjaxWizardButtonBar {

    private final Wizard wizard;

    public AjaxWizardButtonBar(String id, final Wizard wizard) {
        super(id, wizard);
        this.wizard = wizard;

        setOutputMarkupId(true);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
    }

    @Override
    protected WizardButton newPreviousButton(String id, IWizard wizard) {
        return new PreviousButton(id, wizard);
    }

    @Override
    protected WizardButton newNextButton(String id, IWizard wizard) {
        return new NextButton(id, wizard);
    }

    @Override
    protected WizardButton newFinishButton(String id, IWizard wizard) {
        return new FinishButton(id, wizard);
    }

    @Override
    protected WizardButton newCancelButton(String id, IWizard wizard) {
        return new CancelButton(id, wizard);
    }

    private static class CancelButton extends WizardButton {

        public CancelButton(final String id, final IWizard wizard) {
            super(id, wizard, () -> "cancel");
            add(ClassAttribute.append("btn btn-default"));
            add(new InputBehavior(new KeyType[] {KeyType.Escape}, EventType.click));

            // Skip form validation on cancel button
            setDefaultFormProcessing(false);
        }

        @Override
        public final boolean isEnabled() {
            return true;
        }

        @Override
        protected void onClick() {
            IWizardModel wizardModel = getWizardModel();
            wizardModel.cancel();
        }
    }

    private static class NextButton extends WizardButton {

        private final IWizard wizard;

        public NextButton(final String id, final IWizard wizard) {
            super(id, wizard, () -> "next");
            add(ClassAttribute.append("btn btn-default"));
            this.wizard = wizard;
        }

        @Override
        public final boolean isVisible() {
            return getWizardModel().isNextAvailable();
        }

        @Override
        protected void onClick() {
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
        }
    }

    private static class PreviousButton extends WizardButton {

        private final IWizard wizard;

        public PreviousButton(final String id, final IWizard wizard) {
            super(id, wizard, () -> "prev");
            add(ClassAttribute.append("btn btn-default"));
            this.wizard = wizard;

            // Skip form validation on previous button
            setDefaultFormProcessing(false);
        }

        @Override
        public final boolean isVisible() {
            return getWizardModel().isPreviousAvailable();
        }

        @Override
        protected void onClick() {
            getWizardModel().previous();
        }
    }

    private static class FinishButton extends WizardButton {

        public FinishButton(final String id, final IWizard wizard) {
            super(id, wizard, () -> "finish");
            add(ClassAttribute.append("btn btn-default"));
        }

        @Override
        public final boolean isVisible() {
            IWizardStep activeStep = getWizardModel().getActiveStep();
            return activeStep != null && getWizardModel().isLastStep(activeStep);
        }

        @Override
        protected void onClick() {
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
