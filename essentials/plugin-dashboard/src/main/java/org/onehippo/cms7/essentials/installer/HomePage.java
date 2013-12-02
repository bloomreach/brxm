package org.onehippo.cms7.essentials.installer;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletContext;

import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.commons.cnd.CndImporter;
import org.apache.jackrabbit.commons.cnd.ParseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.onehippo.cms7.essentials.dashboard.DashboardPlugin;
import org.onehippo.cms7.essentials.dashboard.Plugin;
import org.onehippo.cms7.essentials.dashboard.ctx.DashboardPluginContext;
import org.onehippo.cms7.essentials.dashboard.ctx.PanelPluginContext;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.config.ProjectSettingsBean;
import org.onehippo.cms7.essentials.dashboard.setup.ProjectSetupPlugin;
import org.onehippo.cms7.essentials.dashboard.utils.GlobalUtils;
import org.onehippo.cms7.essentials.dashboard.utils.PluginScanner;
import org.onehippo.cms7.essentials.installer.panels.BodyPanel;
import org.onehippo.cms7.essentials.installer.panels.DashboardPanel;
import org.onehippo.cms7.essentials.installer.panels.GlobalToolbarPanel;
import org.onehippo.cms7.essentials.installer.panels.MarketplacePanel;
import org.onehippo.cms7.essentials.installer.panels.MenuPanel;
import org.onehippo.cms7.essentials.installer.panels.SetupPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.eventbus.EventBus;
import com.google.inject.Inject;

public class HomePage extends WebPage implements IHeaderContributor {


    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(HomePage.class);
    private static final Predicate<Plugin> MAIN_PLUGIN = new Predicate<Plugin>() {
        @Override
        public boolean apply(final Plugin plugin) {
            final String type = plugin.getType();
            return type != null && "system".contains(type);
        }
    };
    final ImmutableList<Plugin> mainPlugins;
    private final BodyPanel body;
    private final Panel globalToolbarPanel;
    private final ImmutableList<Plugin> pluginList;
    // cannot serialize:
    @Inject
    private transient EventBus eventBus;
    private MenuPanel menu;
    private Plugin selectedPlugin;

    @SuppressWarnings("unchecked")
    public HomePage(final PageParameters parameters) {
        super(parameters);
        final PluginScanner scanner = new PluginScanner();
        final ServletContext servletContext = WebApplication.get().getServletContext();
        final String libPath = servletContext.getRealPath("/WEB-INF/lib");
        log.info("Scanning path for essentials: {}", libPath);
        final List<Plugin> plugins = scanner.scan(libPath);
        for (Plugin plugin : plugins) {
            log.info("Found plugin: {}", plugin);
        }

        body = new BodyPanel("body");
        // filter system essentials:
        mainPlugins = ImmutableList.copyOf(Iterables.filter(plugins, Predicates.and(MAIN_PLUGIN)));
        pluginList = ImmutableList.copyOf(Iterables.filter(plugins, Predicates.not(MAIN_PLUGIN)));

        menu = new MenuPanel("menu", this, pluginList, mainPlugins);

        globalToolbarPanel = new GlobalToolbarPanel("globalToolbar", new PanelPluginContext(GlobalUtils.createSession(), null));
        add(globalToolbarPanel);
        add(menu);
        add(body);
        // TODO: do not use own bootstrapping, deploy plugins API with CMS! @see hippoecm-extension.xml within API module
        // run setup & bootstrapping:
        //bootstrap();

        body.replace(new DashboardPanel("plugin", pluginList, eventBus));

    }

    public static DashboardPlugin instantiatePlugin(final Plugin plugin, final PluginContext context, final String pluginClass) {
        try {
            @SuppressWarnings("unchecked")
            final Class<DashboardPlugin> clazz = (Class<DashboardPlugin>) Class.forName(pluginClass);
            final Constructor<DashboardPlugin> constructor = clazz.getConstructor(String.class, Plugin.class, PluginContext.class);
            return constructor.newInstance("plugin", plugin, context);
        } catch (ClassNotFoundException e) {
            log.error("Couldn't find plugin class", e);
        } catch (InstantiationException e) {
            log.error("Error instantiating plugin", e);
        } catch (IllegalAccessException e) {
            log.error("Error instantiating plugin/access", e);
        } catch (NoSuchMethodException e) {
            log.error("Invalid constructor called", e);
        } catch (InvocationTargetException e) {
            log.error("Error constructing plugin", e);
        }
        return null;
    }

    /**
     * Triggered when menu item is clicked
     *
     * @param plugin plugin model of clicked item
     * @param target ajax target of clicked item
     */
    public void onPluginSelected(final Plugin plugin, final AjaxRequestTarget target) {
        log.info("Plugin selected:  {}", plugin);

        final PluginContext context = new DashboardPluginContext(GlobalUtils.createSession(), plugin);
        // inject project settings:
        final ProjectSettingsBean document = context.getConfigService().read(ProjectSetupPlugin.class.getName());
        if (document != null) {
            context.setBeansPackageName(document.getSelectedBeansPackage());
            context.setComponentsPackageName(document.getSelectedComponentsPackage());
            context.setRestPackageName(document.getSelectedRestPackage());
            context.setProjectNamespacePrefix(document.getProjectNamespace());
        }
        final String pluginClass = plugin.getPluginClass();

        selectedPlugin = plugin;


        final DashboardPlugin component = instantiatePlugin(plugin, context, pluginClass);
        //add(feedbackPanel);
        log.debug("Created component {}", component);
        if (component != null) {
            body.replace(component);
            target.add(body);
        }

    }

    public void onMarketplaceSelected(final AjaxRequestTarget target) {
        selectedPlugin = null;
        body.replace(new MarketplacePanel("plugin", pluginList, eventBus));
        target.add(body);
    }

    public void onSetupSelected(final AjaxRequestTarget target) {

        selectedPlugin = null;

        Plugin plugin = getPluginByName("Settings");
        if (plugin == null) {
            log.info("Settings plugin not found");
        }
        final PluginContext context = new DashboardPluginContext(GlobalUtils.createSession(), plugin);
        body.replace(new SetupPanel("plugin", plugin, context));
        target.add(body);
    }

    private Plugin getPluginByName(String name) {
        Plugin plugin = null;
        for (final Plugin next : mainPlugins) {
            if (next.getName().equals(name)) {
                plugin = next;
            }
        }
        if (plugin != null) {
            return plugin;
        } else {
            for (final Plugin next : pluginList) {
                if (next.getName().equals(name)) {
                    plugin = next;
                }
            }
            return plugin;
        }
    }


    public Plugin getSelectedPlugin() {
        return selectedPlugin;
    }
}
