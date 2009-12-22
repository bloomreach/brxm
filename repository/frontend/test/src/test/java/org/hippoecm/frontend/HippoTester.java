/*
 *  Copyright 2008 Hippo.
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
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.ValueFactory;
import javax.jcr.Workspace;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.version.VersionException;
import javax.transaction.xa.XAResource;

import org.apache.wicket.Request;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.Response;
import org.apache.wicket.Session;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.request.IRequestCycleProcessor;
import org.apache.wicket.util.tester.WicketTester;
import org.apache.wicket.util.value.ValueMap;
import org.hippoecm.frontend.plugin.config.impl.IApplicationFactory;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.api.HippoSession;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

public class HippoTester extends WicketTester {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static class DummySession implements HippoSession {

        public void addLockToken(String lt) {
            // TODO Auto-generated method stub
            
        }

        public void checkPermission(String absPath, String actions) throws AccessControlException, RepositoryException {
            throw new RepositoryException();
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
            throw new RepositoryException();
        }

        public String[] getLockTokens() {
            // TODO Auto-generated method stub
            return null;
        }

        public String getNamespacePrefix(String uri) throws NamespaceException, RepositoryException {
            throw new RepositoryException();
        }

        public String[] getNamespacePrefixes() throws RepositoryException {
            throw new RepositoryException();
        }

        public String getNamespaceURI(String prefix) throws NamespaceException, RepositoryException {
            throw new RepositoryException();
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
            throw new RepositoryException();
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
            throw new RepositoryException();
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
            throw new RepositoryException();
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

        public ClassLoader getSessionClassLoader() throws RepositoryException {
            throw new RepositoryException();
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
            throw new RepositoryException();
        }

        public NodeIterator pendingChanges(Node node, String nodeType) throws NamespaceException,
                NoSuchNodeTypeException, RepositoryException {
            throw new RepositoryException();
        }

        public NodeIterator pendingChanges() throws RepositoryException {
            throw new RepositoryException();
        }
        
    }
    
    private IApplicationFactory appFactory;

    public HippoTester() {
        this(null);
    }

    public HippoTester(IApplicationFactory factory) {
        this(new Main() {
            
            @Override
            public HippoRepository getRepository() throws RepositoryException {
                return null;
            }

            @Override
            public Session newSession(Request request, Response response) {
                UserSession session = new UserSession(request, new LoadableDetachableModel<javax.jcr.Session>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected javax.jcr.Session load() {
                        return new DummySession();
                    }
                    
                });
                return session;
            }
        }, factory);
    }

    public HippoTester(Main main, IApplicationFactory jcrAppFactory) {
        super(main);

        this.appFactory = jcrAppFactory;
    }

    public Home startPluginPage() {
        Home home;
        // create a request cycle, but don't use it.
        // this is a workaround for mockwebapplication's retaining of these cycles. 
        RequestCycle rc = createRequestCycle();
        if (appFactory != null) {
            home = (Home) super.startPage(new Home(appFactory));
        } else {
            home = (Home) super.startPage(Home.class);
        }
        rc.detach();
        return home;
    }
    
}
