package org.onehippo.cms7.essentials.dashboard.installer;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id$"
 */
public class DefaultAfterInstallWindow extends Panel {

    private static Logger log = LoggerFactory.getLogger(DefaultAfterInstallWindow.class);

    public DefaultAfterInstallWindow(final String id) {
        super(id);
    }

    public DefaultAfterInstallWindow(final String id, final IModel<?> model) {
        super(id, model);
    }
}
