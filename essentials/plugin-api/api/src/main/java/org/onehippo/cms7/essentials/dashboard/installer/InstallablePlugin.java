package org.onehippo.cms7.essentials.dashboard.installer;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.TransparentWebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.StringResourceModel;
import org.onehippo.cms7.essentials.dashboard.DashboardPlugin;
import org.onehippo.cms7.essentials.dashboard.Plugin;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



/**
 * Extend this class if your plugin needs to be installed. This plugin offers a nice default interface for essentials which
 * need to be installed. It also provides creating your own plugin interface etc. etc.
 *
 * @version "$Id: InstallablePlugin.java 174582 2013-08-21 16:56:23Z mmilicevic $"
 */
public abstract class InstallablePlugin<T extends Installer> extends DashboardPlugin {

    private static final long serialVersionUID = 1L;
    public static final String SESSION_SUFIX = "installState";
    private static Logger log = LoggerFactory.getLogger(InstallablePlugin.class);
    private String titleId;

    public InstallablePlugin(final String id, final Plugin plugin, final PluginContext context) {
        super(id, plugin, context);
        this.titleId = plugin.getName();
        setOutputMarkupId(true);
        // setOutputMarkupPlaceholderTag(true);

        TransparentWebMarkupContainer main = new TransparentWebMarkupContainer("main");
        add(main);

        final InstallState pluginInstalled = getInstallState();
        if (pluginInstalled != InstallState.INSTALLED_AND_RESTARTED) {
            main.add(new AttributeModifier("style", "display:none;"));
        }
        add(createInstaller(pluginInstalled));
    }

    public InstallState getInstallState() {
        return getInstaller().getInstallState();
    }

    /**
     * overwrite if you need to check for namespace uri CNDUtils etc...
     *
     * @return
     */
    public boolean isInstalled() {
        return getInstaller().getInstallState().equals(InstallState.INSTALLED);
    }

    // public abstract void installed(final String id, final String title, final PluginContext context);

    public abstract T getInstaller();

    /**
     * Create an installer ui.
     *
     * @return
     * @param pluginInstalled
     */
    public Fragment createInstaller(final InstallState pluginInstalled) {
        final Fragment fragment = new Fragment("install-fragment", "default-installer", InstallablePlugin.this);
        final InstallState currentInstallState = getInstallState();
        final Label label = new Label("install-message", new StringResourceModel(currentInstallState.getMessage(), this, null));

        final Form<?> form = new Form<>("form");
        form.setOutputMarkupId(true);

        AjaxButton button = new AjaxButton("install") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
                super.onSubmit(target, form);
                setEnabled(false);
                target.add(this);
                onInstall(this, target);
            }
        };

        //label.setVisible(!currentInstallState.equals(InstallState.INSTALLED_AND_RESTARTED));
        button.setVisible(currentInstallState.equals(InstallState.UNINSTALLED));
        form.add(button);
        fragment.add(form);
        fragment.add(label);
        // hide "installed"
        fragment.setVisible(pluginInstalled != InstallState.INSTALLED_AND_RESTARTED);
        return fragment;
    }

    /**
     * When the install buttons get's clicked. This is what happens:
     * TODO mm: what happens?
     *
     * @param button
     * @param target
     */
    public void onInstall(final AjaxButton button, final AjaxRequestTarget target) {
        getInstaller().install();
    }


}
