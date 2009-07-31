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
package org.hippoecm.repository.decorating.spi;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

import javax.jcr.InvalidItemStateException;
import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Workspace;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.EventListenerIterator;
import javax.jcr.observation.ObservationManager;

import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.DocumentManager;
import org.hippoecm.repository.api.HierarchyResolver;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowDescriptor;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.decorating.DecoratorFactory;
import org.hippoecm.repository.decorating.DocumentManagerDecorator;
import org.hippoecm.repository.decorating.WorkflowManagerDecorator;

public class WorkspaceDecorator extends org.hippoecm.repository.decorating.WorkspaceDecorator {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    static WeakHashMap<Session, Set<EventListenerRegistration>> listeners = new WeakHashMap<Session, Set<EventListenerRegistration>>();

    HippoSession remoteSession = null;
    HippoWorkspace remoteWorkspace = null;
    HierarchyResolver remoteHierarchyResolver = null;

    protected WorkspaceDecorator(DecoratorFactory factory, Session session, Workspace workspace) {
        super(factory, session, workspace);
        remoteSession = ((SessionDecorator)session).getRemoteSession();
        try {
            remoteWorkspace = (HippoWorkspace) remoteSession.getWorkspace();
            remoteHierarchyResolver = remoteWorkspace.getHierarchyResolver();
        } catch(RepositoryException ex) {
        }
    }

    public DocumentManager getDocumentManager() throws RepositoryException {
        return new DocumentManagerDecorator() {
                public Session getSession() {
                    return session;
                }
                public Document getDocument(String category, String identifier) throws RepositoryException {
                    return (Document) wrap(remoteWorkspace.getDocumentManager().getDocument(category, identifier));
                }
            };
    }

    public WorkflowManager getWorkflowManager() throws RepositoryException {
        return new WorkflowManagerDecorator(session) {
                public WorkflowDescriptor getWorkflowDescriptor(String category, Node item) throws RepositoryException {
                    WorkflowManager remoteWorkflowManager = remoteWorkspace.getWorkflowManager();
                    return remoteWorkflowManager.getWorkflowDescriptor(category, remoteSession.getRootNode().getNode(item.getUUID()));
                }
                public WorkflowDescriptor getWorkflowDescriptor(String category, Document document) throws RepositoryException {
                    WorkflowManager remoteWorkflowManager = remoteWorkspace.getWorkflowManager();
                    return remoteWorkflowManager.getWorkflowDescriptor(category, remoteSession.getRootNode().getNode(document.getIdentity()));
                }
                public Workflow getWorkflow(WorkflowDescriptor descriptor) throws RepositoryException {
                    WorkflowManager remoteWorkflowManager = remoteWorkspace.getWorkflowManager();
                    return (Workflow) wrap(remoteWorkflowManager.getWorkflow(descriptor));
                }
                public Workflow getWorkflow(String category, Node item) throws RepositoryException {
                    WorkflowManager remoteWorkflowManager = remoteWorkspace.getWorkflowManager();
                    return (Workflow) wrap(remoteWorkflowManager.getWorkflow(category, remoteSession.getRootNode().getNode(item.getPath().substring(1))));
                }
                public Workflow getWorkflow(String category, Document document) throws RepositoryException {
                    WorkflowManager remoteWorkflowManager = remoteWorkspace.getWorkflowManager();
                    return (Workflow) wrap(remoteWorkflowManager.getWorkflow(category, document));
                }

        };
    }

    @Override
    public ObservationManager getObservationManager() throws UnsupportedRepositoryOperationException,
            RepositoryException {
        final ObservationManager spiObMgr = super.getObservationManager();
        return new ObservationManager() {

            public void addEventListener(EventListener listener, int eventTypes, String absPath, boolean isDeep,
                    String[] uuid, String[] nodeTypeName, boolean noLocal) throws RepositoryException {
                if (!noLocal) {
                    EventListenerRegistration registration = new EventListenerRegistration(listener,
                            eventTypes, absPath, isDeep, uuid, nodeTypeName);
                    synchronized (listeners) {
                        if (!listeners.containsKey(session)) {
                            listeners.put(session, new HashSet<EventListenerRegistration>());
                        }
                        listeners.get(session).add(registration);
                    }
                }
                spiObMgr.addEventListener(listener, eventTypes, absPath, isDeep, uuid, nodeTypeName, noLocal);
            }

            public EventListenerIterator getRegisteredEventListeners() throws RepositoryException {
                return spiObMgr.getRegisteredEventListeners();
            }

            public void removeEventListener(EventListener listener) throws RepositoryException {
                synchronized (listeners) {
                    Set<EventListenerRegistration> registered = listeners.get(session);
                    if (registered != null) {
                        for (Iterator<EventListenerRegistration> iter = registered.iterator(); iter.hasNext(); ) {
                            EventListenerRegistration registration = iter.next();
                            if (registration.listener == listener) {
                                iter.remove();
                                if (registered.size() == 0) {
                                    listeners.remove(session);
                                }
                                break;
                            }
                        }
                    }
                }
                spiObMgr.removeEventListener(listener);
            }
            
        };
    }
    
    
    public HierarchyResolver getHierarchyResolver() throws RepositoryException {
        return new HierarchyResolver() {
                public Item getItem(Node ancestor, String path, boolean isProperty, Entry last) throws InvalidItemStateException, RepositoryException {
                    Item item = remoteHierarchyResolver.getItem(ancestor, path, isProperty, last);
                    if(item.isNode()) {
                        item = session.getRootNode().getNode(item.getPath().substring(1));
                    } else {
                        item = session.getRootNode().getProperty(item.getPath().substring(1));
                    }
                    return factory.getItemDecorator(session, item);
                }

                public Item getItem(Node ancestor, String path) throws InvalidItemStateException, RepositoryException {
                    Item item = remoteHierarchyResolver.getItem(ancestor, path);
                    if(item.isNode()) {
                        item = session.getRootNode().getNode(item.getPath().substring(1));
                    } else {
                        item = session.getRootNode().getProperty(item.getPath().substring(1));
                    }
                    return factory.getItemDecorator(session, item);
                }

                public Property getProperty(Node node, String field) throws RepositoryException {
                    Property property = remoteHierarchyResolver.getProperty(node, field);
                    property = session.getRootNode().getProperty(property.getPath().substring(1));
                    return factory.getPropertyDecorator(session, property);
                }

                public Property getProperty(Node node, String field, Entry last) throws RepositoryException {
                    Property property = remoteHierarchyResolver.getProperty(node, field, last);
                    property = session.getRootNode().getProperty(property.getPath().substring(1));
                    return factory.getPropertyDecorator(session, property);
                }

                public Node getNode(Node node, String field) throws InvalidItemStateException, RepositoryException {
                    node = remoteHierarchyResolver.getNode(node, field);
                    if(node == null) {
                        return null;
                    }
                    node = session.getRootNode().getNode(node.getPath().substring(1));
                    return factory.getNodeDecorator(session, node);
                }
            };
    }

    // FIXME: fragale code, reference (although weak) to all other sessions
    private Object wrap(final Object object) throws RepositoryException {
        if (object == null) {
            return null;
        }
        Class[] interfaces = object.getClass().getInterfaces();
        InvocationHandler handler = new InvocationHandler() {

            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                ObservationManager obMgr = remoteWorkspace.getObservationManager();
                List<EventListenerRegistration> roList = new ArrayList<EventListenerRegistration>();
                synchronized (listeners) {
                    if (listeners.containsKey(session)) {
                        roList.addAll(listeners.get(session));
                    }
                }

                List<EventListener> registered = new ArrayList<EventListener>(roList.size());
                for (EventListenerRegistration registration : roList) {
                    try {
                        obMgr.addEventListener(registration.listener, registration.eventTypes,
                                registration.absPath, registration.isDeep, registration.uuid,
                                registration.nodeTypeName, false);
                        registered.add(registration.listener);
                    } catch (RepositoryException ex) {
                        ex.printStackTrace();
                    }
                }

                Object result;
                try {
                    result = method.invoke(object, args);
                } catch(InvocationTargetException ex) {
                    if(ex.getCause() != null) {
                        throw ex.getCause();
                    } else {
                        throw ex;
                    }
                } finally {
                    for (EventListener listener : registered) {
                        try {
                            obMgr.removeEventListener(listener);
                        } catch (RepositoryException ex) {
                            ex.printStackTrace();
                        }
                    }
                }
                return result;
            }
            
        };
        Class proxyClass = Proxy.getProxyClass(object.getClass().getClassLoader(), interfaces);
        try {
            return proxyClass.getConstructor(new Class[] {InvocationHandler.class}).
                    newInstance(new Object[] {handler});
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            throw new RepositoryException(e);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            throw new RepositoryException(e);
        } catch (SecurityException e) {
            e.printStackTrace();
            throw new RepositoryException(e);
        } catch (InstantiationException e) {
            e.printStackTrace();
            throw new RepositoryException(e);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            throw new RepositoryException(e);
        } catch (InvocationTargetException e) {
            e.printStackTrace();
            throw new RepositoryException(e);
        }
    }

    class EventListenerRegistration {
        EventListener listener;
        int eventTypes;
        String absPath;
        boolean isDeep;
        String[] uuid;
        String[] nodeTypeName;

        public EventListenerRegistration(EventListener listener, int eventTypes, String absPath, boolean isDeep,
                String[] uuid, String[] nodeTypeName) {
            this.listener = listener;
            this.eventTypes = eventTypes;
            this.absPath = absPath;
            this.isDeep = isDeep;
            this.uuid = uuid;
            this.nodeTypeName = nodeTypeName;
        }
    }
}
