package org.onehippo.cms7.essentials.components.gui;

import java.util.ArrayList;
import java.util.List;

import org.onehippo.cms7.essentials.components.gui.panel.ComponentsPanel;
import org.onehippo.cms7.essentials.components.gui.panel.DocumentRegisterPanel;
import org.onehippo.cms7.essentials.dashboard.InstallablePlugin;
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

    private final List<String> registeredDocuments = new ArrayList<>();
    public ComponentsWizard(final String id, final Plugin descriptor, final PluginContext context) {
        super(id, descriptor, context);
        final AjaxWizardPanel panel = new AjaxWizardPanel("wizard");
        panel.addWizard(new DocumentRegisterPanel(this, "Register document types"));
        panel.addWizard(new DocumentRegisterPanel(this, "Register document types"));
        panel.addWizard(new ComponentsPanel(this, "Component installer"));
        //
        // panel.addWizard(new DocumentRegisterPanel("installer2"));
        //
        add(panel);

    }


    @Override
    public ComponentsInstaller getInstaller() {
        return new ComponentsInstaller();
    }

    public List<String> getRegisteredDocuments() {
        return registeredDocuments;
    }

    public void addRegisteredDocument(final String documentName) {
        registeredDocuments.add(documentName);
    }

    public void clearRegistered() {
        registeredDocuments.clear();
    }
}
