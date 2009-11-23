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

import java.io.PrintStream;

import javax.jcr.Node;

import org.apache.wicket.Component;
import org.apache.wicket.Component.IVisitor;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.protocol.http.WebRequestCycle;
import org.apache.wicket.util.value.ValueMap;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.JcrSessionModel;
import org.hippoecm.frontend.plugin.DummyPlugin;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JavaPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JcrApplicationFactory;
import org.hippoecm.repository.TestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;

public abstract class PluginTest extends TestCase {
    @SuppressWarnings("unused")
    private static final String SVN_ID = "$Id$";

    protected static ValueMap CREDENTIALS = new ValueMap("username=" + SYSTEMUSER_ID + ",password=" + SYSTEMUSER_PASSWORD.toString());

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
    protected IPluginContext context;

    @BeforeClass
    public static void setUpClass() throws Exception {
        TestCase.setUpClass(true);
    }

    @Override
    @Before
    public void setUp() throws Exception {
        setUp(false);
    }

    @Override
    public void setUp(boolean clear) throws Exception {
        super.setUp(clear);
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
        JavaPluginConfig config = new JavaPluginConfig("dummy");
        config.put("plugin.class", DummyPlugin.class.getName());
        context = home.getPluginManager().start(config);
    }

    @After
    public void teardown() throws Exception {
        if(tester != null) {
            tester.destroy();
        }
        if (session != null) {
            session.refresh(false);
            while (session.getRootNode().hasNode("config")) {
                session.getRootNode().getNode("config").remove();
                session.save();
            }
        }
        super.tearDown();
    }

    protected void printComponents(final PrintStream out) {
        home.visitChildren(new IVisitor() {

            public Object component(Component component) {
                String name = component.getClass().getName();
                name = name.substring(name.lastIndexOf('.') + 1);
                out.println(component.getPageRelativePath() + ": " + name);
                return IVisitor.CONTINUE_TRAVERSAL;
            }

        });
    }
    
    protected void refreshPage() {
        WebRequestCycle requestCycle = tester.setupRequestAndResponse(true);;
        HippoTester.callOnBeginRequest(requestCycle);
        AjaxRequestTarget target = new PluginRequestTarget(home);
        requestCycle.setRequestTarget(target);

        // process the request target
        tester.processRequestCycle(requestCycle);
    }

    /**
     * Instantiate a plugin from configuration.
     * @param config A plugin configuration, i.e. plugin.class is defined.
     */
    protected IPluginContext start(IPluginConfig config) {
        return home.getPluginManager().start(config);
    }
}
