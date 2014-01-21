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
package org.hippoecm.repository.impl;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.rmi.Remote;
import java.security.AccessControlException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;

import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.version.VersionException;

import org.hippoecm.repository.Modules;
import org.hippoecm.repository.RepositoryMapImpl;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoWorkspace;
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
import org.hippoecm.repository.ext.WorkflowInvocationHandlerModule;
import org.hippoecm.repository.ext.WorkflowInvocationHandlerModuleFactory;
import org.hippoecm.repository.ext.WorkflowManagerModule;
import org.hippoecm.repository.ext.WorkflowManagerRegister;
import org.hippoecm.repository.standardworkflow.WorkflowEventLoggerWorkflow;
import org.hippoecm.repository.standardworkflow.WorkflowEventLoggerWorkflowImpl;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.NodeIterable;
import org.onehippo.repository.api.annotation.WorkflowAction;
import org.onehippo.repository.util.AnnotationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.repository.util.RepoUtils.PRIMITIVE_TO_OBJECT_TYPES;

/** This class is not part of a public accessible API or extensible interface */
public class WorkflowManagerImpl implements WorkflowManager {

    static final Logger log = LoggerFactory.getLogger(WorkflowManagerImpl.class);

    /** Session from which this WorkflowManager instance was created.  Is used
     * to look up which workflows are active for a user.  It is however not
     * used to instantiate workflows, persist and as execution context when
     * performing a workflow step (i.e. method invocation).
     */
    Session session;
    Session rootSession;
    String configuration;
    List<WorkflowInvocation> invocationChain;
    ListIterator<WorkflowInvocation> invocationIndex;
    WorkflowEventLoggerWorkflow eventLoggerWorkflow;
    private static final ThreadLocal<String> INTERACTION_ID = new ThreadLocal<String>();
    private static final ThreadLocal<String> INTERACTION = new ThreadLocal<String>();

    public WorkflowManagerImpl(Session session, Session rootSession) {
        this.session = session;
        this.rootSession = rootSession;
        try {
            configuration = session.getRootNode().getNode(HippoNodeType.CONFIGURATION_PATH+"/"+
                    HippoNodeType.WORKFLOWS_PATH).getIdentifier();
            if (session.nodeExists("/hippo:log")) {
                final Node logFolder = session.getNode("/hippo:log");
                final Node worlflowNode = getWorkflowNode("internal", logFolder);
                Workflow workflow = getRealWorkflow(logFolder, worlflowNode);
                if (workflow instanceof WorkflowEventLoggerWorkflow) {
                    eventLoggerWorkflow = (WorkflowEventLoggerWorkflow) workflow;
                }
            }
            if (eventLoggerWorkflow == null) {
                eventLoggerWorkflow = new WorkflowEventLoggerWorkflowImpl(rootSession);
            }
        } catch (PathNotFoundException ex) {
            log.info("No workflow configuration found. Workflow not started.");
        } catch (RepositoryException ex) {
            log.error("Workflow manager configuration failed: "+ex.getMessage(), ex);
        } catch (WorkflowException e) {
            log.error("Failed to create workflow logger: " + e.getMessage(), e);
        }
    }

    public Session getSession() throws RepositoryException {
        return session;
    }

    Node getWorkflowNode(String category, Node item) {
        if (configuration == null) {
            return null;
        }
        if (item == null) {
            log.error("Cannot retrieve workflow for null node");
            return null;
        }
        try {
            log.debug("Looking for workflow in category {} for node {}", category, item.getPath());

            Node node = JcrUtils.getNodeIfExists(rootSession.getNodeByIdentifier(configuration), category);
            if (node != null) {
                for (Node workflowNode : new NodeIterable(node.getNodes())) {

                    if (!workflowNode.isNodeType(HippoNodeType.NT_WORKFLOW)) {
                        continue;
                    }

                    final String nodeTypeName = workflowNode.getProperty(HippoNodeType.HIPPOSYS_NODETYPE).getString();
                    log.debug("Matching item against {}", nodeTypeName);

                    if (item.isNodeType(nodeTypeName)) {
                        if (checkWorkflowPermission(item, workflowNode)) {
                            log.debug("Found workflow in category {} for node {}", category, item.getPath());
                            return workflowNode;
                        }
                    }
                }
            } else {
                log.debug("Workflow in category {} for node {} not found", category, item.getPath());
            }
        } catch (ItemNotFoundException e) {
            log.error("Workflow category does not exist or workflows definition missing {}", e.getMessage());
        } catch (RepositoryException e) {
            log.error("Generic error accessing workflow definitions {}", e.getMessage(), e);
        }
        return null;
    }

    Node getWorkflowNode(String category, Document document) {
        if (configuration == null) {
            return null;
        }
        if (document == null || document.getIdentity() == null) {
            log.error("Cannot retrieve workflow for null document");
            return null;
        }
        try {
            log.debug("Looking for workflow in category {} for document {}", category, document.getIdentity());

            Node node = JcrUtils.getNodeIfExists(rootSession.getNodeByIdentifier(configuration), category);
            if (node != null) {
                for (Node workflowNode : new NodeIterable(node.getNodes())) {

                    if (!workflowNode.isNodeType(HippoNodeType.NT_WORKFLOW)) {
                        continue;
                    }

                    final String className = workflowNode.getProperty(HippoNodeType.HIPPO_CLASSNAME).getString();
                    final String nodeTypeName = workflowNode.getProperty(HippoNodeType.HIPPOSYS_NODETYPE).getString();

                    log.debug("Matching document type against node type {} or class name {}", nodeTypeName, className);

                    try {
                        Class documentClass = Class.forName(className);
                        Node documentNode = rootSession.getNodeByIdentifier(document.getIdentity());
                        if (documentNode.isNodeType(nodeTypeName) || documentClass.isAssignableFrom(document.getClass())) {
                            if(checkWorkflowPermission(documentNode, workflowNode)) {
                                log.debug("Found workflow in category {} for document {}", category, documentNode.getPath());
                                return workflowNode;
                            }
                        }
                    } catch (ClassNotFoundException ignored) {
                    }
                }
            } else {
                log.debug("Workflow in category {} for document not found", category);
            }
        } catch (ItemNotFoundException e) {
            log.error("Workflow category does not exist or workflows definition missing {}", e.getMessage());
        } catch (RepositoryException e) {
            log.error("Generic error accessing workflow definitions {}", e.getMessage(), e);
        }
        return null;
    }

    private boolean checkWorkflowPermission(final Node item, final Node workflowNode) throws RepositoryException {
        boolean hasPermission = true;
        final Property privileges = JcrUtils.getPropertyIfExists(workflowNode, HippoNodeType.HIPPO_PRIVILEGES);
        if (privileges != null) {
            for (final Value privilege : privileges.getValues()) {
                try {
                    item.getSession().checkPermission(item.getPath(), privilege.getString());
                } catch (AccessControlException e) {
                    log.debug("Item matches but no permission on {} for role {}", item.getPath(), privilege.getString());
                    hasPermission = false;
                    break;
                } catch (AccessDeniedException e) {
                    log.debug("Item matches but no permission on {} for role {}", item.getPath(), privilege.getString());
                    hasPermission = false;
                    break;
                } catch (IllegalArgumentException ex) {
                    /* Still haspermission is true because the underlying repository does not recognized the
                     * permission requested.  This is a fallback for a mis-configured are more bare repository
                     * implementation.
                     */
                }
            }
        }
        return hasPermission;
    }

    public WorkflowDescriptor getWorkflowDescriptor(String category, Node item) throws RepositoryException {
        Node workflowNode = getWorkflowNode(category, item);
        if (workflowNode!=null) {
            return new WorkflowDescriptorImpl(this, category, workflowNode, item);
        }
        log.debug("Workflow for category "+category+" on "+item.getPath()+" is not available");
        return null;
    }

    public WorkflowDescriptor getWorkflowDescriptor(String category, Document document) throws RepositoryException {
        Node workflowNode = getWorkflowNode(category, document);
        if (workflowNode!=null) {
            return new WorkflowDescriptorImpl(this, category, workflowNode, document);
        }
        log.debug("Workflow for category "+category+" on "+document.getIdentity()+" is not available");
        return null;
    }

    private Workflow getRealWorkflow(Node item, Node workflowNode) throws RepositoryException {
        if (workflowNode == null) {
            return null;
        }
        String classname = workflowNode.getProperty(HippoNodeType.HIPPO_CLASSNAME).getString();
        Class clazz;
        try {
            clazz = Class.forName(classname);
        } catch (ClassNotFoundException e) {
            final String message = "Workflow specified at " + workflowNode.getPath() + " not present";
            log.error(message);
            throw new RepositoryException(message, e);
        }
        String uuid = item.getIdentifier();
        /* The synchronized must operate on the core root session, because there is
         * only one such session, while there may be many decorated ones.
         */
        synchronized (SessionDecorator.unwrap(rootSession)) {
            Workflow workflow = null;
            if (InternalWorkflow.class.isAssignableFrom(clazz)) {
                try {
                    Constructor[] constructors = clazz.getConstructors();
                    for (final Constructor constructor : constructors) {
                        Class[] params = constructor.getParameterTypes();
                        if (params.length == 4 && WorkflowContext.class.isAssignableFrom(params[0])
                                && Session.class.isAssignableFrom(params[1])
                                && Session.class.isAssignableFrom(params[2])
                                && Node.class.isAssignableFrom(params[3])) {
                            workflow = (Workflow) constructor.newInstance(
                                    new WorkflowContextNodeImpl(workflowNode, getSession(), item), getSession(), rootSession, item);
                            break;
                        } else if (params.length == 3 && Session.class.isAssignableFrom(params[0])
                                && Session.class.isAssignableFrom(params[1])
                                && Node.class.isAssignableFrom(params[2])) {
                            workflow = (Workflow) constructor.newInstance(getSession(), rootSession, item);
                            break;
                        }
                    }
                    if(workflow == null) {
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

            } else if (WorkflowImpl.class.isAssignableFrom(clazz)) {
                try {
                    Object object = clazz.newInstance();
                    workflow = (Workflow)object;
                }
                catch (Exception ex) {
                    // TODO DEJDO?
                    throw new RepositoryException("Workflow class ["+classname+"] instantiation exception", ex);
                }
            }
            else {
                throw new RepositoryException("Unsupported type of workflow class ["+classname+"]");
            }
            if (workflow instanceof WorkflowImpl) {
                Node rootSessionNode = rootSession.getNodeByIdentifier(uuid);
                ((WorkflowImpl)workflow).setWorkflowContext(new WorkflowContextNodeImpl(workflowNode, item.getSession(), item));
                ((WorkflowImpl)workflow).setNode(rootSessionNode);
            }
            return workflow;
        }
    }

    public Workflow getWorkflow(WorkflowDescriptor descriptor) throws RepositoryException {
        WorkflowDescriptorImpl descriptorImpl = (WorkflowDescriptorImpl)descriptor;
        try {
            Node node = session.getNodeByIdentifier(descriptorImpl.getUuid());
            return getWorkflow(descriptorImpl.getCategory(), node);
        } catch (PathNotFoundException ex) {
            log.debug("Workflow no longer available "+descriptorImpl.getUuid());
            return null;
        }
    }

    public Workflow getWorkflow(String category, Node item) throws RepositoryException {
        Node workflowNode = getWorkflowNode(category, item);
        if (workflowNode != null) {
            final Workflow workflow = getRealWorkflow(item, workflowNode);
            if (workflow != null) {
                final String workflowName = workflowNode.getName();
                final boolean objectPersist = !InternalWorkflow.class.isInstance(workflow);
                final String path = item.getPath();
                final String uuid = item.getIdentifier();
                final Class[] interfaces = getRemoteInterfaces(workflow.getClass());
                final InvocationHandler handler = new WorkflowInvocationHandler(category, workflowName, workflow, uuid, path, objectPersist);
                return createWorkflow(workflow.getClass(), interfaces, handler);
            }
        }

        log.debug("Workflow for category {} on {} is not available", category, item.getPath());
        return null;
    }

    public Workflow getWorkflow(String category, Document document) throws RepositoryException {
        return getWorkflow(category, session.getNodeByUUID(document.getIdentity()));
    }

    private Workflow getWorkflow(Node workflowNode, WorkflowChainHandler handler) throws RepositoryException, WorkflowException {
        /* The synchronized must operate on the core root session, because there is
         * only one such session, while there may be many decorated ones.
         */
        synchronized (SessionDecorator.unwrap(rootSession)) {
            Workflow workflow;

            String workflowClassName = workflowNode.getProperty(HippoNodeType.HIPPO_CLASSNAME).getString();
            try {
                final Class<? extends Workflow> workflowClass = (Class<? extends Workflow>) Class.forName(workflowClassName);
                Class[] interfaces = getRemoteInterfaces(workflowClass);
                workflow = createWorkflow(workflowClass, interfaces, handler);
            } catch (ClassNotFoundException ex) {
                log.debug("Unable to locate workflow class", ex);
                throw new RepositoryException("Unable to locate workflow class", ex);
            }

            return workflow;
        }
    }

    Workflow getWorkflowInternal(Node workflowNode, Node item) throws RepositoryException {
        String category = workflowNode.getParent().getName();
        String workflowName = workflowNode.getName();
        String classname = workflowNode.getProperty(HippoNodeType.HIPPO_CLASSNAME).getString();
        Class clazz;
        try {
            clazz = Class.forName(classname);
        } catch (ClassNotFoundException e) {
            log.error("Workflow specified at " + workflowNode.getPath() + " not present");
            throw new RepositoryException("workflow not present", e);
        }

        String uuid = item.getIdentifier();
        String path = item.getPath();

        /* The synchronized must operate on the core root session, because there is
         * only one such session, while there may be many decorated ones.
         */
        synchronized (SessionDecorator.unwrap(rootSession)) {

            Workflow workflow;
            if (WorkflowImpl.class.isAssignableFrom(clazz)) {
                try {
                    workflow = (Workflow)clazz.newInstance();
                }
                catch (Exception ex) {
                    // TODO DEJDO?
                    throw new RepositoryException("Workflow class ["+classname+"] instantiation exception", ex);
                }
            }
            else {
                throw new RepositoryException("Unsupported type of workflow class ["+classname+"]");
            }

            if (workflow instanceof WorkflowImpl) {
                Node rootSessionNode = rootSession.getNodeByIdentifier(uuid);
                ((WorkflowImpl)workflow).setWorkflowContext(new WorkflowContextNodeImpl(workflowNode, getSession(), item));
                ((WorkflowImpl)workflow).setNode(rootSessionNode);
            }
            Class[] interfaces = getRemoteInterfaces(workflow.getClass());
            InvocationHandler handler = new WorkflowInvocationHandler(category, workflowName, workflow, uuid, path, true);
            return createWorkflow(workflow.getClass(), interfaces, handler);
        }
    }

    private Workflow createWorkflow(final Class<? extends Workflow> workflowClass, final Class[] interfaces, final InvocationHandler handler) throws RepositoryException {
        try {
            Class proxyClass = Proxy.getProxyClass(workflowClass.getClassLoader(), interfaces);
            return (Workflow)proxyClass.getConstructor(new Class[] {InvocationHandler.class}).newInstance(handler);
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
    }

    private Class[] getRemoteInterfaces(Class<? extends Workflow> workflowClass) {
        Set<Class> result = new HashSet<>();
        Class<?> klass = workflowClass;
        while (Workflow.class.isAssignableFrom(klass)) {
            Class[] interfaces = workflowClass.getInterfaces();
            for (final Class anInterface : interfaces) {
                if (Remote.class.isAssignableFrom(anInterface)) {
                    result.add(anInterface);
                }
            }
            klass = klass.getSuperclass();
        }
        return result.toArray(new Class[result.size()]);
    }

    @Override
    public WorkflowManager getContextWorkflowManager(Object specification) throws MappingException, RepositoryException {
        return null;
    }

    class WorkflowInvocationHandler implements InvocationHandler {
        String category;
        String workflowName;
        Workflow upstream;
        String uuid;
        String path;
        boolean objectPersist;

        WorkflowInvocationHandler(String category, String workflowName, Workflow upstream, String uuid, String path, boolean objectPersist) {
            this.category = category;
            this.workflowName = workflowName;
            this.upstream = upstream;
            this.uuid = uuid;
            this.path = path;
            this.objectPersist = objectPersist;
        }

        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            Object returnObject = null;
            Throwable returnException = null;

            if ("toString".equals(method.getName()) && (args == null || args.length == 0)) {
                return "WorkflowInvocationHandler[" + category + ", " + workflowName + "]";
            }

            invocationChain = new LinkedList<WorkflowInvocation>();
            invocationIndex = invocationChain.listIterator();

            synchronized (SessionDecorator.unwrap(rootSession)) {
                rootSession.refresh(false);

                WorkflowPostActions postActions = null;
                boolean resetInteraction = false;
                try {
                    String interactionId = INTERACTION_ID.get();
                    String interaction = INTERACTION.get();
                    if (interactionId == null) {
                        interactionId = UUID.randomUUID().toString();
                        INTERACTION_ID.set(interactionId);
                        interaction = category + ":" + workflowName + ":" + method.getName();
                        INTERACTION.set(interaction);
                        resetInteraction = true;
                    }

                    Method targetMethod = upstream.getClass().getMethod(method.getName(), method.getParameterTypes());
                    postActions = WorkflowPostActionsImpl.createPostActions(WorkflowManagerImpl.this, category, targetMethod, uuid);
                    returnObject = targetMethod.invoke(upstream, args);
                    if (objectPersist && !targetMethod.getName().equals("hints")) {
                        rootSession.save();
                    }
                    if (returnObject instanceof Document) {
                        Document doc = (Document)returnObject;
                        if (doc.getNode() != null) {
                            returnObject = new Document(doc.getNode());
                        }
                        else {
                            returnObject = new Document();
                            ((Document)returnObject).setIdentity(doc.getIdentity());
                        }
                    }
                    while (!invocationChain.isEmpty()) {
                        WorkflowInvocationImpl current = (WorkflowInvocationImpl) invocationChain.remove(0);
                        invocationIndex = invocationChain.listIterator();
                        current.invoke(WorkflowManagerImpl.this);
                    }
                    WorkflowAction wfActionAnno = AnnotationUtils.findMethodAnnotation(targetMethod, WorkflowAction.class);
                    if (wfActionAnno == null || wfActionAnno.loggable()) {
                        eventLoggerWorkflow.logWorkflowStep(session.getUserID(), upstream.getClass().getName(),
                                targetMethod.getName(), args, returnObject, path, interaction, interactionId,
                                category, workflowName);
                    }
                    if (postActions != null) {
                        postActions.execute(returnObject);
                    }
                    return returnObject;
                } catch (NoSuchMethodException ex) {
                    rootSession.refresh(false);
                    throw returnException = new RepositoryException("Impossible failure for workflow proxy", ex);
                } catch (IllegalAccessException ex) {
                    rootSession.refresh(false);
                    throw returnException = new RepositoryException("Impossible failure for workflow proxy", ex);
                } catch (InvocationTargetException ex) {
                    rootSession.refresh(false);
                    log.info(ex.getClass().getName()+": "+ex.getMessage(), ex);
                    throw returnException = ex.getCause();
                } finally {
                    if (resetInteraction) {
                        INTERACTION.remove();
                        INTERACTION_ID.remove();
                    }
                    if (postActions != null) {
                        postActions.dispose();
                    }
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
        }
    }

    private abstract class WorkflowChainHandler implements InvocationHandler {
        protected Node workflowNode;
        WorkflowInvocationHandlerModule module;

        WorkflowChainHandler(Node workflowNode, WorkflowInvocationHandlerModule module) {
            this.workflowNode = workflowNode;
            this.module = module;
        }

        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            return invoke(method, args);
        }

        abstract Object invoke(Method method, Object[] args) throws RepositoryException, WorkflowException;

        public void submit(WorkflowManager workflowManager, WorkflowInvocationImpl invocation) throws RepositoryException, WorkflowException {
            module.submit(workflowManager, invocation);
        }
    }

    public static class WorkflowInvocationImpl implements WorkflowInvocation {

        private WorkflowManager workflowManager;
        private Node workflowNode;
        private Document workflowSubject;
        private Node workflowSubjectNode;
        private Method method;
        private Object[] arguments;
        private String category = null;
        private String workflowName = null;
        private String methodName = null;
        private Class[] parameterTypes = null;
        private String interactionId;
        private String interaction;

        public WorkflowInvocationImpl(final String category,
                                      final String workflowName,
                                      final String subjectId,
                                      final String methodName,
                                      final Class[] parameterTypes,
                                      final Object[] arguments,
                                      final String interactionId,
                                      final String interaction) {
            this.category = category;
            this.workflowName = workflowName;
            workflowSubject = new Document();
            workflowSubject.setIdentity(subjectId);
            this.methodName = methodName;
            this.parameterTypes = new Class[parameterTypes.length];
            for (int index = 0; index < parameterTypes.length; index++) {
                Class<?> type = parameterTypes[index];
                if (type.isPrimitive()) {
                    type = PRIMITIVE_TO_OBJECT_TYPES.get(type);
                }
                this.parameterTypes[index] = type;
            }
            this.arguments = arguments;
            this.interactionId = interactionId;
            this.interaction = interaction;
        }



        WorkflowInvocationImpl(WorkflowManager workflowManager, Node workflowNode, Session rootSession, Document workflowSubject, Method method, Object[] args) throws RepositoryException {
            this(workflowNode.getParent().getName(), workflowNode.getName(), workflowSubject.getIdentity(), method.getName(), method.getParameterTypes(),
                    (args != null ? args.clone() : null), WorkflowManagerImpl.INTERACTION_ID.get(), WorkflowManagerImpl.INTERACTION.get());
            this.workflowManager = workflowManager;
            this.workflowNode = workflowNode;
            this.workflowSubject = workflowSubject;
            this.method = method;
            this.workflowSubjectNode = null;
            try {
                String uuid = workflowSubject.getIdentity();
                if (uuid != null && !"".equals(uuid)) {
                    this.workflowSubjectNode = rootSession.getNodeByIdentifier(uuid);
                }
            } catch (ItemNotFoundException ignore) {
            }
        }

        WorkflowInvocationImpl(WorkflowManager workflowManager, Node workflowNode, Session rootSession, Node workflowSubject, Method method, Object[] args) throws RepositoryException {
            this(workflowNode.getParent().getName(), workflowNode.getName(), workflowSubject.getIdentifier(), method.getName(), method.getParameterTypes(),
                    (args != null ? args.clone() : null), WorkflowManagerImpl.INTERACTION_ID.get(), WorkflowManagerImpl.INTERACTION.get());
            this.workflowManager = workflowManager;
            this.workflowNode = workflowNode;
            this.workflowSubject = null;
            this.method = method;
            this.workflowSubjectNode = rootSession.getNodeByIdentifier(workflowSubject.getIdentifier());
        }

        @Override
        public Node getSubject() {
            return workflowSubjectNode;
        }

        @Override
        public void setSubject(Node node) {
            try {
                workflowManager = ((HippoWorkspace)node.getSession().getWorkspace()).getWorkflowManager();
            } catch(RepositoryException ex) {
                log.error(ex.getClass().getName()+": "+ex.getMessage(), ex);
            }
            workflowSubjectNode = node;
            workflowSubject = null;
        }

        @Override
        public String getCategory() {
            return category;
        }

        @Override
        public String getWorkflowName() {
            return workflowName;
        }

        @Override
        public String getMethodName() {
            return methodName;
        }

        @Override
        public Class[] getParameterTypes() {
            return parameterTypes;
        }

        @Override
        public Object[] getArguments() {
            return arguments;
        }

        @Override
        public String getInteractionId() {
            return interactionId;
        }

        @Override
        public String getInteraction() {
            return interaction;
        }

        @Override
        public Object invoke(Session session) throws RepositoryException, WorkflowException {
            workflowSubjectNode = session.getNodeByUUID(workflowSubjectNode.getUUID());
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
                    Class<?> type = types[i];
                    if (type.isPrimitive()) {
                        type = PRIMITIVE_TO_OBJECT_TYPES.get(type);
                    }
                    if(!type.equals(parameterTypes[i])) {
                        method = null;
                        break;
                    }
                }
            }
            boolean resetInteractionId = false;
            try {
                if (WorkflowManagerImpl.INTERACTION_ID.get() == null && interactionId != null) {
                    WorkflowManagerImpl.INTERACTION_ID.set(interactionId);
                    WorkflowManagerImpl.INTERACTION.set(interaction);
                    resetInteractionId = true;
                }
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
            } finally {
                if (resetInteractionId) {
                    WorkflowManagerImpl.INTERACTION_ID.remove();
                    WorkflowManagerImpl.INTERACTION.remove();
                }
            }
        }

        Object invoke(WorkflowManagerImpl manager) throws RepositoryException, WorkflowException {
            WorkflowPostActions postActions = null;
            try {
                Workflow workflow = null;
                String classname = workflowNode.getProperty(HippoNodeType.HIPPO_CLASSNAME).getString();

                Node item = workflowSubjectNode;
                if (item == null) {
                    item = manager.rootSession.getNodeByUUID(workflowSubject.getIdentity());
                }
                String uuid = item.getIdentifier();
                String path = item.getPath();
                String userId = item.getSession().getUserID();
                boolean objectPersist;
                postActions = WorkflowPostActionsImpl.createPostActions(manager, category, method, item.getIdentifier());
                try {
                    Class clazz = Class.forName(classname);
                    if (InternalWorkflow.class.isAssignableFrom(clazz)) {
                        objectPersist = false;
                        Constructor[] constructors = clazz.getConstructors();
                        int constructorIndex;
                        for (constructorIndex = 0; constructorIndex < constructors.length; constructorIndex++) {
                            Class[] params = constructors[constructorIndex].getParameterTypes();
                            if (params.length == 4 && WorkflowContext.class.isAssignableFrom(params[0]) && Session.class.isAssignableFrom(params[1]) && Session.class.isAssignableFrom(params[2]) && Node.class.isAssignableFrom(params[3])) {
                                workflow = (Workflow)constructors[constructorIndex].newInstance(manager.new WorkflowContextNodeImpl(workflowNode, item.getSession(), item), manager.getSession(), manager.rootSession, item);
                                break;
                            } else if (params.length == 3 && Session.class.isAssignableFrom(params[0]) && Session.class.isAssignableFrom(params[1]) && Node.class.isAssignableFrom(params[2])) {
                                workflow = (Workflow)constructors[constructorIndex].newInstance(manager.getSession(), manager.rootSession, item);
                                break;
                            }
                        }
                        if (constructorIndex == constructors.length) {
                            throw new RepositoryException("no valid constructor found in standards plugin");
                        }
                    } else if (WorkflowImpl.class.isAssignableFrom(clazz)) {
                        objectPersist = true;
                        Object object = clazz.newInstance();
                        workflow = (Workflow)object;
                    }
                    else {
                        throw new RepositoryException("Unsupported workflow class type ["+classname+"]");
                    }
                    if (workflow instanceof WorkflowImpl) {
                        Node rootSessionNode = ((WorkflowManagerImpl)workflowManager).rootSession.getNodeByIdentifier(uuid);
                        ((WorkflowImpl)workflow).setWorkflowContext(manager.new WorkflowContextNodeImpl(workflowNode, item.getSession(), item));
                        ((WorkflowImpl)workflow).setNode(rootSessionNode);
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
                    if (objectPersist && !targetMethod.getName().equals("hints")) {
                        manager.rootSession.save();
                    }
                    WorkflowAction wfActionAnno = AnnotationUtils.findMethodAnnotation(targetMethod, WorkflowAction.class);
                    if (wfActionAnno == null || wfActionAnno.loggable()) {
                        manager.eventLoggerWorkflow.logWorkflowStep(userId, workflow.getClass().getName(),
                                targetMethod.getName(), arguments, returnObject, path, interaction, interactionId,
                                category, workflowName);
                    }
                    if (postActions != null) {
                        postActions.execute(returnObject);
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
            } finally {
                if (postActions != null) {
                    postActions.dispose();
                }
            }
        }
    }

    static class InvocationBinding<T> implements Comparable {
        Class<T> contextClass;
        WorkflowInvocationHandlerModuleFactory<T> handlerClass;
        public InvocationBinding(Class<T> contextClass, WorkflowInvocationHandlerModuleFactory<T> handlerClass) {
            this.contextClass = contextClass;
            this.handlerClass = handlerClass;
        }
        @Override
        public int compareTo(Object o) {
            if (o == null) {
                throw new NullPointerException();
            }
            if (o == this) {
                return 0;
            }
            InvocationBinding other = (InvocationBinding)o;
            if (other.contextClass.equals(contextClass)) {
                return 0;
            }
            if (contextClass.isAssignableFrom(other.contextClass)) {
                return -1;
            } else if (other.contextClass.isAssignableFrom(contextClass)) {
                return 1;
            } else {
                return contextClass.getName().compareTo(other.contextClass.getName());
            }
        }
    }

    private abstract class WorkflowContextImpl implements WorkflowContext {
        Session subjectSession;
        Node workflowDefinition;
        WorkflowInvocationHandlerModule module;

        WorkflowContextImpl(Node workflowNode, Session subjectSession) {
            this(workflowNode, subjectSession, null);
        }

        WorkflowContextImpl(Node workflowNode, Session subjectSession, WorkflowInvocationHandlerModule module) {
            this.workflowDefinition = workflowNode;
            this.subjectSession = subjectSession;
            if(module == null) {
                this.module = new WorkflowInvocationHandlerModule() {
                        public Object submit(WorkflowManager manager, WorkflowInvocation invocation) {
                            invocationIndex.add(invocation);
                            return null;
                        }
                    };
            } else {
                this.module = module;
            }
        }

        WorkflowInvocationHandlerModule getWorkflowInvocationHandlerModule(Object specification) {
            Modules<WorkflowManagerModule> modules = new Modules<WorkflowManagerModule>(Modules.getModules(), WorkflowManagerModule.class);
            final SortedSet<InvocationBinding> invocationHandlers = new TreeSet<InvocationBinding>();
            for (WorkflowManagerModule module : modules) {
                module.register(new WorkflowManagerRegister() {
                    public <T> void bind(Class<T> contextClass, WorkflowInvocationHandlerModuleFactory<T> handlerClass) {
                        invocationHandlers.add(new InvocationBinding(contextClass, handlerClass));
                    }
                });
            }
            for(InvocationBinding invocationHandler : invocationHandlers) {
                if(invocationHandler.contextClass.isInstance(specification)) {
                    return invocationHandler.handlerClass.createInvocationHandler(specification);
                }
            }
            return null;
        }

        public WorkflowContext getWorkflowContext(Object specification) throws MappingException, RepositoryException {
            if(specification instanceof Document) {
                String uuid = ((Document)specification).getIdentity();
                Node node = subjectSession.getNodeByUUID(uuid);
                return new WorkflowContextNodeImpl(workflowDefinition, subjectSession, node, module);
            } else if(specification == null) {
                return newContext(workflowDefinition, subjectSession, new WorkflowInvocationHandlerModule() {
                    public Object submit(WorkflowManager workflowManager, WorkflowInvocation invocation) throws RepositoryException, WorkflowException {
                        return invocation.invoke(rootSession);
                    }
                });
            } else {
                WorkflowInvocationHandlerModule invocationHandlerModule = getWorkflowInvocationHandlerModule(specification);
                if (invocationHandlerModule != null) {
                    return newContext(workflowDefinition, subjectSession, invocationHandlerModule);
                } else {
                    log.debug("No context defined for class " + specification.getClass().getName());
                    throw new MappingException("No context defined for class " + specification.getClass().getName());
                }
            }
        }

        public Workflow getWorkflow(String category, final Document document) throws MappingException, WorkflowException, RepositoryException {
            Node workflowNode = WorkflowManagerImpl.this.getWorkflowNode(category, document);
            if (workflowNode != null) {
                return WorkflowManagerImpl.this.getWorkflow(workflowNode,
                       new WorkflowChainHandler(workflowNode, module) {
                           @Override
                           public Object invoke(Method method, Object[] args) throws RepositoryException, WorkflowException {
                               return module.submit(WorkflowManagerImpl.this, new WorkflowInvocationImpl(WorkflowManagerImpl.this, workflowNode, rootSession, document, method, args));
                           }
                       });
            }
            log.debug("Workflow for category "+category+" on document is not available");
            throw new MappingException("Workflow for category "+category+" on document is not available");
        }

        protected abstract WorkflowContextImpl newContext(Node workflowDefinition, Session subjectSession, WorkflowInvocationHandlerModule specification);

        public abstract Workflow getWorkflow(String category) throws MappingException, WorkflowException, RepositoryException;

        public String getUserIdentity() {
            return session.getUserID();
        }

        public Session getUserSession() {
            return session;
        }

        public Session getInternalWorkflowSession() {
            return WorkflowManagerImpl.this.rootSession;
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

        WorkflowContextNodeImpl(Node workflowNode, Session subjectSession, Node subject, WorkflowInvocationHandlerModule module) {
            super(workflowNode, subjectSession, module);
            this.subject = subject;
        }

        public Workflow getWorkflow(String category) throws MappingException, WorkflowException, RepositoryException {
            Node workflowNode = WorkflowManagerImpl.this.getWorkflowNode(category, subject);
            if (workflowNode != null) {
                return WorkflowManagerImpl.this.getWorkflow(workflowNode,
                       new WorkflowChainHandler(workflowNode, module) {
                           @Override
                           public Object invoke(Method method, Object[] args) throws RepositoryException, WorkflowException {
                               return module.submit(WorkflowManagerImpl.this, new WorkflowInvocationImpl(WorkflowManagerImpl.this, workflowNode, rootSession, subject, method, args));
                           }
                       });
            }
            log.debug("Workflow for category "+category+" on document is not available");
            throw new MappingException("Workflow for category "+category+" on document is not available");
        }

        protected WorkflowContextImpl newContext(Node workflowDefinition, Session subjectSession, WorkflowInvocationHandlerModule specification) {
            return new WorkflowContextNodeImpl(workflowDefinition, subjectSession, subject, specification);
        }
    }

    private class WorkflowContextDocumentImpl extends WorkflowContextImpl {
        Document subject;

        WorkflowContextDocumentImpl(Node workflowNode, Session subjectSession, Document subject) {
            super(workflowNode, subjectSession);
            this.subject = subject;
        }

        WorkflowContextDocumentImpl(Node workflowNode, Session subjectSession, Document subject, WorkflowInvocationHandlerModule module) {
            super(workflowNode, subjectSession, module);
            this.subject = subject;
        }

        public Workflow getWorkflow(String category) throws MappingException, WorkflowException, RepositoryException {
            Node workflowNode = WorkflowManagerImpl.this.getWorkflowNode(category, subject);
            if (workflowNode != null) {
                return WorkflowManagerImpl.this.getWorkflow(workflowNode,
                       new WorkflowChainHandler(workflowNode, module) {
                           @Override
                           public Object invoke(Method method, Object[] args) throws RepositoryException, WorkflowException {
                               return module.submit(WorkflowManagerImpl.this, new WorkflowInvocationImpl(WorkflowManagerImpl.this, workflowNode, rootSession, subject, method, args));
                           }
                       });
            }
            log.debug("Workflow for category "+category+" on document is not available");
            throw new MappingException("Workflow for category "+category+" on document is not available");
        }

        protected WorkflowContextImpl newContext(Node workflowDefinition, Session subjectSession, WorkflowInvocationHandlerModule specification) {
            return new WorkflowContextDocumentImpl(workflowDefinition, subjectSession, subject, specification);
        }
    }
}
