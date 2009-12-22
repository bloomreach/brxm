package org.hippoecm.frontend.session;

import static org.junit.Assert.assertTrue;

import org.apache.wicket.RequestCycle;
import org.apache.wicket.util.value.ValueMap;
import org.hippoecm.frontend.PluginTest;
import org.junit.Test;

public class UserSessionTest extends PluginTest {

    @Test
    public void testSaveOnLogout() throws Exception {
        tester.setupRequestAndResponse();

        UserSession userSession = new UserSession(RequestCycle.get().getRequest());
        userSession.login(new ValueMap("username=admin,password=admin"));

        javax.jcr.Session jcrSession = userSession.getJcrSession();
        jcrSession.getRootNode().addNode("test", "nt:unstructured");
        jcrSession = null;
        userSession = null;
        System.gc();

        Thread.sleep(500);

        RequestCycle.get().detach();

        session.refresh(false);
        assertTrue(session.getRootNode().hasNode("test"));
    }

}
