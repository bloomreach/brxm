package org.onehippo.cms7.essentials.installer;

import org.apache.wicket.RuntimeConfigurationType;
import org.apache.wicket.guice.GuiceComponentInjector;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.protocol.http.WebApplication;
import org.onehippo.cms7.essentials.dashboard.utils.inject.EventBusModule;
import org.onehippo.cms7.essentials.setup.SetupPage;

import com.google.inject.Guice;
import com.google.inject.Injector;

/**
 * Application object for your web application. If you want to run this application without deploying, run the Start class.
 *
 * @see org.onehippo.cms7.essentials.installer.Start#main(String[])
 */
public class WicketApplication extends WebApplication {
    /**
     * @see org.apache.wicket.Application#getHomePage()
     */
    @Override
    public Class<? extends WebPage> getHomePage() {
        return SetupPage.class;
    }

    /**
     * @see org.apache.wicket.Application#init()
     */
    @Override
    public void init() {
        super.init();
        getMarkupSettings().setStripWicketTags(true);
        // inject event bus:
        final Injector injector = Guice.createInjector(new EventBusModule());
        getComponentInstantiationListeners().add(new GuiceComponentInjector(this, injector));

    }




    @Override
    public RuntimeConfigurationType getConfigurationType() {
        // TODO call super, testing only
        return RuntimeConfigurationType.DEVELOPMENT;
    }
}
