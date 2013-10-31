package org.onehippo.cms7.essentials.components.gui;

import org.onehippo.cms7.essentials.components.gui.panel.AttachComponentPanel;
import org.onehippo.cms7.essentials.components.gui.panel.ComponentsPanel;
import org.onehippo.cms7.essentials.components.gui.panel.provider.ComponentProvider;
import org.onehippo.cms7.essentials.dashboard.installer.InstallablePlugin;
import org.onehippo.cms7.essentials.dashboard.Plugin;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.wizard.AjaxWizardPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id$"
 */
public class ComponentsWizard extends InstallablePlugin<ComponentsInstaller> {

    private static final long serialVersionUID = 1L;
    private static Logger log = LoggerFactory.getLogger(ComponentsWizard.class);


    public ComponentsWizard(final String id, final Plugin descriptor, final PluginContext context) {
        super(id, descriptor, context);
        final AjaxWizardPanel panel = new AjaxWizardPanel("wizard") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onFinish() {
                info("Finished installing components. You might need to re-deploy you site.war due to new templates on disk.");
            }
        };
        // TODO move attach to last
        panel.addWizard(new ComponentsPanel(this, "Component installer"));
        panel.addWizard(new AttachComponentPanel(this, "Attach components"));
        //
        // panel.addWizard(new DocumentRegisterPanel("installer2"));
        //
        add(panel);

    }

    public ComponentProvider getProvider() {
        return new ComponentProvider();
    }

    @Override
    public ComponentsInstaller getInstaller() {
        return new ComponentsInstaller();
    }

}
