/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.AccessControlException;
import java.util.NoSuchElementException;

import javax.jcr.AccessDeniedException;
import javax.jcr.Credentials;
import javax.jcr.InvalidItemStateException;
import javax.jcr.InvalidSerializedDataException;
import javax.jcr.Item;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.LoginException;
import javax.jcr.NamespaceException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.ValueFactory;
import javax.jcr.Workspace;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.retention.RetentionManager;
import javax.jcr.security.AccessControlManager;
import javax.jcr.version.VersionException;
import javax.servlet.ServletContext;
import javax.transaction.xa.XAResource;

import org.apache.wicket.Application;
import org.apache.wicket.RuntimeConfigurationType;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.page.IPageManagerContext;
import org.apache.wicket.request.Request;
import org.apache.wicket.request.Response;
import org.apache.wicket.util.tester.WicketTester;
import org.hippoecm.frontend.plugin.config.IPluginConfigService;
import org.hippoecm.frontend.plugin.config.impl.IApplicationFactory;
import org.hippoecm.frontend.plugin.config.impl.JavaClusterConfig;
import org.hippoecm.frontend.plugin.config.impl.JavaConfigService;
import org.hippoecm.frontend.session.PluginUserSession;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.api.HippoSession;
import org.onehippo.repository.security.User;
import org.onehippo.repository.security.domain.DomainRuleExtension;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

public class HippoTester extends WicketTester {

    static class NullNodeIterator implements NodeIterator {

        public Node nextNode() {
            throw new NoSuchElementException();
        }

        public long getPosition() {
            return 0;
        }

        public long getSize() {
            return 0;
        }

        public void skip(long skipNum) {
            if (skipNum > 0) {
                throw new NoSuchElementException();
            }
        }

        public boolean hasNext() {
            return false;
        }

        public Object next() {
            throw new NoSuchElementException();
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }

    }

    private static class DummySession implements HippoSession {

        public void addLockToken(String lt) {
            // TODO Auto-generated method stub

        }

        public void checkPermission(String absPath, String actions) throws AccessControlException, RepositoryException {
            // everything is fine
        }

        public void exportDocumentView(String absPath, ContentHandler contentHandler, boolean skipBinary,
                boolean noRecurse) throws PathNotFoundException, SAXException, RepositoryException {
            throw new RepositoryException();
        }

        public void exportDocumentView(String absPath, OutputStream out, boolean skipBinary, boolean noRecurse)
                throws IOException, PathNotFoundException, RepositoryException {
            throw new RepositoryException();
        }

        public void exportSystemView(String absPath, ContentHandler contentHandler, boolean skipBinary,
                boolean noRecurse) throws PathNotFoundException, SAXException, RepositoryException {
            throw new RepositoryException();
        }

        public void exportSystemView(String absPath, OutputStream out, boolean skipBinary, boolean noRecurse)
                throws IOException, PathNotFoundException, RepositoryException {
            throw new RepositoryException();
        }

        public Object getAttribute(String name) {
            // TODO Auto-generated method stub
            return null;
        }

        public String[] getAttributeNames() {
            // TODO Auto-generated method stub
            return null;
        }

        public ContentHandler getImportContentHandler(String parentAbsPath, int uuidBehavior)
                throws PathNotFoundException, ConstraintViolationException, VersionException, LockException,
                RepositoryException {
            throw new RepositoryException();
        }

        public Item getItem(String absPath) throws PathNotFoundException, RepositoryException {
            throw new PathNotFoundException();
        }

        public String[] getLockTokens() {
            // TODO Auto-generated method stub
            return null;
        }

        public String getNamespacePrefix(String uri) throws NamespaceException, RepositoryException {
            throw new NamespaceException();
        }

        public String[] getNamespacePrefixes() throws RepositoryException {
            return new String[0];
        }

        public String getNamespaceURI(String prefix) throws NamespaceException, RepositoryException {
            throw new NamespaceException();
        }

        public Node getNodeByUUID(String uuid) throws ItemNotFoundException, RepositoryException {
            throw new RepositoryException();
        }

        public Repository getRepository() {
            // TODO Auto-generated method stub
            return null;
        }

        public Node getRootNode() throws RepositoryException {
            throw new RepositoryException();
        }

        public String getUserID() {
            // TODO Auto-generated method stub
            return null;
        }

        public ValueFactory getValueFactory() throws UnsupportedRepositoryOperationException, RepositoryException {
            throw new RepositoryException();
        }

        public Workspace getWorkspace() {
            // TODO Auto-generated method stub
            return null;
        }

        public boolean hasPendingChanges() throws RepositoryException {
            return false;
        }

        public javax.jcr.Session impersonate(Credentials credentials) throws LoginException, RepositoryException {
            throw new RepositoryException();
        }

        public void importXML(String parentAbsPath, InputStream in, int uuidBehavior) throws IOException,
                PathNotFoundException, ItemExistsException, ConstraintViolationException, VersionException,
                InvalidSerializedDataException, LockException, RepositoryException {
            throw new RepositoryException();
        }

        public boolean isLive() {
            // TODO Auto-generated method stub
            return true;
        }

        public boolean itemExists(String absPath) throws RepositoryException {
            return false;
        }

        public void logout() {
            // TODO Auto-generated method stub

        }

        public void move(String srcAbsPath, String destAbsPath) throws ItemExistsException, PathNotFoundException,
                VersionException, ConstraintViolationException, LockException, RepositoryException {
            throw new RepositoryException();
        }

        public void refresh(boolean keepChanges) throws RepositoryException {
        }

        public void removeLockToken(String lt) {
        }

        public void save() throws AccessDeniedException, ItemExistsException, ConstraintViolationException,
                InvalidItemStateException, VersionException, LockException, NoSuchNodeTypeException,
                RepositoryException {
        }

        public void setNamespacePrefix(String prefix, String uri) throws NamespaceException, RepositoryException {
            throw new RepositoryException();
        }

        public Node copy(Node srcNode, String destAbsNodePath) throws RepositoryException {
            throw new RepositoryException();
        }

        public void exportDereferencedView(String absPath, OutputStream out, boolean binaryAsLink, boolean noRecurse)
                throws IOException, PathNotFoundException, RepositoryException {
            throw new RepositoryException();
        }

        public void exportDereferencedView(String absPath, ContentHandler contentHandler, boolean skipBinary,
                boolean noRecurse) throws PathNotFoundException, SAXException, RepositoryException {
            throw new RepositoryException();
        }

        public ClassLoader getSessionClassLoader() throws RepositoryException {
            throw new RepositoryException();
        }

        @Override
        public User getUser() throws ItemNotFoundException, RepositoryException {
            throw new ItemNotFoundException();
        }

        public XAResource getXAResource() {
            // TODO Auto-generated method stub
            return null;
        }

        public void importDereferencedXML(String parentAbsPath, InputStream in, int uuidBehavior,
                int referenceBehavior, int mergeBehavior) throws IOException, PathNotFoundException,
                ItemExistsException, ConstraintViolationException, VersionException, InvalidSerializedDataException,
                LockException, RepositoryException {
            throw new RepositoryException();
        }

        public NodeIterator pendingChanges(Node node, String nodeType, boolean prune) throws NamespaceException,
                NoSuchNodeTypeException, RepositoryException {
            return new NullNodeIterator();
        }

        public NodeIterator pendingChanges(Node node, String nodeType) throws NamespaceException,
                NoSuchNodeTypeException, RepositoryException {
            return new NullNodeIterator();
        }

        public NodeIterator pendingChanges() throws RepositoryException {
            return new NullNodeIterator();
        }

        public Node getNodeByIdentifier(String id) throws ItemNotFoundException, RepositoryException {
            throw new ItemNotFoundException();
        }

        public Node getNode(String absPath) throws PathNotFoundException, RepositoryException {
            throw new PathNotFoundException();
        }

        public Property getProperty(String absPath) throws PathNotFoundException, RepositoryException {
            throw new PathNotFoundException();
        }

        public boolean nodeExists(String absPath) throws RepositoryException {
            return false;
        }

        public boolean propertyExists(String absPath) throws RepositoryException {
            return false;
        }

        public void removeItem(String absPath) throws VersionException, LockException, ConstraintViolationException, AccessDeniedException, RepositoryException {
            throw new RepositoryException();
        }

        public boolean hasPermission(String absPath, String actions) throws RepositoryException {
            return true;
        }

        public boolean hasCapability(String methodName, Object target, Object[] arguments) throws RepositoryException {
            return false;
        }

        public AccessControlManager getAccessControlManager() throws UnsupportedRepositoryOperationException, RepositoryException {
            throw new UnsupportedRepositoryOperationException();
        }

        public RetentionManager getRetentionManager() throws UnsupportedRepositoryOperationException, RepositoryException {
            throw new UnsupportedRepositoryOperationException();
        }

        public void registerSessionCloseCallback(CloseCallback callback) {
        }

        @Override
        public Session createSecurityDelegate(final Session session, final DomainRuleExtension... domainExtensions) throws RepositoryException {
            throw new UnsupportedOperationException();
        }

        @Override
        public void localRefresh() {
            // TODO Auto-generated method stub
        }
    }

    public HippoTester() {
        this(new HippoTesterApplication());
    }

    public HippoTester(Main main) {
        super(main);
    }

    public HippoTester(Main main, ServletContext context) {
        super(main, context);
    }

    public Home startPluginPage() {
        Home home;
        // create a request cycle, but don't use it.
        // this is a workaround for mockwebapplication's retaining of these cycles.
        // FIXME: still necessary?
//        RequestCycle rc = createRequestCycle();
        home = super.startPage(PluginPage.class);
//        rc.detach();

        Behavior behavior = new TestExecutorBehavior();
        home.add(behavior);
        return home;
    }

    public IPageManagerContext getPageManagerContext() {
        return ((Main) Application.get()).getPageManagerContext();
    }

    public void runInAjax(Home page, Runnable callback) {
        final TestExecutorBehavior executor = page.getBehaviors(TestExecutorBehavior.class).get(0);
        executor.setCallback(callback);
        executeBehavior(executor);
    }

    static class TestApplicationFactory implements IApplicationFactory {

        public IPluginConfigService getDefaultApplication() {
            return getApplication(null);
        }

        public IPluginConfigService getApplication(String name) {
            JavaConfigService configService = new JavaConfigService("test");
            JavaClusterConfig plugins = new JavaClusterConfig();
            configService.addClusterConfig("test", plugins);
            return configService;
        }
    }

    private static class TestExecutorBehavior extends AbstractDefaultAjaxBehavior {

        private Runnable callback;

        void setCallback(Runnable callback) {
            this.callback = callback;
        }

        @Override
        protected void respond(final AjaxRequestTarget target) {
            try {
                callback.run();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static class HippoTesterApplication extends Main {

        @Override
        public RuntimeConfigurationType getConfigurationType() {
            return RuntimeConfigurationType.DEVELOPMENT;
        }

        @Override
        // suppress development mode warning from test output
        protected void outputDevelopmentModeWarning() {
        }

        @Override
        public HippoRepository getRepository() throws RepositoryException {
            return null;
        }

        @Override
        public IApplicationFactory getApplicationFactory(final Session jcrSession) {
            return new TestApplicationFactory();
        }

        @Override
        public UserSession newSession(Request request, Response response) {
            return new PluginUserSession(request, new LoadableDetachableModel<Session>() {
                private static final long serialVersionUID = 1L;

                @Override
                protected Session load() {
                    return new DummySession();
                }

            });
        }
    }
}
