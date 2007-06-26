package org.hippocms.repository.webapp;

import org.apache.wicket.protocol.http.WebApplication;

public class Main extends WebApplication {

    public Main() {
    }

    protected void init() {
        super.init();
        getDebugSettings().setAjaxDebugModeEnabled(true);
    }

    public Class getHomePage() {
        return Browser.class;
    }

}
