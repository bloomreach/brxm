package org.hippoecm.hst.plugins.frontend.editor.sitemap;

import org.apache.wicket.Component;
import org.apache.wicket.extensions.wizard.Wizard;

public class AjaxWizard extends Wizard {
    private static final long serialVersionUID = 1L;

    public AjaxWizard(String id, boolean addDefaultCssStyle) {
        super(id, addDefaultCssStyle);
    }

    @Override
    protected Component newButtonBar(String id) {
        return new AjaxWizardButtonBar(id, this);
    }

}
