package org.onehippo.cms7.essentials.dashboard.installer;

import org.onehippo.cms7.essentials.dashboard.DashboardPlugin;
import org.onehippo.cms7.essentials.dashboard.Plugin;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;


/**
 * Extend this class if your plugin needs to be installed. This plugin offers a nice default interface for essentials
 * which need to be installed. It also provides creating your own plugin interface etc. etc.
 *
 * @version "$Id: InstallablePlugin.java 174582 2013-08-21 16:56:23Z mmilicevic $"
 */
public abstract class InstallablePlugin<T extends Installer> extends DashboardPlugin {

    private static final long serialVersionUID = 1L;
    public static final String SESSION_SUFIX = "installState";

    private InstallState pluginInstalled;

    private String titleId;

    public InstallablePlugin(final String id, final Plugin plugin, final PluginContext context) {
        super(id, plugin, context);
   /*
        this.titleId = plugin.getName();
        setOutputMarkupId(true);
        // setOutputMarkupPlaceholderTag(true);

        Label description = new Label("description", plugin.getDescription());
        description.setEscapeModelStrings(false);
        add(description);

        main = new TransparentWebMarkupContainer("main");
        add(main);
        main.setOutputMarkupId(true);

        pluginInstalled = getInstallState();
        if (pluginInstalled != InstallState.INSTALLED_AND_RESTARTED) {
            main.add(new AttributeModifier("style", "display:none;"));
        }
        installer = createInstaller(pluginInstalled, plugin);
        add(installer);*/
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
     * @param pluginInstalled
     * @param plugin
     * @return
     */
  /*  public Fragment createInstaller(final InstallState pluginInstalled, final Plugin plugin) {
        final Fragment fragment = new Fragment("install-fragment", "default-installer", InstallablePlugin.this);
        fragment.setOutputMarkupId(true);
        final InstallState currentInstallState = getInstallState();
        final Label label = new Label("install-message", new StringResourceModel(currentInstallState.getMessage(), this, null));

        final Form<?> form = new Form<>("form");
        form.setOutputMarkupId(true);

        final ModalWindow afterInstallWindow = getAfterInstallWindow(plugin);

        fragment.add(afterInstallWindow);

        AjaxButton button = new AjaxButton("install") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
                super.onSubmit(target, form);
                setEnabled(false);
                target.add(this);
                onInstall(this, target);
                afterInstallWindow.show(target);
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

    *//**
     * When the install buttons get's clicked. This is what happens: TODO mm: what happens?
     *
     * @param button
     * @param target
     *//*
    public void onInstall(final AjaxButton button, final AjaxRequestTarget target) {
        getInstaller().install();
        pluginInstalled = getInstaller().getInstallState();
    }

    public ModalWindow getAfterInstallWindow(final Plugin plugin) {
        ModalWindow window = new ModalWindow("modal");

        window.setContent(new DefaultAfterInstallWindow(window.getContentId()));
        window.setTitle(new StringResourceModel("install_title", this, null, new Model(plugin.getName())));
        window.setCloseButtonCallback(new ModalWindow.CloseButtonCallback() {
            public boolean onCloseButtonClicked(AjaxRequestTarget target) {
                if (pluginInstalled == InstallState.INSTALLED_AND_RESTARTED) {
                    main.add(new AttributeModifier("style", ""));
                    installer.setVisible(false);
                }
                //main.add(new AttributeModifier("style", ""));
                target.add(main, installer);
                return true;
            }
        });
        return window;
    }*/


}
