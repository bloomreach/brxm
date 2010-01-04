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
package org.hippoecm.repository.impl;

import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.WeakHashMap;

import javax.jcr.AccessDeniedException;
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
import javax.jcr.observation.EventListener;
import javax.jcr.observation.EventListenerIterator;
import javax.jcr.observation.ObservationManager;
import javax.jcr.util.TraversingItemVisitor;
import javax.jcr.version.VersionException;

import org.apache.jackrabbit.core.observation.SynchronousEventListener;
import org.hippoecm.repository.HierarchyResolverImpl;
import org.hippoecm.repository.api.DocumentManager;
import org.hippoecm.repository.api.HierarchyResolver;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.decorating.DecoratorFactory;
import org.hippoecm.repository.jackrabbit.RepositoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple workspace decorator.
 */
public class WorkspaceDecorator extends org.hippoecm.repository.decorating.WorkspaceDecorator implements HippoWorkspace {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    protected final static Logger logger = LoggerFactory.getLogger(WorkspaceDecorator.class);

    static WeakHashMap<Session, Set<EventListenerDecorator>> listeners = new WeakHashMap<Session, Set<EventListenerDecorator>>();

    /** The underlying workspace instance. */
    protected final Workspace workspace;
    protected Session session;
    protected DocumentManager documentManager;
    protected WorkflowManager workflowManager;
    private SessionDecorator rootSession;

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
        documentManager = null;
        workflowManager = null;
        rootSession = null;
    }

    @Override
    protected void finalize() {
        if (rootSession != null) {
            if (rootSession.isLive()) {
                rootSession.logout();
            }
            rootSession = null;
        }
    }

    @Override
    public DocumentManager getDocumentManager() throws RepositoryException {
        if (documentManager == null) {
            documentManager = new DocumentManagerImpl(session);
        }
        return documentManager;
    }

    @Override
    public WorkflowManager getWorkflowManager() throws RepositoryException {
        if (rootSession == null) {
            Repository repository = RepositoryDecorator.unwrap(session.getRepository());
            try {
                if (repository instanceof RepositoryImpl) {
                    rootSession = (SessionDecorator) factory.getSessionDecorator(session.getRepository(), session
                            .impersonate(new SimpleCredentials("workflowuser", new char[] {}))); // FIXME: hardcoded workflowuser
                }
            } catch (LoginException ex) {
                logger.debug("User " + session.getUserID() + " is not allowed to impersonate to workflow session", ex);
                throw new AccessDeniedException("User " + session.getUserID()
                        + " is not allowed to obtain the workflow manager", ex);
            } catch (RepositoryException ex) {
                logger.error("Error while trying to obtain workflow session " + ex.getClass().getName() + ": "
                        + ex.getMessage(), ex);
                throw new RepositoryException("Error while trying to obtain workflow session", ex);
            }
        }

        if (workflowManager == null) {
            workflowManager = new WorkflowManagerImpl(session, rootSession);
        }

        return workflowManager;
    }

    @Override
    public HierarchyResolver getHierarchyResolver() throws RepositoryException {
        return new HierarchyResolverImpl();
    }

    @Override
    public ObservationManager getObservationManager() throws UnsupportedRepositoryOperationException,
            RepositoryException {
        final ObservationManager upstream = super.getObservationManager();
        return new ObservationManager() {

            public void addEventListener(EventListener listener, int eventTypes, String absPath, boolean isDeep,
                    String[] uuid, String[] nodeTypeName, boolean noLocal) throws RepositoryException {
                EventListenerDecorator decorator = new EventListenerDecorator(listener);
                synchronized (listeners) {
                    if (!listeners.containsKey(session)) {
                        listeners.put(session, new HashSet<EventListenerDecorator>());
                    }
                    listeners.get(session).add(decorator);
                }
                upstream.addEventListener(decorator, eventTypes, absPath, isDeep, uuid, nodeTypeName, noLocal);
            }

            public EventListenerIterator getRegisteredEventListeners() throws RepositoryException {
                // create local copy
                final Set<EventListenerDecorator> currentListeners = new HashSet<EventListenerDecorator>();
                synchronized (listeners) {
                    if (listeners.containsKey(session)) {
                        currentListeners.addAll(listeners.get(session));
                    }
                }

                return new EventListenerIterator() {
                    private final Iterator<EventListenerDecorator> listenerIterator = currentListeners.iterator();
                    private final long size = currentListeners.size();
                    private long pos = 0;

                    public EventListener nextEventListener() {
                        if (!hasNext()) {
                            throw new NoSuchElementException();
                        }
                        pos++;
                        return listenerIterator.next().listener;
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
                synchronized (listeners) {
                    Set<EventListenerDecorator> registered = listeners.get(session);
                    if (registered != null) {
                        for (Iterator<EventListenerDecorator> iter = registered.iterator(); iter.hasNext();) {
                            EventListenerDecorator decorator = iter.next();
                            if (decorator.listener == listener) {
                                iter.remove();
                                upstream.removeEventListener(decorator);
                                if (registered.size() == 0) {
                                    listeners.remove(session);
                                }
                                break;
                            }
                        }
                    }
                }
            }

        };
    }

    static class EventListenerDecorator implements SynchronousEventListener {
        EventListener listener;

        public EventListenerDecorator(EventListener listener) {
            this.listener = listener;
        }

        public void onEvent(EventIterator events) {
            listener.onEvent(events);
        }
    }

    private void touch(String destAbsPath) throws RepositoryException {
        Node destination = null, parent = getSession().getRootNode().getNode(
                destAbsPath.substring(1, destAbsPath.lastIndexOf("/")));
        for (NodeIterator iter = parent.getNodes(destAbsPath.substring(destAbsPath.lastIndexOf("/") + 1)); iter
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
}
