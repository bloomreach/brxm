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

    protected static IPluginContext context;

    public static class DummyPlugin implements IPlugin {
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

    @BeforeClass
    public static void startRepository() throws Exception {
        setRepository(HippoRepositoryFactory.getHippoRepository());
    }

    @AfterClass
    public static void stopRepository() throws Exception {
        tearDownClass(true);
    }

    @Before
    public void setUp() throws Exception {
        super.setUp(false);
        root = session.getRootNode();
        build(session, config);

        tester = new HippoTester(new JcrSessionModel(null) {
            private static final long serialVersionUID = 1L;

            @Override
            protected Object load() {
                return session;
            }
        });

        JcrApplicationFactory jcrAppFactory = new JcrApplicationFactory(new JcrNodeModel("/config"));
        home = (Home) tester.startPage(new Home(jcrAppFactory));
    }

    @After
    public void teardown() throws Exception {
        super.tearDown();
    }

    protected void startContext() {
        
    }
    
    protected IPluginContext start(IPluginConfig config) {
        return home.getPluginManager().start(config);
    }
}
