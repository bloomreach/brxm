package org.onehippo.cms7.essentials.app;

import org.apache.wicket.RuntimeConfigurationType;
import org.apache.wicket.guice.GuiceComponentInjector;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.protocol.http.WebApplication;
import org.onehippo.cms7.essentials.dashboard.utils.inject.EventBusModule;
import org.onehippo.cms7.essentials.setup.SetupPage;

import com.google.inject.Guice;
import com.google.inject.Injector;

/**
 *
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
        final Injector injector = Guice.createInjector(EventBusModule.getInstance());
        getComponentInstantiationListeners().add(new GuiceComponentInjector(this, injector));

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBusModule.getInstance().cleanup();


    }

    @Override
    public RuntimeConfigurationType getConfigurationType() {
        // TODO call super, testing only
        return RuntimeConfigurationType.DEVELOPMENT;
    }
}
