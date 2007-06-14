package org.hippocms.repository.webapp;

import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.settings.ISecuritySettings;
import org.apache.wicket.util.crypt.ClassCryptFactory;
import org.apache.wicket.util.crypt.NoCrypt;

public class RepositoryBrowser extends WebApplication {

    public RepositoryBrowser() {
    }

    protected void init() {
        super.init();

        // WARNING: DO NOT do this on a real world application unless
        // you really want your app's passwords all passed around and
        // stored in unencrypted browser cookies (BAD IDEA!)!!!

        // The NoCrypt class is being used here because not everyone
        // has the java security classes required by Crypt installed
        // and we want them to be able to run the examples out of the
        // box.
        getSecuritySettings().setCryptFactory(
                new ClassCryptFactory(NoCrypt.class, ISecuritySettings.DEFAULT_ENCRYPTION_KEY));

        // disable debugging mode, because it slows down the tree
        getDebugSettings().setAjaxDebugModeEnabled(false);
    }

    public Class getHomePage() {
        return TreeView.class;
    }

}
