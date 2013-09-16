package org.onehippo.cms7.essentials.components.gui.panel;

import org.apache.wicket.markup.html.panel.Panel;
import org.onehippo.cms7.essentials.dashboard.wizard.AjaxWizardPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id$"
 */
public class DocumentRegisterPanel extends AjaxWizardPanel {

    private static final long serialVersionUID = 1L;
    private static Logger log = LoggerFactory.getLogger(DocumentRegisterPanel.class);

    public DocumentRegisterPanel(final String id) {
        super(id);
    }
}
