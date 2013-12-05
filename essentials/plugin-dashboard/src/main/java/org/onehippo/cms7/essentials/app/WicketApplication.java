package org.onehippo.cms7.essentials.app;

import java.util.List;

import javax.servlet.ServletContext;

import org.apache.wicket.guice.GuiceComponentInjector;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.protocol.http.WebApplication;
import org.onehippo.cms7.essentials.dashboard.Plugin;
import org.onehippo.cms7.essentials.dashboard.config.ProjectSettingsBean;
import org.onehippo.cms7.essentials.dashboard.ctx.DashboardPluginContext;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.setup.ProjectSetupPlugin;
import org.onehippo.cms7.essentials.dashboard.utils.GlobalUtils;
import org.onehippo.cms7.essentials.dashboard.utils.PluginScanner;
import org.onehippo.cms7.essentials.dashboard.utils.inject.EventBusModule;
import org.onehippo.cms7.essentials.dashboard.utils.inject.PropertiesModule;
import org.onehippo.cms7.essentials.installer.HomePage;
import org.onehippo.cms7.essentials.setup.SetupPage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class WicketApplication extends WebApplication {

    private static final Logger log = LoggerFactory.getLogger(WicketApplication.class);

    @Override
    public Class<? extends WebPage> getHomePage() {
        // TODO: mm disable this after sprint is done
        if(true){
            return HomePage.class;
        }


        // check if setup is done and show different page:
        final PluginScanner scanner = new PluginScanner();
        final ServletContext servletContext = WebApplication.get().getServletContext();
        final String libPath = servletContext.getRealPath("/WEB-INF/lib");
        log.debug("Scanning path for essentials: {}", libPath);
        final List<Plugin> plugins = scanner.scan(libPath);
        Plugin plugin = getPluginByName("Settings", plugins);
        PluginContext context = new DashboardPluginContext(GlobalUtils.createSession(), plugin);
        final ProjectSettingsBean document = context.getConfigService().read(ProjectSetupPlugin.class.getName());
        if (document != null && document.getSetupDone()) {

            return HomePage.class;
        }

        return SetupPage.class;
    }

    private Plugin getPluginByName(String name, Iterable<Plugin> plugins) {
        for (final Plugin next : plugins) {
            if (next.getName().equals(name)) {
                return next;
            }
        }
        return null;
    }

    @Override
    public void init() {
        super.init();
        getMarkupSettings().setStripWicketTags(true);
        // inject modules
        getComponentInstantiationListeners().add(new GuiceComponentInjector(this, EventBusModule.getInstance(), new PropertiesModule()));

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBusModule.getInstance().cleanup();
    }
}
