/*
 *  Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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

import javax.jcr.Credentials;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.Value;
import javax.transaction.NotSupportedException;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

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
import org.hippoecm.frontend.session.LoginException;
import org.hippoecm.frontend.session.PluginUserSession;
import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.api.InitializationProcessor;
import org.hippoecm.repository.api.ReferenceWorkspace;
import org.hippoecm.repository.api.RepositoryMap;
import org.junit.After;
import org.junit.Before;
import org.onehippo.repository.mock.MockNode;
import org.onehippo.repository.testutils.RepositoryTestCase;

/**
 * Base class for plugin tests that use a mocked repository.
 */
public abstract class MockPluginTest {

    protected final class RepositoryStub implements Repository, HippoRepository {

        @Override
        public Session login() throws javax.jcr.LoginException, RepositoryException {
            return root.getSession();
        }

        @Override
        public Session login(final String username, final char[] password) throws javax.jcr.LoginException, RepositoryException {
            return root.getSession();
        }

        @Override
        public String[] getDescriptorKeys() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isStandardDescriptor(final String key) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isSingleValueDescriptor(final String key) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Value getDescriptorValue(final String key) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Value[] getDescriptorValues(final String key) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getDescriptor(final String key) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Session login(final Credentials credentials, final String workspaceName) throws javax.jcr.LoginException, NoSuchWorkspaceException, RepositoryException {
            return root.getSession();
        }

        @Override
        public Session login(final Credentials credentials) throws javax.jcr.LoginException, RepositoryException {
            return root.getSession();
        }

        @Override
        public Session login(final String workspaceName) throws javax.jcr.LoginException, NoSuchWorkspaceException, RepositoryException {
            throw new UnsupportedOperationException();
        }

        @Override
        public Session login(final SimpleCredentials credentials) throws javax.jcr.LoginException, RepositoryException {
            return root.getSession();
        }

        @Override
        public void close() {
        }

        @Override
        public UserTransaction getUserTransaction(final Session session) throws RepositoryException, NotSupportedException {
            throw new UnsupportedOperationException();
        }

        @Override
        public UserTransaction getUserTransaction(final TransactionManager tm, final Session session) throws NotSupportedException {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getLocation() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Repository getRepository() {
            return new RepositoryStub();
        }

        @Override
        public RepositoryMap getRepositoryMap(final Node node) throws RepositoryException {
            throw new UnsupportedOperationException();
        }

        @Override
        public InitializationProcessor getInitializationProcessor() {
            throw new UnsupportedOperationException();
        }

        @Override
        public ReferenceWorkspace getOrCreateReferenceWorkspace() throws RepositoryException {
            throw new UnsupportedOperationException();
        }
    }

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
            return "mock-plugin-test";
        }

        @Override
        public ResourceReference getPluginApplicationFavIconReference() {
            throw new UnsupportedOperationException();
        }

        @Override
        public IApplicationFactory getApplicationFactory(final Session jcrSession) {
            return new JcrApplicationFactory(new JcrNodeModel("/config"));
        }

        @Override
        public HippoRepository getRepository() throws RepositoryException {
            return new RepositoryStub();
        }

        @Override
        public PluginUserSession newSession(Request request, Response response) {
            UserCredentials fakeCredentials = new UserCredentials("admin", "admin");
            try {
                PluginUserSession.setCredentials(fakeCredentials);
            } catch (RepositoryException e) {
                throw new IllegalStateException("Cannot set fake credentials in test", e);
            }
            return new PluginUserSession(request);
        }

    }

    protected MockNode root;
    protected HippoTester tester;
    protected Home home;
    protected IPluginContext context;

    @Before
    public void setUp() throws Exception {
        root = MockNode.root();

        tester = new HippoTester(new PluginTestApplication());

        home = tester.startPluginPage();
        JavaPluginConfig config = new JavaPluginConfig("dummy");
        config.put("plugin.class", DummyPlugin.class.getName());
        context = ((PluginPage) home).getPluginManager().start(config);
    }

    @After
    public void tearDown() throws Exception {
        if (tester != null) {
            tester.destroy();
        }
    }

}
