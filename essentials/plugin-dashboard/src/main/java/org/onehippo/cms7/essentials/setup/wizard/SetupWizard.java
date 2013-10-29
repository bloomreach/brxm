package org.onehippo.cms7.essentials.setup.wizard;

import org.hippoecm.frontend.plugins.standards.wizard.AjaxWizard;
import org.onehippo.cms7.essentials.dashboard.wizard.AjaxWizardPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id$"
 */
public class SetupWizard extends AjaxWizardPanel {

    private static Logger log = LoggerFactory.getLogger(SetupWizard.class);


    public SetupWizard(final String id) {
        super(id);
    }
}
