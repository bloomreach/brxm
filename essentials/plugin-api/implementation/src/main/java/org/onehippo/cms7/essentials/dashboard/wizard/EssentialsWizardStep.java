package org.onehippo.cms7.essentials.dashboard.wizard;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.wizard.WizardStep;

/**
 * @version "$Id$"
 */
public class EssentialsWizardStep extends WizardStep {

    private static final long serialVersionUID = 1L;
    private boolean processed;

    public EssentialsWizardStep(final String title) {
        super(title, null);
    }

    /**
     * Called before component is rendered
     */
    public void refresh(final AjaxRequestTarget target) {
        // do nothing
    }

    public boolean isProcessed() {
        return processed;
    }

    public void setProcessed(final boolean processed) {
        this.processed = processed;
    }

}
