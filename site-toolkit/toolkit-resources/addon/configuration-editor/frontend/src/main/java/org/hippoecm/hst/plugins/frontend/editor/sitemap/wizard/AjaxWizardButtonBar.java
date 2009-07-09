package org.hippoecm.hst.plugins.frontend.editor.sitemap.wizard;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.wizard.IWizardModel;
import org.apache.wicket.extensions.wizard.IWizardStep;
import org.apache.wicket.extensions.wizard.Wizard;
import org.apache.wicket.extensions.wizard.WizardButtonBar;
import org.apache.wicket.markup.html.form.Form;

public class AjaxWizardButtonBar extends WizardButtonBar {

    public AjaxWizardButtonBar(String id, final Wizard wizard) {
        super(id, wizard);

        setOutputMarkupId(true);

        addOrReplace(new AjaxWizardButton("next", wizard, "next") {

            @Override
            public final boolean isEnabled() {
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

                target.addComponent(wizard);
            }

        });

        addOrReplace(new AjaxWizardButton("previous", wizard, "prev") {

            @Override
            public final boolean isEnabled() {
                return getWizardModel().isPreviousAvailable();
            }

            @Override
            protected void onClick(AjaxRequestTarget target, Form form) {
                getWizardModel().previous();
                target.addComponent(wizard);
            }
        });

        addOrReplace(new AjaxWizardButton("finish", wizard, "finish") {

            @Override
            public final boolean isEnabled() {
                IWizardStep activeStep = getWizardModel().getActiveStep();
                return (activeStep != null && getWizardModel().isLastStep(activeStep));
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
                // target.addComponent(wizard);
            }
        });

    }
}
