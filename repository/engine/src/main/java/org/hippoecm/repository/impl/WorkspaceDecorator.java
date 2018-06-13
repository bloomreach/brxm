/*
 *  Copyright 2008-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.WeakHashMap;

import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.InvalidSerializedDataException;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.NamespaceRegistry;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Workspace;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventJournal;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.EventListenerIterator;
import javax.jcr.observation.ObservationManager;
import javax.jcr.util.TraversingItemVisitor;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.hippoecm.repository.HierarchyResolverImpl;
import org.hippoecm.repository.api.HierarchyResolver;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoVersionManager;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.jackrabbit.HippoLocalItemStateManager;
import org.hippoecm.repository.jackrabbit.RepositoryImpl;
import org.hippoecm.repository.security.HippoSecurityManager;
import org.hippoecm.repository.security.service.SecurityServiceImpl;
import org.onehippo.repository.security.SecurityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.ContentHandler;

public class WorkspaceDecorator extends SessionBoundDecorator implements HippoWorkspace {

    protected static final Logger logger = LoggerFactory.getLogger(WorkspaceDecorator.class);

    private static final WeakHashMap<Session, Set<EventListener>> listeners = new WeakHashMap<Session, Set<EventListener>>();

    protected final Workspace workspace;
    protected WorkflowManagerImpl workflowManager;

    public static Workspace unwrap(final Workspace workspace) {
        if (workspace instanceof WorkspaceDecorator) {;
            return ((WorkspaceDecorator)workspace).workspace;
        }
        return workspace;
    }

    WorkspaceDecorator(final SessionDecorator session) {
        super(session);
        this.workspace = session.session.getWorkspace();
        workflowManager = null;
    }

    public SessionDecorator getSession() {
        return session;
    }

    public String getName() {
        return workspace.getName();
    }

    public void postMountEnabled(final boolean enabled) {
        ((HippoLocalItemStateManager)((org.apache.jackrabbit.core.WorkspaceImpl)workspace).getItemStateManager()).setEnabled(enabled);
    }

    @Override
    public void importXML(final String parentAbsPath, final InputStream in, final int uuidBehaviour) throws IOException,
            PathNotFoundException, ItemExistsException, ConstraintViolationException, InvalidSerializedDataException,
            LockException, RepositoryException {
        try {
            postMountEnabled(false);
            workspace.importXML(parentAbsPath, in, uuidBehaviour);
        } finally {
            postMountEnabled(true);
        }
    }

    @Override
    public HippoVersionManager getVersionManager() throws UnsupportedRepositoryOperationException, RepositoryException {
        return new VersionManagerDecorator(workspace.getVersionManager(), this);
    }

    @Override
    public WorkflowManager getWorkflowManager() throws RepositoryException {
        if (workflowManager == null) {
            workflowManager = new WorkflowManagerImpl(session);
        }
        return workflowManager;
    }

    @Override
    public HierarchyResolver getHierarchyResolver() throws RepositoryException {
        return new HierarchyResolverImpl();
    }

    @Override
    public SecurityService getSecurityService() throws RepositoryException {
        JackrabbitSession session = (JackrabbitSession) SessionDecorator.unwrap(this.session);
        RepositoryImpl repository = (RepositoryImpl) session.getRepository();
        HippoSecurityManager securityManager = (HippoSecurityManager) repository.getSecurityManager();
        return new SecurityServiceImpl(securityManager, session);
    }

    @Override
    public ObservationManager getObservationManager() throws UnsupportedRepositoryOperationException,
            RepositoryException {
        final ObservationManager upstream = workspace.getObservationManager();
        return new ObservationManager() {

            public void addEventListener(EventListener listener, int eventTypes, String absPath, boolean isDeep,
                    String[] uuid, String[] nodeTypeName, boolean noLocal) throws RepositoryException {
                EventListener registeringListener = listener;
                // if the listener is marked as SynchronousEventListener, then decorate it with Jackrabbit synchronous event listener.
                if (listener instanceof org.hippoecm.repository.api.SynchronousEventListener) {
                    registeringListener = new JackrabbitSynchronousEventListenerDecorator(registeringListener);
                }
                synchronized (listeners) {
                    if (!listeners.containsKey(session)) {
                        listeners.put(session, new HashSet<>());
                    }
                    listeners.get(session).add(registeringListener);
                }
                upstream.addEventListener(registeringListener, eventTypes, absPath, isDeep, uuid, nodeTypeName, noLocal);
            }

            public EventListenerIterator getRegisteredEventListeners() throws RepositoryException {
                // create local copy
                final Set<EventListener> currentListeners = new HashSet<>();
                synchronized (listeners) {
                    if (listeners.containsKey(session)) {
                        currentListeners.addAll(listeners.get(session));
                    }
                }

                return new EventListenerIterator() {
                    private final Iterator<EventListener> listenerIterator = currentListeners.iterator();
                    private final long size = currentListeners.size();
                    private long pos = 0;

                    public EventListener nextEventListener() {
                        if (!hasNext()) {
                            throw new NoSuchElementException();
                        }
                        pos++;
                        EventListener listener = listenerIterator.next();
                        if (listener instanceof JackrabbitSynchronousEventListenerDecorator) {
                            return ((JackrabbitSynchronousEventListenerDecorator) listener).listener;
                        }
                        return listener;
                    }

                    public void skip(long skipNum) {
                        while (skipNum-- > 0) {
                            next();
                        }
                    }

                    public long getSize() {
                        return size;
                    }

                    public long getPosition() {
                        return pos;
                    }

                    public void remove() {
                        throw new UnsupportedOperationException("EventListenerIterator.remove()");
                    }

                    public boolean hasNext() {
                        return listenerIterator.hasNext();
                    }

                    public Object next() {
                        return nextEventListener();
                    }
                };
            }

            public void removeEventListener(final EventListener listener) throws RepositoryException {
                if (!session.isLive()) {
                    return;
                }
                synchronized (listeners) {
                    final Set<EventListener> registered = listeners.get(session);
                    if (registered != null) {
                        for (Iterator<EventListener> iter = registered.iterator(); iter.hasNext();) {
                            final EventListener registeredListener = iter.next();
                            if (registeredListener == listener || (registeredListener instanceof JackrabbitSynchronousEventListenerDecorator && ((JackrabbitSynchronousEventListenerDecorator) registeredListener).listener == listener)) {
                                iter.remove();
                                upstream.removeEventListener(registeredListener);
                                if (registered.size() == 0) {
                                    listeners.remove(session);
                                }
                                break;
                            }
                        }
                    }
                }
            }

            public void setUserData(final String userData) throws RepositoryException {
                upstream.setUserData(userData);
            }

            public EventJournal getEventJournal() throws RepositoryException {
                return upstream.getEventJournal();
            }

            public EventJournal getEventJournal(final int eventTypes, final String absPath, final boolean isDeep,
                                                final String[] uuid, final String[] nodeTypeName) throws RepositoryException {
                return upstream.getEventJournal(eventTypes, absPath, isDeep, uuid, nodeTypeName);
            }
        };
    }

    static class JackrabbitSynchronousEventListenerDecorator implements org.apache.jackrabbit.core.observation.SynchronousEventListener {

        final EventListener listener;

        public JackrabbitSynchronousEventListenerDecorator(final EventListener listener) {
            this.listener = listener;
        }

        public void onEvent(final EventIterator events) {
            listener.onEvent(events);
        }
    }

    private void touch(final String destAbsPath) throws RepositoryException {
        Node destination = null;
        final int parentPathEnd = destAbsPath.lastIndexOf("/");
        final Node parent = (parentPathEnd <= 0) ? getSession().getRootNode()
                : getSession().getNode(destAbsPath.substring(0, parentPathEnd));
        for (NodeIterator iter = parent.getNodes(destAbsPath.substring(parentPathEnd + 1)); iter
                .hasNext();) {
            destination = iter.nextNode();
        }
        if (destination != null) {
            destination.accept(new TraversingItemVisitor.Default() {
                @Override
                public void visit(final Node node) throws RepositoryException {
                    if (node instanceof HippoNode) {
                        try {
                            final Node canonical = ((HippoNode) node).getCanonicalNode();
                            if (canonical == null || !canonical.isSame(node)) {
                                return;
                            }
                        } catch (ItemNotFoundException e) {
                            // canonical node no longer exists
                            return;
                        }
                    }
                    super.visit(node);
                }

                @Override
                protected void leaving(final Node node, final int level) throws RepositoryException {
                    if (node.isNodeType(HippoNodeType.NT_DERIVED)) {
                        if (!node.isCheckedOut()) {
                            for (int depth = node.getDepth(); depth > 0; depth--) {
                                final Node ancestor = (Node) node.getAncestor(depth);
                                if (ancestor.isNodeType("mix:versionable")) {
                                    ancestor.checkout();
                                    break;
                                }
                            }
                        }
                        node.setProperty("hippo:compute", (String) null);
                    }
                }
            });
            destination.save();
        }
    }

    @Override
    public void copy(final String srcAbsPath, final String destAbsPath) throws ConstraintViolationException, VersionException,
            AccessDeniedException, PathNotFoundException, ItemExistsException, LockException, RepositoryException {
        workspace.copy(srcAbsPath, destAbsPath);
        touch(destAbsPath);
    }

    @Override
    public void copy(final String srcWorkspace, final String srcAbsPath, final String destAbsPath) throws NoSuchWorkspaceException,
            ConstraintViolationException, VersionException, AccessDeniedException, PathNotFoundException,
            ItemExistsException, LockException, RepositoryException {
        workspace.copy(srcWorkspace, srcAbsPath, destAbsPath);
        touch(destAbsPath);
    }

    @Override
    public void clone(final String srcWorkspace, final String srcAbsPath, final String destAbsPath, boolean removeExisting)
            throws NoSuchWorkspaceException, ConstraintViolationException, VersionException, AccessDeniedException,
            PathNotFoundException, ItemExistsException, LockException, RepositoryException {
        workspace.clone(srcWorkspace, srcAbsPath, destAbsPath, removeExisting);
        touch(destAbsPath);
    }

    @Override
    public void move(final String srcAbsPath, final String destAbsPath) throws ConstraintViolationException, VersionException,
            AccessDeniedException, PathNotFoundException, ItemExistsException, LockException, RepositoryException {
        workspace.move(srcAbsPath, destAbsPath);
        touch(destAbsPath);
    }

    public void restore(final Version[] versions, final boolean removeExisting) throws ItemExistsException,
            UnsupportedRepositoryOperationException, VersionException, LockException, InvalidItemStateException,
            RepositoryException {
        Version[] unwrapped = Arrays.stream(versions).map(v -> VersionDecorator.unwrap(v)).toArray(size -> new Version[size]);
        workspace.restore(unwrapped, removeExisting);
    }

    public QueryManagerDecorator getQueryManager() throws RepositoryException {
        return new QueryManagerDecorator(session, workspace.getQueryManager());
    }

    public NamespaceRegistry getNamespaceRegistry() throws RepositoryException {
        return workspace.getNamespaceRegistry();
    }

    public NodeTypeManager getNodeTypeManager() throws RepositoryException {
        return workspace.getNodeTypeManager();
    }

    public String[] getAccessibleWorkspaceNames() throws RepositoryException {
        return workspace.getAccessibleWorkspaceNames();
    }

    public ContentHandler getImportContentHandler(final String parentAbsPath, final int uuidBehaviour)
            throws PathNotFoundException, ConstraintViolationException, VersionException, LockException,
            RepositoryException {
        return workspace.getImportContentHandler(parentAbsPath, uuidBehaviour);
    }

    public LockManagerDecorator getLockManager() throws UnsupportedRepositoryOperationException, RepositoryException {
        return new LockManagerDecorator(session, workspace.getLockManager());
    }

    public void createWorkspace(final String name) throws AccessDeniedException, UnsupportedRepositoryOperationException, RepositoryException {
        workspace.createWorkspace(name);
    }

    public void createWorkspace(final String name, final String srcWorkspace) throws AccessDeniedException, UnsupportedRepositoryOperationException, NoSuchWorkspaceException, RepositoryException {
        workspace.createWorkspace(name, srcWorkspace);
    }

    public void deleteWorkspace(final String name) throws AccessDeniedException, UnsupportedRepositoryOperationException, NoSuchWorkspaceException, RepositoryException {
        workspace.deleteWorkspace(name);
    }

    void dispose() {
        if (workflowManager != null) {
            workflowManager.close();
        }
    }
}
