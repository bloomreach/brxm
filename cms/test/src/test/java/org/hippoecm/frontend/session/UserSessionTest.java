/*
 *  Copyright 2009-2014 Hippo B.V. (http://www.onehippo.com)
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.frontend.session;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.apache.wicket.request.cycle.RequestCycle;
import org.hippoecm.frontend.PluginTest;
import org.hippoecm.frontend.model.UserCredentials;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


public class UserSessionTest extends PluginTest {

    private UserCredentials credentials = new UserCredentials("admin", "admin");

    @Test
    public void testSaveOnLogout() throws Exception {

        PluginUserSession userSession = new PluginUserSession(RequestCycle.get().getRequest());
        userSession.login(credentials);

        javax.jcr.Session jcrSession = userSession.getJcrSession();
        jcrSession.getRootNode().addNode("test", "nt:unstructured");

        // on detach the changes are not persisted
        RequestCycle.get().detach();

        session.refresh(false);
        assertFalse(session.getRootNode().hasNode("test"));

        // on invalidate the backing jcr session gets logged out and saves its changes
        userSession.onInvalidate();

        session.refresh(false);
        assertTrue(session.getRootNode().hasNode("test"));
    }


    @Test
    public void testDontSaveOnLogoutWhenSaveOnExitIsFalse() throws Exception {
        session.getNode("/config/test-app").setProperty("frontend:saveonexit", false);
        session.save();

        PluginUserSession userSession = new PluginUserSession(RequestCycle.get().getRequest());
        userSession.login(credentials);

        javax.jcr.Session jcrSession = userSession.getJcrSession();
        jcrSession.getRootNode().addNode("test", "nt:unstructured");

        // on invalidate the backing jcr session gets logged out and does not save its changes
        // due to saveonexit = false
        userSession.onInvalidate();

        session.refresh(false);
        assertFalse(session.getRootNode().hasNode("test"));
    }

    @Test
    public void assert_jcr_session_gets_logged_out_during_serialization() throws Exception {

        PluginUserSession userSession = new PluginUserSession(RequestCycle.get().getRequest());
        userSession.login(credentials);

        javax.jcr.Session jcrSession = userSession.getJcrSession();

        assertTrue(jcrSession.isLive());

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(bos);
        out.writeObject(userSession);

        assertFalse(jcrSession.isLive());
    }

    @Test
    public void assert_jcr_session_is_new_one_and_live_after_deserialization() throws Exception {
        PluginUserSession userSession = new PluginUserSession(RequestCycle.get().getRequest());
        userSession.login(credentials);

        javax.jcr.Session jcrSession = userSession.getJcrSession();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(bos);
        out.writeObject(userSession);
        bos.flush();
        // assert jcrSession is not live any more
        assertFalse(jcrSession.isLive());

        InputStream bis = new ByteArrayInputStream(bos.toByteArray());
        ObjectInputStream in = new ObjectInputStream(bis);

        PluginUserSession deserUserSession = (PluginUserSession) in.readObject();
        javax.jcr.Session deserJcrSession = deserUserSession.getJcrSession();

        assertTrue(deserJcrSession.isLive());
        assertFalse(deserJcrSession == jcrSession);
    }


}
