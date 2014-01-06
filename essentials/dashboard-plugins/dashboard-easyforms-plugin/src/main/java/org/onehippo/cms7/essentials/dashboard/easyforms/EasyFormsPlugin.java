package org.onehippo.cms7.essentials.dashboard.easyforms;


import org.onehippo.cms7.essentials.dashboard.installer.InstallablePlugin;
import org.onehippo.cms7.essentials.dashboard.Plugin;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.easyforms.installer.EasyFormsInstaller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Session;

public class EasyFormsPlugin extends InstallablePlugin<EasyFormsInstaller> {

    private static final Logger log = LoggerFactory.getLogger(EasyFormsPlugin.class);
    private static final long serialVersionUID = 1L;
    private transient Session session;

    public EasyFormsPlugin(String id, Plugin plugin, PluginContext context) {
        super(id, plugin, context);

        session = context.getSession();
/*
        final AjaxButton addButton = new AjaxButton("add") {

            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
                super.onSubmit(target, form);
                onFormSubmit(this, target);
                target.add(feedbackPanel);
                //target.add(getContext().getFeedBackPanel());    global message
            }
        };
*/
    }


    @Override
    public EasyFormsInstaller getInstaller() {
        return new EasyFormsInstaller(getContext(), "http://forge.onehippo.org/ef/1.2");
    }
}
