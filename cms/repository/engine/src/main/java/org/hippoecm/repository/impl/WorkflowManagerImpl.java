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

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.security.AccessControlException;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Vector;

import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.version.VersionException;

import javax.jdo.JDOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.hippoecm.repository.RepositoryMapImpl;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.RepositoryMap;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowContext;
import org.hippoecm.repository.api.WorkflowDescriptor;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.ext.InternalWorkflow;
import org.hippoecm.repository.ext.WorkflowImpl;
import org.hippoecm.repository.ext.WorkflowInvocation;
import org.hippoecm.repository.ext.WorkflowModule;
import org.hippoecm.repository.quartz.SchedulerWorkflowModule;
import org.hippoecm.repository.standardworkflow.EventLoggerImpl;

public class WorkflowManagerImpl implements WorkflowManager {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    final static Logger log = LoggerFactory.getLogger(WorkflowManagerImpl.class);

    /** Session from which this WorkflowManager instance was created.  Is used
     * to look-up which workflows are active for a user.  It is however not
     * used to instantiate workflows, persist and as execution context when
     * performing a workflow step (i.e. method invocatin).
     */
    Session session;
    Session rootSession;
    String configuration;
    List<WorkflowInvocation> invocationChain;
    ListIterator<WorkflowInvocation> invocationIndex;
    DocumentManagerImpl documentManager;

    public WorkflowManagerImpl(Session session, Session rootSession) {
        this.session = session;
        this.rootSession = rootSession;
        documentManager = new DocumentManagerImpl(rootSession);
        try {
            configuration = session.getRootNode().getNode(HippoNodeType.CONFIGURATION_PATH+"/"+
                    HippoNodeType.WORKFLOWS_PATH).getUUID();
        } catch (PathNotFoundException ex) {
            log.info("No workflow configuration found. Workflow not started.");
        } catch (RepositoryException ex) {
            log.error("workflow manager configuration failed: "+ex.getMessage(), ex);
        }
    }

    public Session getSession() throws RepositoryException {
        return session;
    }

    private Node getWorkflowNode(String category, Node item, Session session) {
        if (configuration==null) {
            return null;
        }
        try {
            if (log.isDebugEnabled()) {
                log.debug("looking for workflow in category "+category+" for node "+(item==null ? "<none>" : item.getPath()));
            }

            // if the user session has not yet been saved, no workflow is possible
            // as the root session will not be able to find it.  (ItemNotFoundException)
            if (!item.isNodeType("mix:referenceable") && !item.isNodeType("rep:root")) {
                log.debug("no workflow for node because node is not mix:referenceable");
                return null;
            }
            if(!item.isNodeType("rep:root")) {
                rootSession.getNodeByUUID(item.getUUID());
            }

            Node node = session.getNodeByUUID(configuration);
            if (node.hasNode(category)) {
                node = node.getNode(category);
                Node workflowNode = null;
                for (NodeIterator iter = node.getNodes(); iter.hasNext();) {
                    workflowNode = iter.nextNode();
                    if (workflowNode==null) {
                        continue;
                    }
                    if (log.isDebugEnabled()) {
                        StringBuffer sb = new StringBuffer();
                        sb.append(item.getPrimaryNodeType().getName());
                        NodeType[] nodeTypes = item.getMixinNodeTypes();
                        for(int i=0; i<nodeTypes.length; i++) {
                            sb.append(", ");
                            sb.append(nodeTypes[i].getName());
                        }
                        log.debug("matching item of types "+new String(sb)+" against " +
                                  workflowNode.getProperty(HippoNodeType.HIPPOSYS_NODETYPE).getString());
                    }
                    if (item.isNodeType(workflowNode.getProperty(HippoNodeType.HIPPOSYS_NODETYPE).getString())) {
                        boolean hasPermission = true;
                        if(workflowNode.hasProperty(HippoNodeType.HIPPO_PRIVILEGES)) {
                            Value[] privileges = workflowNode.getProperty(HippoNodeType.HIPPO_PRIVILEGES).getValues();
                            for(int i=0; i<privileges.length; i++) {
                                try {
                                    item.getSession().checkPermission(item.getPath(), privileges[i].getString());
                                } catch(AccessControlException ex) {
                                    log.debug("item matches but no permission on "+item.getPath()+" for role "+privileges[i].getString());
                                    hasPermission = false;
                                    break;
                                } catch(AccessDeniedException ex) {
                                    log.debug("item matches but no permission on "+item.getPath()+" for role "+privileges[i].getString());
                                    hasPermission = false;
                                    break;
                                }
                            }
                        }
                        if(hasPermission) {
                            if(log.isDebugEnabled()) {
                                log.debug("found workflow in category " + category + " for node " +
                                          (item==null ? "<none>" : item.getPath()));
                            }
                            return workflowNode;
                        }
                    }
                }
            } else {
                log.debug("workflow in category "+category+" for node "+(item==null ? "<none>" : item.getPath())+" not found");
            }
        } catch (ItemNotFoundException ex) {
            log.error("workflow category does not exist or workflows definition missing "+ex.getMessage());
        } catch (PathNotFoundException ex) {
            log.error("workflow category does not exist or workflows definition missing "+ex.getMessage());
        } catch (ValueFormatException ex) {
            log.error("misconfiguration of workflow definition");
        } catch (RepositoryException ex) {
            log.error("generic error accessing workflow definitions "+ex.getClass().getName()+": "+ex.getMessage(), ex);
        }
        return null;
    }

    private Node getWorkflowNode(String category, Document document, Session session) {
        if (configuration==null) {
            return null;
        }
        if (document==null) {
            log.error("cannot retrieve workflow for non-existing document");
            return null;
        }
        if (log.isDebugEnabled()) {
            log.debug("looking for workflow in category "+category+" for document "+document.getIdentity());
        }
        try {
            Node node = session.getNodeByUUID(configuration);
            if (node.hasNode(category)) {
                node = node.getNode(category);
                Node workflowNode = null;
                for (NodeIterator iter = node.getNodes(); iter.hasNext();) {
                    workflowNode = iter.nextNode();
                    if (workflowNode==null) {
                        continue;
                    }
                    if (log.isDebugEnabled()) {
                        log.debug("matching document type against " +
                                workflowNode.getProperty(HippoNodeType.HIPPO_CLASSNAME).getString());
                    }
                    try {
                        Class documentClass = Class.forName(workflowNode.getProperty(HippoNodeType.HIPPO_CLASSNAME).getString());
                        Node documentNode = (document.getIdentity()!=null ? session.getNodeByUUID(document.getIdentity()) : null);
                        if ((documentNode != null && documentNode.isNodeType(workflowNode.getProperty(HippoNodeType.HIPPOSYS_NODETYPE).getString())) || documentClass.isAssignableFrom(document.getClass())) {
                            boolean hasPermission = true;
                            if(workflowNode.hasProperty(HippoNodeType.HIPPO_PRIVILEGES)) {
                                Value[] privileges = workflowNode.getProperty(HippoNodeType.HIPPO_PRIVILEGES).getValues();
                                for(int i=0; i<privileges.length; i++) {
                                    try {
                                        session.checkPermission(documentNode.getPath(), privileges[i].getString());
                                    } catch(AccessControlException ex) {
                                        log.debug("item matches but no permission on "+documentNode.getPath()+" for role "+privileges[i].getString());
                                        hasPermission = false;
                                        break;
                                    } catch(AccessDeniedException ex) {
                                        log.debug("item matches but no permission on "+documentNode.getPath()+" for role "+privileges[i].getString());
                                        hasPermission = false;
                                        break;
                                    }
                                }
                            }
                            if(hasPermission) {
                                if(log.isDebugEnabled()) {
                                    log.debug("found workflow in category "+category+" for document");
                                }
                                return workflowNode;
                            }
                        }
                    } catch (ClassNotFoundException ex) {
                    }
                }
            } else {
                log.debug("workflow in category "+category+" for document not found");
            }
        } catch (ItemNotFoundException ex) {
            log.error("workflow category does not exist or workflows definition missing "+ex.getMessage());
        } catch (PathNotFoundException ex) {
            log.error("workflow category does not exist or workflows definition missing "+ex.getMessage());
        } catch (ValueFormatException ex) {
            log.error("misconfiguration of workflow definition");
        } catch (RepositoryException ex) {
            log.error("generic error accessing workflow definitions "+ex.getMessage());
        }
        return null;
    }

    public WorkflowDescriptor getWorkflowDescriptor(String category, Node item) throws RepositoryException {
        Node workflowNode = getWorkflowNode(category, item, session);
        if (workflowNode!=null) {
            return new WorkflowDescriptorImpl(this, category, workflowNode, item);
        }
        log.debug("Workflow for category "+category+" on "+item.getPath()+" is not available");
        return null;
    }

    public WorkflowDescriptor getWorkflowDescriptor(String category, Document document) throws RepositoryException {
        Node workflowNode = getWorkflowNode(category, document, session);
        if (workflowNode!=null) {
            return new WorkflowDescriptorImpl(this, category, workflowNode, document);
        }
        log.debug("Workflow for category "+category+" on "+document.getIdentity()+" is not available");
        return null;
    }

    public Workflow getWorkflow(WorkflowDescriptor descriptor) throws RepositoryException {
        WorkflowDescriptorImpl descriptorImpl = (WorkflowDescriptorImpl)descriptor;
        try {
            Node node = session.getNodeByUUID(descriptorImpl.uuid);
            return getWorkflow(descriptorImpl.category, node);
        } catch (PathNotFoundException ex) {
            log.debug("Workflow no longer available "+descriptorImpl.uuid);
            return null;
        }
    }

    public Workflow getWorkflow(String category, Node item) throws RepositoryException {
        Node workflowNode = getWorkflowNode(category, item, session);
        if (workflowNode!=null) {
            try {
                String classname = workflowNode.getProperty(HippoNodeType.HIPPO_CLASSNAME).getString();
                Node types = workflowNode.getNode(HippoNodeType.HIPPO_TYPES);

                String uuid = null;
                /* The synchronized must operate on the core root session, because there is
                 * only one such session, while there may be many decorated ones.
                 */
                synchronized (SessionDecorator.unwrap(rootSession)) {
                    documentManager.reset();
                    Workflow workflow = null; // compiler does not detect properly there is no path where this not set
                    Class clazz = Class.forName(classname);
                    if (InternalWorkflow.class.isAssignableFrom(clazz)) {
                        try {
                            Constructor[] constructors = clazz.getConstructors();
                            int constructorIndex;
                            for (constructorIndex=0; constructorIndex<constructors.length; constructorIndex++) {
                                Class[] params = constructors[constructorIndex].getParameterTypes();
                                if(params.length == 4 && WorkflowContext.class.isAssignableFrom(params[0])
                                                      && Session.class.isAssignableFrom(params[1])
                                                      && Session.class.isAssignableFrom(params[2])
                                                      && Node.class.isAssignableFrom(params[3])) {
                                    workflow = (Workflow)constructors[constructorIndex].newInstance(new Object[] {
                                            new WorkflowContextNodeImpl(workflowNode, getSession(), item), getSession(), rootSession, item });
                                    break;
                                } else if(params.length == 3 && Session.class.isAssignableFrom(params[0])
                                                             && Session.class.isAssignableFrom(params[1])
                                                             && Node.class.isAssignableFrom(params[2])) {
                                    workflow = (Workflow)constructors[constructorIndex].newInstance(new Object[] {
                                            getSession(), rootSession, item });
                                    break;
                                }
                            }
                            if(constructorIndex == constructors.length) {
                                throw new RepositoryException("no valid constructor found in standards plugin");
                            }
                        } catch (IllegalAccessException ex) {
                            log.debug("no access to standards plugin", ex);
                            throw new RepositoryException("no access to standards plugin", ex);
                        } catch (InstantiationException ex) {
                            log.debug("standards plugin invalid", ex);
                            throw new RepositoryException("standards plugin invalid", ex);
                        } catch (InvocationTargetException ex) {
                            log.debug("standards plugin invalid", ex);
                            throw new RepositoryException("standards plugin invalid", ex);
                        }
                    } else {
                        uuid = item.getUUID();
                        Object object = documentManager.getObject(uuid, classname, types);
                        workflow = (Workflow)object;
                        if (workflow instanceof WorkflowImpl) {
                            ((WorkflowImpl)workflow).setWorkflowContext(new WorkflowContextNodeImpl(workflowNode, getSession(), item));
                        }
                    }

                    try {
                        Class[] interfaces = workflow.getClass().getInterfaces();
                        Vector vector = new Vector();
                        for (int i = 0; i<interfaces.length; i++) {
                            if (Remote.class.isAssignableFrom(interfaces[i])) {
                                vector.add(interfaces[i]);
                            }
                        }
                        interfaces = (Class[])vector.toArray(new Class[vector.size()]);
                        InvocationHandler handler = new WorkflowInvocationHandler(workflow, uuid, types);
                        Class proxyClass = Proxy.getProxyClass(workflow.getClass().getClassLoader(), interfaces);
                        workflow = (Workflow)proxyClass.getConstructor(new Class[] {InvocationHandler.class}).
                                newInstance(new Object[] {handler});

                        /* note that the returned workflow object is no longer exported at this time, the
                         * remoting layer will take care of this now, because it has knowledge of the
                         * registry to use (the port number).
                         */
                    } catch (NoSuchMethodException ex) {
                        log.debug("Impossible situation creating workflow proxy", ex);
                        throw new RepositoryException("Impossible situation creating workflow proxy", ex);
                    } catch (InstantiationException ex) {
                        log.error("Unable to create proxy for workflow");
                        throw new RepositoryException("Unable to create proxy for workflow", ex);
                    } catch (IllegalAccessException ex) {
                        log.debug("Impossible situation creating workflow proxy", ex);
                        throw new RepositoryException("Impossible situation creating workflow proxy", ex);
                    } catch (InvocationTargetException ex) {
                        log.debug("Impossible situation creating workflow proxy", ex);
                        throw new RepositoryException("Impossible situation creating workflow proxy", ex);
                    }

                    return workflow;
                }
            } catch (ClassNotFoundException ex) {
                log.error("Workflow specified at "+workflowNode.getPath()+" not present");
                throw new RepositoryException("workflow not present", ex);
            } catch (PathNotFoundException ex) {
                log.error("Workflow specification corrupt on node "+workflowNode.getPath());
                throw new RepositoryException("workflow specification corrupt", ex);
            } catch (ValueFormatException ex) {
                log.error("Workflow specification corrupt on node "+workflowNode.getPath());
                throw new RepositoryException("workflow specification corrupt", ex);
            }
        }
        log.debug("Workflow for category "+category+" on "+item.getPath()+" is not available");
        return null;
    }

    public Workflow getWorkflow(String category, Document document) throws RepositoryException {
        return getWorkflow(category, session.getNodeByUUID(document.getIdentity()));
    }

    private Workflow getWorkflow(Node workflowNode, WorkflowChainHandler handler) throws RepositoryException, WorkflowException {
        try {
            /* The synchronized must operate on the core root session, because there is
             * only one such session, while there may be many decorated ones.
             */
            synchronized (SessionDecorator.unwrap(rootSession)) {
                Workflow workflow = null;

                String workflowClassName = workflowNode.getProperty(HippoNodeType.HIPPO_CLASSNAME).getString();
                try {
                    Class workflowClass = Class.forName(workflowClassName);
                    Class[] interfaces = workflowClass.getInterfaces();
                    Vector vector = new Vector();
                    for (int i = 0; i<interfaces.length; i++) {
                        if (Remote.class.isAssignableFrom(interfaces[i])) {
                            vector.add(interfaces[i]);
                        }
                    }
                    interfaces = (Class[])vector.toArray(new Class[vector.size()]);
                    Class proxyClass = Proxy.getProxyClass(workflowClass.getClassLoader(), interfaces);
                    workflow = (Workflow)proxyClass.getConstructor(new Class[] {InvocationHandler.class}).
                            newInstance(new Object[] {handler});

                } catch (ClassNotFoundException ex) {
                    log.debug("Unable to locate workflow class", ex);
                    throw new RepositoryException("Unable to locate workflow class", ex);
                } catch (NoSuchMethodException ex) {
                    log.debug("Impossible situation creating workflow proxy", ex);
                    throw new RepositoryException("Impossible situation creating workflow proxy", ex);
                } catch (InstantiationException ex) {
                    log.error("Unable to create proxy for workflow");
                    throw new RepositoryException("Unable to create proxy for workflow", ex);
                } catch (IllegalAccessException ex) {
                    log.debug("Impossible situation creating workflow proxy", ex);
                    throw new RepositoryException("Impossible situation creating workflow proxy", ex);
                } catch (InvocationTargetException ex) {
                    log.debug("Impossible situation creating workflow proxy", ex);
                    throw new RepositoryException("Impossible situation creating workflow proxy", ex);
                }

                return workflow;
            }
        } catch (PathNotFoundException ex) {
            log.error("Workflow specification corrupt on node "+workflowNode.getPath());
            throw new RepositoryException("workflow specification corrupt", ex);
        } catch (ValueFormatException ex) {
            log.error("Workflow specification corrupt on node "+workflowNode.getPath());
            throw new RepositoryException("workflow specification corrupt", ex);
        }
    }

    class WorkflowInvocationHandler implements InvocationHandler {
        Workflow upstream;
        String uuid;
        Node types;

        WorkflowInvocationHandler(Workflow upstream, String uuid, Node types) {
            this.upstream = upstream;
            this.uuid = uuid;
            this.types = types;
        }

        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            Method targetMethod = null;
            Object returnObject = null;
            Throwable returnException = null;

            invocationChain = new LinkedList<WorkflowInvocation>();
            invocationIndex = invocationChain.listIterator();

            try {
                String path = getPath(uuid);
                targetMethod = upstream.getClass().getMethod(method.getName(), method.getParameterTypes());
                synchronized (SessionDecorator.unwrap(rootSession)) {
                    returnObject = targetMethod.invoke(upstream, args);
                    if (uuid!=null) {
                        documentManager.putObject(uuid, types, upstream);
                        rootSession.save();
                    }
                    if (returnObject instanceof Document) {
                        returnObject = new Document(((Document)returnObject).getIdentity());
                    }
                    if(!targetMethod.getName().equals("hints")) {
                        EventLoggerImpl eventLogger = new EventLoggerImpl(rootSession);
                        eventLogger.logWorkflowStep(session.getUserID(), upstream.getClass().getName(),
                                                    targetMethod.getName(), args, returnObject, path);
                    }
                    while (!invocationChain.isEmpty()) {
                        WorkflowInvocationImpl current = (WorkflowInvocationImpl) invocationChain.remove(0);
                        invocationIndex = invocationChain.listIterator();
                        current.invoke(WorkflowManagerImpl.this);
                    }
                }

                return returnObject;
            } catch (RepositoryException ex) {
                rootSession.refresh(false);
                throw returnException = ex;
            } catch (RemoteException ex) {
                rootSession.refresh(false);
                throw returnException = ex;
            } catch (NoSuchMethodException ex) {
                rootSession.refresh(false);
                throw returnException = new RepositoryException("Impossible failure for workflow proxy", ex);
            } catch (JDOException ex) {
                rootSession.refresh(false);
                if(ex.getCause() != null)
                    throw returnException = ex.getCause();
                Throwable[] nestedExceptions = ex.getNestedExceptions();
                if(nestedExceptions != null) {
                    for(Throwable nestedException : nestedExceptions) {
                        if(nestedException instanceof RepositoryException) {
                            throw returnException = nestedException;
                        }
                    }
                }
                throw returnException = new RepositoryException("Internal failure for workflow proxy", ex);
            } catch (IllegalAccessException ex) {
                rootSession.refresh(false);
                throw returnException = new RepositoryException("Impossible failure for workflow proxy", ex);
            } catch (InvocationTargetException ex) {
                rootSession.refresh(false);
                log.info(ex.getClass().getName()+": "+ex.getMessage(), ex);
                throw returnException = ex.getCause();
            } finally {
                StringBuffer sb = new StringBuffer();
                sb.append("AUDIT workflow invocation ");
                sb.append(uuid);
                sb.append(".");
                sb.append(upstream!=null ? upstream.getClass().getName() : "<unknown>");
                sb.append(".");
                sb.append(method!=null ? method.getName() : "<unknown>");
                sb.append("(");
                if (args!=null) {
                    for (int i = 0; i<args.length; i++) {
                        if (i>0) {
                            sb.append(", ");
                        }
                        sb.append(args[i]!=null ? args[i].toString() : "null");
                    }
                }
                sb.append(") -> ");
                if (returnException!=null) {
                    sb.append(returnException.getClass().getName());
                } else if (returnObject!=null) {
                    sb.append(returnObject.toString());
                } else {
                    sb.append("<<null>>");
                }
                if(method!=null && method.getName().equals("hints")) {
                    if (log.isDebugEnabled()) {
                        log.debug(new String(sb));
                    }
                } else {
                    log.info(new String(sb));
                }
            }
        }

        String getPath(String uuid) {
            if (uuid==null||uuid.equals("")) {
                return null;
            }
            try {
                Node node = session.getNodeByUUID(uuid);
                return node.getPath();
            } catch (ItemNotFoundException e) {
                return null;
            } catch (RepositoryException e) {
                log.error(e.getMessage(), e);
                return null;
            }
        }
    }

    private abstract class WorkflowChainHandler implements InvocationHandler {
        protected Node workflowNode;
        WorkflowModule module;

        WorkflowChainHandler(Node workflowNode, WorkflowModule module) {
            this.workflowNode = workflowNode;
            this.module = module;
        }

        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            invoke(method, args);
            return null;
        }

        abstract void invoke(Method method, Object[] args) throws RepositoryException;

        public void submit(WorkflowManager workflowManager, WorkflowInvocationImpl invocation) {
            module.submit(workflowManager, invocation);
        }
    }

    public static class WorkflowInvocationImpl implements WorkflowInvocation {
        Node workflowNode;
        Document workflowSubject;
        Node workflowSubjectNode;
        Method method;
        Object[] arguments;
        String category = null;
        String methodName = null;
        Class[] parameterTypes = null;

        public WorkflowInvocationImpl() {
            workflowNode = null;
            workflowSubject = null;
            workflowSubjectNode = null;
            method = null;
            arguments = null;
            category = null;
            methodName = null;
            parameterTypes = null;
        }
        WorkflowInvocationImpl(Node workflowNode, Document workflowSubject, Method method, Object[] args)
          throws RepositoryException {
            this.workflowNode = workflowNode;
            this.workflowSubject = workflowSubject;
            this.method = method;
            this.arguments = (args!=null ? args.clone() : args);
            this.workflowSubjectNode = null;
            this.category = workflowNode.getParent().getName();
            this.methodName = method.getName();
            this.parameterTypes = method.getParameterTypes();
        }

        WorkflowInvocationImpl(Node workflowNode, Node workflowSubject, Method method, Object[] args)
          throws RepositoryException {
            this.workflowNode = workflowNode;
            this.workflowSubject = null;
            this.method = method;
            this.arguments = (args!=null ? args.clone() : args);
            this.workflowSubjectNode = workflowSubject;
            this.category = workflowNode.getParent().getName();
            this.methodName = method.getName();
            this.parameterTypes = method.getParameterTypes();
        }

        public void readExternal(ObjectInput input) throws IOException, ClassNotFoundException {
            category = (String) input.readObject();
            String uuid = (String) input.readObject();
            workflowSubject = new Document(uuid);
            String className = (String) input.readObject();
            methodName = (String) input.readObject();
            int length = input.readInt();
            parameterTypes = new Class[length];
            for(int i=0; i<length; i++) {
                parameterTypes[i] = Class.forName((String)input.readObject());
            }
            arguments = (Object[]) input.readObject();
        }

        public void writeExternal(ObjectOutput output) throws IOException {
            try {
                output.writeObject(workflowNode.getParent().getName());
                output.writeObject(workflowSubjectNode.getUUID());
                output.writeObject(method.getClass().getName());
                output.writeObject(method.getName());
                Class[] parameterTypes = method.getParameterTypes();
                output.writeInt(parameterTypes.length);
                for(int i=0; i<parameterTypes.length; i++) {
                    output.writeObject(parameterTypes[i]);
                }
                output.writeObject(arguments);
            } catch(RepositoryException ex) {
                log.debug("not serializable", ex);
                throw new IOException("not serializable");
            }
        }

        public Node getSubject() {
            return workflowSubjectNode;
        }

        public void setSubject(Node node) {
            workflowSubjectNode = node;
            workflowSubject = null;
        }

        public Object invoke(Session session) throws RepositoryException, WorkflowException {
            workflowSubjectNode = session.getNodeByUUID(workflowSubjectNode.getUUID());
            WorkflowManager workflowManager = new WorkflowManagerImpl(session, session);
            Workflow workflow = workflowManager.getWorkflow(category, workflowSubjectNode);
            Method[] methods = workflow.getClass().getMethods();
            method = null;
            for(int methodIndex=0; methodIndex<methods.length && method == null; ++methodIndex) {
                if(!methods[methodIndex].getName().equals(methodName))
                    continue;
                Class[] types = methods[methodIndex].getParameterTypes();
                if(types.length != parameterTypes.length)
                    continue;
                method = methods[methodIndex];
                for(int i=0; i<types.length; i++) {
                    if(types[i] != parameterTypes[i]) {
                        method = null;
                        break;
                    }
                }
            }
            try {
                return method.invoke(workflow, arguments);
            } catch(IllegalAccessException ex) {
                log.debug(ex.getMessage(), ex);
                throw new RepositoryException(ex.getMessage(), ex);
            } catch(InvocationTargetException ex) {
                Throwable cause = ex.getCause();
                if (cause instanceof WorkflowException) {
                    throw (WorkflowException) cause;
                } else if (cause instanceof MappingException) {
                    throw (MappingException) cause;
                } else if (cause instanceof RepositoryException) {
                    throw (RepositoryException) cause;
                } else if (cause != null) {
                    log.debug(cause.getMessage(), cause);
                    throw new RepositoryException(cause.getMessage(), cause);
                } else {
                    log.debug(ex.getMessage(), ex);
                    throw new RepositoryException(ex.getMessage(), ex);
                }
            }
        }

        Object invoke(WorkflowManagerImpl manager) throws RepositoryException, WorkflowException {
            try {
                Workflow workflow = null; // compiler does not detect properly there is no path where this not set
                String classname = workflowNode.getProperty(HippoNodeType.HIPPO_CLASSNAME).getString();
                Node types = workflowNode.getNode(HippoNodeType.HIPPO_TYPES);
                String uuid = null;

                Node item = workflowSubjectNode;
                if (item == null) {
                    item = manager.rootSession.getNodeByUUID(workflowSubject.getIdentity());
                }
                try {
                    Class clazz = Class.forName(classname);
                    if (InternalWorkflow.class.isAssignableFrom(clazz)) {
                        Constructor[] constructors = clazz.getConstructors();
                        int constructorIndex;
                        for (constructorIndex = 0; constructorIndex < constructors.length; constructorIndex++) {
                            Class[] params = constructors[constructorIndex].getParameterTypes();
                            if (params.length == 4 && WorkflowContext.class.isAssignableFrom(params[0]) && Session.class.isAssignableFrom(params[1]) && Session.class.isAssignableFrom(params[2]) && Node.class.isAssignableFrom(params[3])) {
                                workflow = (Workflow)constructors[constructorIndex].newInstance(new Object[] {
                                            manager.new WorkflowContextNodeImpl(workflowNode, item.getSession(), item), manager.getSession(), manager.rootSession, item
                                        });
                                break;
                            } else if (params.length == 3 && Session.class.isAssignableFrom(params[0]) && Session.class.isAssignableFrom(params[1]) && Node.class.isAssignableFrom(params[2])) {
                                workflow = (Workflow)constructors[constructorIndex].newInstance(new Object[] {
                                            manager.getSession(), manager.rootSession, item
                                        });
                                break;
                            }
                        }
                        if (constructorIndex == constructors.length) {
                            throw new RepositoryException("no valid constructor found in standards plugin");
                        }
                    } else {
                        uuid = item.getUUID();
                        Object object = manager.documentManager.getObject(uuid, classname, types);
                        workflow = (Workflow)object;
                        if (workflow instanceof WorkflowImpl) {
                            ((WorkflowImpl)workflow).setWorkflowContext(manager.new WorkflowContextNodeImpl(workflowNode, item.getSession(), item));
                        }
                    }
                } catch (IllegalAccessException ex) {
                    log.debug("no access to standards plugin", ex);
                    throw new RepositoryException("no access to standards plugin", ex);
                } catch (ClassNotFoundException ex) {
                    log.debug("standards plugin missing", ex);
                    throw new RepositoryException("standards plugin missing", ex);
                } catch (InstantiationException ex) {
                    log.debug("standards plugin invalid", ex);
                    throw new RepositoryException("standards plugin invalid", ex);
                } catch (InvocationTargetException ex) {
                    log.debug("standards plugin invalid", ex);
                    throw new RepositoryException("standards plugin invalid", ex);
                }

                Method targetMethod = workflow.getClass().getMethod(method.getName(), method.getParameterTypes());
                synchronized (SessionDecorator.unwrap(manager.rootSession)) {
                    Object returnObject = targetMethod.invoke(workflow, arguments);
                    if (uuid!=null) {
                        manager.documentManager.putObject(uuid, types, workflow);
                        manager.rootSession.save();
                    }
                    return returnObject;
                }
            } catch (InvocationTargetException ex) {
                Throwable cause = ex.getCause();
                if (cause instanceof WorkflowException) {
                    throw (WorkflowException) cause;
                } else if (cause instanceof MappingException) {
                    throw (MappingException) cause;
                } else if (cause instanceof RepositoryException) {
                    throw (RepositoryException) cause;
                } else if (cause != null) {
                    log.debug(cause.getMessage(), cause);
                    throw new RepositoryException(cause.getMessage(), cause);
                } else {
                    log.debug(ex.getMessage(), ex);
                    throw new RepositoryException(ex.getMessage(), ex);
                }
            } catch (NoSuchMethodException ex) {
                log.debug("standards plugin invalid", ex);
                throw new RepositoryException("standards plugin invalid", ex);
            } catch (IllegalAccessException ex) {
                log.debug("no access to standards plugin", ex);
                throw new RepositoryException("no access to standards plugin", ex);
            } catch (PathNotFoundException ex) {
                log.error("Workflow specification corrupt on node "+workflowNode.getPath());
                throw new RepositoryException("workflow specification corrupt", ex);
            } catch (ValueFormatException ex) {
                log.error("Workflow specification corrupt on node "+workflowNode.getPath());
                throw new RepositoryException("workflow specification corrupt", ex);
            } catch (NoSuchNodeTypeException ex) {
                log.debug(ex.getMessage(), ex);
                throw ex;
            } catch (LockException ex) {
                log.debug(ex.getMessage(), ex);
                throw ex;
            } catch (VersionException ex) {
                log.debug(ex.getMessage(), ex);
                throw ex;
            } catch (ConstraintViolationException ex) {
                log.debug(ex.getMessage(), ex);
                throw ex;
            } catch (AccessDeniedException ex) {
                log.debug(ex.getMessage(), ex);
                throw ex;
            } catch (ItemNotFoundException ex) {
                log.debug(ex.getMessage(), ex);
                throw ex;
            } catch (UnsupportedRepositoryOperationException ex) {
                log.debug(ex.getMessage(), ex);
                throw ex;
            } catch (ItemExistsException ex) {
                log.debug(ex.getMessage(), ex);
                throw ex;
            } catch (InvalidItemStateException ex) {
                log.debug(ex.getMessage(), ex);
                throw ex;
            }
        }
    }

    private abstract class WorkflowContextImpl implements WorkflowContext {
        Session subjectSession;
        Node workflowDefinition;
        WorkflowModule module;

        WorkflowContextImpl(Node workflowNode, Session subjectSession) {
            this(workflowNode, subjectSession, null);
        }

        WorkflowContextImpl(Node workflowNode, Session subjectSession, WorkflowModule module) {
            this.workflowDefinition = workflowNode;
            this.subjectSession = subjectSession;
            if(module == null) {
                this.module = new WorkflowModule() {
                        public void submit(WorkflowManager manager, WorkflowInvocation invocation) {
                            invocationIndex.add(invocation);
                        }
                    };
            } else {
                this.module = module;
            }
        }

        public WorkflowContext getWorkflowContext(Object specification) throws MappingException, RepositoryException {
            if(specification instanceof java.util.Date) {
                return newContext(workflowDefinition, subjectSession, new SchedulerWorkflowModule((java.util.Date)specification));
            } else if(specification instanceof Document) {
                String uuid = ((Document)specification).getIdentity();
                Node node = subjectSession.getNodeByUUID(uuid);
                return newContext(workflowDefinition, subjectSession, null);
            }
            log.debug("No context defined for class "+(specification!=null?specification.getClass().getName():"none"));
            throw new MappingException("No context defined for class "+(specification!=null?specification.getClass().getName():"none"));
        }

        public Document getDocument(String category, String identifier) throws RepositoryException {
            return documentManager.getDocument(category, identifier);
        }

        public Workflow getWorkflow(String category, final Document document) throws MappingException, WorkflowException, RepositoryException {
            Node workflowNode = WorkflowManagerImpl.this.getWorkflowNode(category, document, documentManager.getSession());
            if (workflowNode != null) {
                return WorkflowManagerImpl.this.getWorkflow(workflowNode,
                       new WorkflowChainHandler(workflowNode, module) {
                           @Override
                           public void invoke(Method method, Object[] args) throws RepositoryException {
                               module.submit(WorkflowManagerImpl.this, new WorkflowInvocationImpl(workflowNode, document, method, args));
                           }
                       });
            }
            log.debug("Workflow for category "+category+" on document is not available");
            throw new MappingException("Workflow for category "+category+" on document is not available");
        }

        protected abstract WorkflowContextImpl newContext(Node workflowDefinition, Session subjectSession, WorkflowModule specification);
        
        public abstract Workflow getWorkflow(String category) throws MappingException, WorkflowException, RepositoryException;

        public String getUserIdentity() {
            return session.getUserID();
        }

        public RepositoryMap getWorkflowConfiguration() {
            try {
                if(workflowDefinition.hasNode("hipposys:config")) {
                    return new RepositoryMapImpl(workflowDefinition.getNode("hipposys:config"));
                }
            } catch(RepositoryException ex) {
                try {
                    log.error("Cannot access configuration of workflow defined in "+workflowDefinition.getPath());
                } catch(RepositoryException e) {
                    log.error("Double access error accessing configuration of workflow");
                }
            }
            return new RepositoryMapImpl();
        }
    }

    private class WorkflowContextNodeImpl extends WorkflowContextImpl {
        Node subject;

        WorkflowContextNodeImpl(Node workflowNode, Session subjectSession, Node subject) {
            super(workflowNode, subjectSession);
            this.subject = subject;
        }

        WorkflowContextNodeImpl(Node workflowNode, Session subjectSession, Node subject, WorkflowModule module) {
            super(workflowNode, subjectSession, module);
            this.subject = subject;
        }

        public Workflow getWorkflow(String category) throws MappingException, WorkflowException, RepositoryException {
            Node workflowNode = WorkflowManagerImpl.this.getWorkflowNode(category, subject, documentManager.getSession());
            if (workflowNode != null) {
                return WorkflowManagerImpl.this.getWorkflow(workflowNode,
                       new WorkflowChainHandler(workflowNode, module) {
                           @Override
                           public void invoke(Method method, Object[] args) throws RepositoryException {
                               module.submit(WorkflowManagerImpl.this, new WorkflowInvocationImpl(workflowNode, subject, method, args));
                           }
                       });
            }
            log.debug("Workflow for category "+category+" on document is not available");
            throw new MappingException("Workflow for category "+category+" on document is not available");
        }
        
        protected WorkflowContextImpl newContext(Node workflowDefinition, Session subjectSession, WorkflowModule specification) {
            return new WorkflowContextNodeImpl(workflowDefinition, subjectSession, subject, specification);
        }
    }

    private class WorkflowContextDocumentImpl extends WorkflowContextImpl {
        Document subject;

        WorkflowContextDocumentImpl(Node workflowNode, Session subjectSession, Document subject) {
            super(workflowNode, subjectSession);
            this.subject = subject;
        }

        WorkflowContextDocumentImpl(Node workflowNode, Session subjectSession, Document subject, WorkflowModule module) {
            super(workflowNode, subjectSession, module);
            this.subject = subject;
        }

        public Workflow getWorkflow(String category) throws MappingException, WorkflowException, RepositoryException {
            Node workflowNode = WorkflowManagerImpl.this.getWorkflowNode(category, subject, documentManager.getSession());
            if (workflowNode != null) {
                return WorkflowManagerImpl.this.getWorkflow(workflowNode,
                       new WorkflowChainHandler(workflowNode, module) {
                           @Override
                           public void invoke(Method method, Object[] args) throws RepositoryException {
                               module.submit(WorkflowManagerImpl.this, new WorkflowInvocationImpl(workflowNode, subject, method, args));
                           }
                       });
            }
            log.debug("Workflow for category "+category+" on document is not available");
            throw new MappingException("Workflow for category "+category+" on document is not available");
        }
        
        protected WorkflowContextImpl newContext(Node workflowDefinition, Session subjectSession, WorkflowModule specification) {
            return new WorkflowContextDocumentImpl(workflowDefinition, subjectSession, subject, specification);
        }
    }
}
