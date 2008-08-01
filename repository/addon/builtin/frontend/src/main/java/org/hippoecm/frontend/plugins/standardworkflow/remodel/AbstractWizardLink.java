package org.hippoecm.frontend.plugins.standardworkflow.remodel;

import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.wizard.IWizardModel;

public abstract class AbstractWizardLink extends AjaxLink {
    private static final long serialVersionUID = 1L;

    private final RemodelWizard wizard;

    public AbstractWizardLink(String id, RemodelWizard wizard) {
        super(id);
        this.wizard = wizard;
        setOutputMarkupId(true);
    }

    protected final RemodelWizard getWizard() {
        return wizard;
    }

    protected final IWizardModel getWizardModel() {
        return wizard.getWizardModel();
    }

}