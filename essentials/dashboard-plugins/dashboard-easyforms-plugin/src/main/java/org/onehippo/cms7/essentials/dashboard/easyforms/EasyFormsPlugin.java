package org.onehippo.cms7.essentials.dashboard.easyforms;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.onehippo.cms7.essentials.dashboard.installer.InstallablePlugin;
import org.onehippo.cms7.essentials.dashboard.Plugin;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.easyforms.installer.EasyFormsInstaller;
import org.onehippo.cms7.essentials.dashboard.ui.EssentialsFeedbackPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Session;

public class EasyFormsPlugin extends InstallablePlugin<EasyFormsInstaller> {

    private final static Logger log = LoggerFactory.getLogger(EasyFormsPlugin.class);
    private static final long serialVersionUID = 1L;
    private transient Session session;

    public EasyFormsPlugin(String id, Plugin plugin, PluginContext context) {
        super(id, plugin, context);
        final FeedbackPanel feedbackPanel = new EssentialsFeedbackPanel("feedback");
        feedbackPanel.setOutputMarkupId(true);
        add(feedbackPanel);

        session = context.getSession();

        final Form<?> form = new Form("form");
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
        form.add(addButton);
        add(form);
    }

    private void onFormSubmit(AjaxButton components, AjaxRequestTarget target) {

    }

    @Override
    public EasyFormsInstaller getInstaller() {
        return new EasyFormsInstaller(getContext(), "http://forge.onehippo.org/ef/1.2");
    }
}
