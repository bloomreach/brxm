/*
 *  Copyright 2008-2014 Hippo B.V. (http://www.onehippo.com)
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
import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.WeakHashMap;

import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidSerializedDataException;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Workspace;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventJournal;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.EventListenerIterator;
import javax.jcr.observation.ObservationManager;
import javax.jcr.util.TraversingItemVisitor;
import javax.jcr.version.VersionException;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.hippoecm.repository.HierarchyResolverImpl;
import org.hippoecm.repository.api.HierarchyResolver;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoVersionManager;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.decorating.DecoratorFactory;
import org.hippoecm.repository.jackrabbit.HippoLocalItemStateManager;
import org.hippoecm.repository.jackrabbit.RepositoryImpl;
import org.hippoecm.repository.security.HippoSecurityManager;
import org.hippoecm.repository.security.service.SecurityServiceImpl;
import org.onehippo.repository.security.SecurityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple workspace decorator.
 */
public class WorkspaceDecorator extends org.hippoecm.repository.decorating.WorkspaceDecorator implements HippoWorkspace {

    protected static final Logger logger = LoggerFactory.getLogger(WorkspaceDecorator.class);

    private static final WeakHashMap<Session, Set<EventListener>> listeners = new WeakHashMap<Session, Set<EventListener>>();

    /** The underlying workspace instance. */
    protected final Workspace workspace;
    protected Session session;
    protected WorkflowManagerImpl workflowManager;

    /**
     * Creates a workspace decorator.
     *
     * @param factory
     * @param session
     * @param workspace
     */
    public WorkspaceDecorator(DecoratorFactory factory, Session session, Workspace workspace) {
        super(factory, session, workspace);
        this.session = session;
        this.workspace = workspace;
        workflowManager = null;
    }

    public void postMountEnabled(boolean enabled) {
        ((HippoLocalItemStateManager)((org.apache.jackrabbit.core.WorkspaceImpl)workspace).getItemStateManager()).setEnabled(enabled);
    }

    @Override
    public void importXML(String parentAbsPath, InputStream in, int uuidBehaviour) throws IOException,
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
        final ObservationManager upstream = super.getObservationManager();
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
                        listeners.put(session, new HashSet<EventListener>());
                    }
                    listeners.get(session).add(registeringListener);
                }
                upstream.addEventListener(registeringListener, eventTypes, absPath, isDeep, uuid, nodeTypeName, noLocal);
            }

            public EventListenerIterator getRegisteredEventListeners() throws RepositoryException {
                // create local copy
                final Set<EventListener> currentListeners = new HashSet<EventListener>();
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

            public void removeEventListener(EventListener listener) throws RepositoryException {
                if (!session.isLive()) {
                    return;
                }
                synchronized (listeners) {
                    Set<EventListener> registered = listeners.get(session);
                    if (registered != null) {
                        for (Iterator<EventListener> iter = registered.iterator(); iter.hasNext();) {
                            EventListener registeredListener = iter.next();
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

            public void setUserData(String userData) throws RepositoryException {
                upstream.setUserData(userData);
            }

            public EventJournal getEventJournal() throws RepositoryException {
                return upstream.getEventJournal();
            }

            public EventJournal getEventJournal(int eventTypes, String absPath, boolean isDeep, String[] uuid, String[] nodeTypeName) throws RepositoryException {
                return upstream.getEventJournal(eventTypes, absPath, isDeep, uuid, nodeTypeName);
            }
        };
    }

    static class JackrabbitSynchronousEventListenerDecorator implements org.apache.jackrabbit.core.observation.SynchronousEventListener {
        EventListener listener;

        public JackrabbitSynchronousEventListenerDecorator(EventListener listener) {
            this.listener = listener;
        }

        public void onEvent(EventIterator events) {
            listener.onEvent(events);
        }
    }

    private void touch(String destAbsPath) throws RepositoryException {
        Node destination = null;
        int parentPathEnd = destAbsPath.lastIndexOf("/");
        Node parent = (parentPathEnd <= 0) ? getSession().getRootNode()
                : getSession().getNode(destAbsPath.substring(0, parentPathEnd));
        for (NodeIterator iter = parent.getNodes(destAbsPath.substring(parentPathEnd + 1)); iter
                .hasNext();) {
            destination = iter.nextNode();
        }
        if (destination != null) {
            destination.accept(new TraversingItemVisitor.Default() {
                @Override
                public void visit(Node node) throws RepositoryException {
                    if (node instanceof HippoNode) {
                        try {
                            Node canonical = ((HippoNode) node).getCanonicalNode();
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
                protected void leaving(Node node, int level) throws RepositoryException {
                    if (node.isNodeType(HippoNodeType.NT_DERIVED)) {
                        if (!node.isCheckedOut()) {
                            for (int depth = node.getDepth(); depth > 0; depth--) {
                                Node ancestor = (Node) node.getAncestor(depth);
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
    public void copy(String srcAbsPath, String destAbsPath) throws ConstraintViolationException, VersionException,
            AccessDeniedException, PathNotFoundException, ItemExistsException, LockException, RepositoryException {
        super.copy(srcAbsPath, destAbsPath);
        touch(destAbsPath);
    }

    @Override
    public void copy(String srcWorkspace, String srcAbsPath, String destAbsPath) throws NoSuchWorkspaceException,
            ConstraintViolationException, VersionException, AccessDeniedException, PathNotFoundException,
            ItemExistsException, LockException, RepositoryException {
        super.copy(srcWorkspace, srcAbsPath, destAbsPath);
        touch(destAbsPath);
    }

    @Override
    public void clone(String srcWorkspace, String srcAbsPath, String destAbsPath, boolean removeExisting)
            throws NoSuchWorkspaceException, ConstraintViolationException, VersionException, AccessDeniedException,
            PathNotFoundException, ItemExistsException, LockException, RepositoryException {
        super.clone(srcWorkspace, srcAbsPath, destAbsPath, removeExisting);
        touch(destAbsPath);
    }

    @Override
    public void move(String srcAbsPath, String destAbsPath) throws ConstraintViolationException, VersionException,
            AccessDeniedException, PathNotFoundException, ItemExistsException, LockException, RepositoryException {
        super.move(srcAbsPath, destAbsPath);
        touch(destAbsPath);
    }

    void dispose() {
        if (workflowManager != null) {
            workflowManager.close();
        }
    }
}
