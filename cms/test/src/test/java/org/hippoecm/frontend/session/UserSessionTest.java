/*
 *  Copyright 2009 Hippo.
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

import org.apache.wicket.RequestCycle;
import org.apache.wicket.util.value.ValueMap;
import org.hippoecm.frontend.PluginTest;
import org.junit.Test;

import static org.junit.Assert.assertTrue;


public class UserSessionTest extends PluginTest {

    @Test
    public void testSaveOnLogout() throws Exception {
        tester.setupRequestAndResponse();

        PluginUserSession userSession = new PluginUserSession(RequestCycle.get().getRequest());
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

    @Test
    public void testDontSaveOnLogoutWhenSaveOnExitIsFalse() throws Exception {
        tester.setupRequestAndResponse();

        session.getNode("/config/test-app").setProperty("frontend:saveonexit", false);

        PluginUserSession userSession = new PluginUserSession(RequestCycle.get().getRequest());
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
