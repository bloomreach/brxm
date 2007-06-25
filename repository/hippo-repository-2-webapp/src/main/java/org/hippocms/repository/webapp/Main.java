package org.hippocms.repository.webapp;

import org.apache.wicket.protocol.http.WebApplication;

public class Main extends WebApplication {

    public Main() {
    }

    protected void init() {
        super.init();
        // disable ajax debugging mode for the time being.
        getDebugSettings().setAjaxDebugModeEnabled(false);
    }

    public Class getHomePage() {
        return Browser.class;
    }

}
