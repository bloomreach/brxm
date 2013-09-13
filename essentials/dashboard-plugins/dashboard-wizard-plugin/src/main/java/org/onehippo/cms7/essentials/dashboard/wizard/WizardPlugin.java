package org.onehippo.cms7.essentials.dashboard.wizard;


import org.onehippo.cms7.essentials.dashboard.DashboardPlugin;
import org.onehippo.cms7.essentials.dashboard.Plugin;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id$"
 */
public class WizardPlugin extends DashboardPlugin {

    private static final long serialVersionUID = 1L;
    private final static Logger log = LoggerFactory.getLogger(WizardPlugin.class);

    public WizardPlugin(final String id, final Plugin descriptor, final PluginContext context) {
        super(id, descriptor, context);

        AjaxWizardPanel panel = new AjaxWizardPanel("wizard");
        panel.addWizard(new Step1("test1"));
        panel.addWizard(new Step2("test2"));
        panel.addWizard(new Step3("test3"));
        panel.initSteps();

        add(panel);
    }

    private final class Step1 extends EssentialsWizardStep {
        private Step1(final String title) {
            super(title);
        }
    }

    private final class Step2 extends EssentialsWizardStep {
        private Step2(final String title) {
            super(title);
        }
    }

    private final class Step3 extends EssentialsWizardStep {
        private Step3(final String title) {
            super(title);
        }
    }


}
