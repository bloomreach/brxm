/*
 *  Copyright 2009-2013 Hippo B.V. (http://www.onehippo.com)
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
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.wicket.Component;
import org.apache.wicket.RuntimeConfigurationType;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.request.Request;
import org.apache.wicket.request.Response;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.util.value.ValueMap;
import org.apache.wicket.util.visit.IVisit;
import org.apache.wicket.util.visit.IVisitor;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.UserCredentials;
import org.hippoecm.frontend.plugin.DummyPlugin;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.IApplicationFactory;
import org.hippoecm.frontend.plugin.config.impl.JavaPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JcrApplicationFactory;
import org.hippoecm.frontend.session.AccessiblePluginUserSession;
import org.hippoecm.frontend.session.LoginException;
import org.hippoecm.frontend.session.PluginUserSession;
import org.hippoecm.repository.HippoRepository;
import org.junit.After;
import org.junit.Before;
import org.onehippo.repository.testutils.RepositoryTestCase;

public abstract class PluginTest extends RepositoryTestCase {

    protected final class PluginTestApplication extends Main {

        public PluginTestApplication() {
        }

        @Override
        public RuntimeConfigurationType getConfigurationType() {
            // suppress development mode warning from test output
            return RuntimeConfigurationType.DEPLOYMENT;
        }

        @Override
        public String getPluginApplicationName() {
            return "test-app";
        }

        @Override
        public ResourceReference getPluginApplicationFavIconReference() {
            return new PackageResourceReference(PluginTestApplication.class, "test.ico");
        }

        @Override
        public IApplicationFactory getApplicationFactory(final Session jcrSession) {
            return new JcrApplicationFactory(new JcrNodeModel("/config"));
        }

        @Override
        public HippoRepository getRepository() throws RepositoryException {
            return server;
        }

        @Override
        public PluginUserSession newSession(Request request, Response response) {

            AccessiblePluginUserSession userSession = new AccessiblePluginUserSession(request);

            try {
                userSession.login(USER_CREDENTIALS, new LoadableDetachableModel<Session>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected Session load() {
                        return session;
                    }
                });
            } catch (LoginException e) {
                e.printStackTrace();
            }

            return userSession;
        }
    }

    /**
     * Use the USER_CREDENTIALS instead
     */
    @Deprecated
    protected static ValueMap CREDENTIALS = new ValueMap("username=" + RepositoryTestCase.SYSTEMUSER_ID + ",password=" + RepositoryTestCase.SYSTEMUSER_PASSWORD.toString());

    protected static UserCredentials USER_CREDENTIALS = new UserCredentials(RepositoryTestCase.SYSTEMUSER_ID, RepositoryTestCase.SYSTEMUSER_PASSWORD.toString());

    private String[] config = new String[] {
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

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        
        while (session.getRootNode().hasNode("config")) {
            session.getRootNode().getNode("config").remove();
            session.save();
            session.refresh(false);
        }
        root = session.getRootNode();
        build(session, getConfig());
        session.save();

        tester = new HippoTester(new PluginTestApplication());

        home = tester.startPluginPage();
        JavaPluginConfig config = new JavaPluginConfig("dummy");
        config.put("plugin.class", DummyPlugin.class.getName());
        context = ((PluginPage) home).getPluginManager().start(config);
    }

    @After
    @Override
    public void tearDown() throws Exception {
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
        home.visitChildren(new IVisitor<Component, Void>() {

            @Override
            public void component(Component component, IVisit<Void> visit) {
                String name = component.getClass().getName();
                name = name.substring(name.lastIndexOf('.') + 1);
                out.println(component.getPageRelativePath() + ": " + name);
            }

        });
    }
    
    protected void refreshPage() {
        tester.runInAjax(home, new Runnable(){

            @Override
            public void run() {
            }
        });

    }

    /**
     * Instantiate a plugin from configuration.
     * @param config A plugin configuration, i.e. plugin.class is defined.
     */
    protected IPluginContext start(IPluginConfig config) {
        return ((PluginPage) home).getPluginManager().start(config);
    }

    public void setConfig(String[] config) {
        this.config = config;
    }

    public String[] getConfig() {
        return config;
    }
}
