package org.onehippo.cms7.essentials.components.gui;

import org.onehippo.cms7.essentials.components.gui.panel.DocumentRegisterPanel;
import org.onehippo.cms7.essentials.dashboard.DashboardPlugin;
import org.onehippo.cms7.essentials.dashboard.Plugin;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.wizard.AjaxWizardPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id$"
 */
public class ComponentsWizard extends DashboardPlugin {

    private static final long serialVersionUID = 1L;
    private static Logger log = LoggerFactory.getLogger(ComponentsWizard.class);

    public ComponentsWizard(final String id, final Plugin descriptor, final PluginContext context) {
        super(id, descriptor, context);
        final AjaxWizardPanel panel = new AjaxWizardPanel("wizard");
        panel.addWizard(new DocumentRegisterPanel("installer"));
        //
        // panel.addWizard(new DocumentRegisterPanel("installer2"));
        //
        add(panel);

    }
}
