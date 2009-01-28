package org.hippoecm.frontend;

import org.apache.wicket.Request;
import org.apache.wicket.Response;
import org.apache.wicket.Session;
import org.apache.wicket.util.tester.WicketTester;
import org.hippoecm.frontend.model.JcrSessionModel;
import org.hippoecm.frontend.session.UserSession;

public class HippoTester extends WicketTester {

    public HippoTester() {
        this(new JcrSessionModel(null) {
            @Override
            protected Object load() {
                return null;
            }
        });
    }

    public HippoTester(final JcrSessionModel sessionModel) {
        super(new NonPageCachingDummyWebApplication() {
            @Override
            public Session newSession(Request request, Response response) {
                return new UserSession(request, sessionModel);
            }
        });
    }

}
