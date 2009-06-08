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
package org.hippoecm.frontend;

import javax.jcr.Node;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.protocol.http.WebRequestCycle;
import org.apache.wicket.util.value.ValueMap;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.JcrSessionModel;
import org.hippoecm.frontend.plugin.IPlugin;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JcrApplicationFactory;
import org.hippoecm.repository.HippoRepositoryFactory;
import org.hippoecm.repository.TestCase;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

public abstract class PluginTest extends TestCase {
    @SuppressWarnings("unused")
    private static final String SVN_ID = "$Id$";

    protected static ValueMap CREDENTIALS = new ValueMap("username=" + SYSTEMUSER_ID + ",password=" + SYSTEMUSER_PASSWORD.toString());
    
    protected static IPluginContext context;

    public static class DummyPlugin implements IPlugin {
        private static final long serialVersionUID = 1L;

        public DummyPlugin(IPluginContext context, IPluginConfig config) {
            PluginTest.context = context;
        }
    }

    String[] config = new String[] {
            "/config", "nt:unstructured",
            "/config/test-app", "frontend:application",
            "/config/test-app/default", "frontend:plugincluster",
                "/config/test-app/default/plugin", "frontend:plugin",
                    "plugin.class", DummyPlugin.class.getName(),
    };

    protected Node root;
    protected HippoTester tester;
    protected Home home;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp(true);
        while (session.getRootNode().hasNode("config")) {
            session.getRootNode().getNode("config").remove();
            session.save();
            session.refresh(false);
        }
        root = session.getRootNode();
        build(session, config);
        session.save();

        JcrApplicationFactory jcrAppFactory = new JcrApplicationFactory(new JcrNodeModel("/config"));
        tester = new HippoTester(new JcrSessionModel(CREDENTIALS) {
            private static final long serialVersionUID = 1L;

            @Override
            protected Object load() {
                return session;
            }

        }, jcrAppFactory);

        home = tester.startPluginPage();
    }

    @After
    public void teardown() throws Exception {
        tester.destroy();
        if (session != null) {
            session.refresh(false);
            while (session.getRootNode().hasNode("config")) {
                session.getRootNode().getNode("config").remove();
                session.save();
            }
        }
        super.tearDown();
    }

    protected void refreshPage() {
        WebRequestCycle requestCycle = tester.setupRequestAndResponse(true);;
        HippoTester.callOnBeginRequest(requestCycle);
        AjaxRequestTarget target = new PluginRequestTarget(home);
        requestCycle.setRequestTarget(target);

        // process the request target
        tester.processRequestCycle(requestCycle);
    }

    protected IPluginContext start(IPluginConfig config) {
        return home.getPluginManager().start(config);
    }
}
