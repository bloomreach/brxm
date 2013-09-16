package org.onehippo.cms7.essentials.dashboard.wizard;

import org.apache.wicket.extensions.wizard.WizardStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id$"
 */
public class EssentialsWizardStep extends WizardStep {

    private static Logger log = LoggerFactory.getLogger(EssentialsWizardStep.class);

    private boolean processed;

    public EssentialsWizardStep(final String title) {
        super(title, null);
    }

    public boolean isProcessed() {
        return processed;
    }

    public void setProcessed(final boolean processed) {
        this.processed = processed;
    }

}
