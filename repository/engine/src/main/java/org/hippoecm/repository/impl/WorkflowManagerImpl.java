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
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

import org.hippoecm.repository.Modules;
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

/**
 * This class is not part of a public accessible API or extensible interface
 */
public class WorkflowManagerImpl implements WorkflowManager {

    static final Logger log = LoggerFactory.getLogger(WorkflowManagerImpl.class);

    static final ThreadLocal<String> INTERACTION_ID = new ThreadLocal<String>();
    static final ThreadLocal<String> INTERACTION = new ThreadLocal<String>();

    /**
     * Session from which this WorkflowManager instance was created.  Is used to look up which workflows are active for
     * a user.  It is however not used to instantiate workflows, persist and as execution context when performing a
     * workflow step (i.e. method invocation).
     */
    Session session;
    Session rootSession;
    String configuration;
    List<WorkflowInvocation> invocationChain;
    ListIterator<WorkflowInvocation> invocationIndex;
    WorkflowEventLoggerWorkflow eventLoggerWorkflow;

    public WorkflowManagerImpl(Session session, Session rootSession) {
        this.session = session;
        this.rootSession = rootSession;
        try {
            configuration = session.getRootNode().getNode(HippoNodeType.CONFIGURATION_PATH + "/" +
                    HippoNodeType.WORKFLOWS_PATH).getIdentifier();
            if (session.nodeExists("/hippo:log")) {
                final Node logFolder = session.getNode("/hippo:log");
                final WorkflowDefinition workflowDefinition = getWorkflowDefinition("internal", logFolder);
                Workflow workflow = createWorkflow(logFolder, workflowDefinition);
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
            log.error("Workflow manager configuration failed: " + ex.getMessage(), ex);
        } catch (WorkflowException e) {
            log.error("Failed to create workflow logger: " + e.getMessage(), e);
        }
    }

    public Session getSession() throws RepositoryException {
        return session;
    }

    WorkflowDefinition getWorkflowDefinition(String category, Node item) {
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
                    if (!item.isNodeType(nodeTypeName)) {
                        continue;
                    }

                    if (workflowNode.hasProperty(HippoNodeType.HIPPOSYS_SUBTYPE)) {
                        if (!HippoNodeType.NT_HANDLE.equals(nodeTypeName)) {
                            log.warn("Unsupported property 'hipposys:subtype' on nodetype '" + nodeTypeName + "'");
                        } else {
                            if (!item.hasNode(item.getName())) {
                                log.warn("No child node exists for handle {}", item.getPath());
                                return null;
                            }
                            final String subTypeName = workflowNode.getProperty(HippoNodeType.HIPPOSYS_SUBTYPE).getString();
                            Node variant = item.getNode(item.getName());
                            if (!variant.isNodeType(subTypeName)) {
                                continue;
                            }
                            if (checkWorkflowPermission(variant, workflowNode)) {
                                return new WorkflowDefinition(workflowNode);
                            } else {
                                continue;
                            }
                        }
                    }

                    log.debug("Found workflow in category {} for node {}", category, item.getPath());
                    if (checkWorkflowPermission(item, workflowNode)) {
                        return new WorkflowDefinition(workflowNode);
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

    WorkflowDefinition getWorkflowDefinition(String category, Document document) {
        if (configuration == null) {
            return null;
        }
        if (document == null || document.getIdentity() == null) {
            log.error("Cannot retrieve workflow for null document");
            return null;
        }
        try {
            Node documentNode = rootSession.getNodeByIdentifier(document.getIdentity());
            return getWorkflowDefinition(category, documentNode);
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
        WorkflowDefinition workflowDefinition = getWorkflowDefinition(category, item);
        if (workflowDefinition != null) {
            return new WorkflowDescriptorImpl(this, category, workflowDefinition, item);
        }
        log.debug("Workflow for category " + category + " on " + item.getPath() + " is not available");
        return null;
    }

    public WorkflowDescriptor getWorkflowDescriptor(String category, Document document) throws RepositoryException {
        WorkflowDefinition workflowDefinition = getWorkflowDefinition(category, document);
        if (workflowDefinition != null) {
            return new WorkflowDescriptorImpl(this, category, workflowDefinition, document);
        }
        log.debug("Workflow for category " + category + " on " + document.getIdentity() + " is not available");
        return null;
    }

    public Workflow getWorkflow(WorkflowDescriptor descriptor) throws RepositoryException {
        WorkflowDescriptorImpl descriptorImpl = (WorkflowDescriptorImpl) descriptor;
        try {
            Node node = session.getNodeByIdentifier(descriptorImpl.getUuid());
            return getWorkflow(descriptorImpl.getCategory(), node);
        } catch (PathNotFoundException ex) {
            log.debug("Workflow no longer available " + descriptorImpl.getUuid());
            return null;
        }
    }

    public Workflow getWorkflow(String category, Node item) throws RepositoryException {
        WorkflowDefinition workflowDefinition = getWorkflowDefinition(category, item);
        if (workflowDefinition != null) {
            return createProxiedWorkflow(workflowDefinition, item);
        }

        log.debug("Workflow for category {} on {} is not available", category, item.getPath());
        return null;
    }

    public Workflow getWorkflow(String category, Document document) throws RepositoryException {
        return getWorkflow(category, session.getNodeByIdentifier(document.getIdentity()));
    }

    private Workflow getWorkflow(WorkflowDefinition workflowDefinition, WorkflowChainHandler handler) throws RepositoryException, WorkflowException {
        /* The synchronized must operate on the core root session, because there is
         * only one such session, while there may be many decorated ones.
         */
        synchronized (SessionDecorator.unwrap(rootSession)) {
            Workflow workflow;

            final Class<? extends Workflow> workflowClass = workflowDefinition.getWorkflowClass();
            Class[] interfaces = getRemoteInterfaces(workflowClass);
            workflow = createWorkflowProxy(workflowClass, interfaces, handler);

            return workflow;
        }
    }

    Workflow createProxiedWorkflow(WorkflowDefinition workflowDefinition, Node item) throws RepositoryException {
        String category = workflowDefinition.getCategory();
        String workflowName = workflowDefinition.getName();

        String uuid = item.getIdentifier();
        String path = item.getPath();

        /* The synchronized must operate on the core root session, because there is
         * only one such session, while there may be many decorated ones.
         */
        Workflow workflow = createWorkflow(item, workflowDefinition);
        final boolean objectPersist = workflow instanceof WorkflowImpl;

        Class[] interfaces = getRemoteInterfaces(workflow.getClass());
        InvocationHandler handler = new WorkflowInvocationHandler(category, workflowName, workflow, uuid, path, objectPersist);
        return createWorkflowProxy(workflow.getClass(), interfaces, handler);
    }

    Workflow createWorkflow(Node item, WorkflowDefinition workflowDefinition) throws RepositoryException {
        if (workflowDefinition == null) {
            return null;
        }

        String uuid = item.getIdentifier();
        /* The synchronized must operate on the core root session, because there is
         * only one such session, while there may be many decorated ones.
         */
        synchronized (SessionDecorator.unwrap(rootSession)) {
            Workflow workflow = null;
            Class<? extends Workflow> clazz = workflowDefinition.getWorkflowClass();
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
                                    new WorkflowContextNodeImpl(workflowDefinition, getSession(), item), getSession(), rootSession, item);
                            break;
                        } else if (params.length == 3 && Session.class.isAssignableFrom(params[0])
                                && Session.class.isAssignableFrom(params[1])
                                && Node.class.isAssignableFrom(params[2])) {
                            workflow = (Workflow) constructor.newInstance(getSession(), rootSession, item);
                            break;
                        }
                    }
                    if (workflow == null) {
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
                    workflow = clazz.newInstance();
                } catch (Exception ex) {
                    // TODO DEJDO?
                    throw new RepositoryException("Workflow class [" + clazz.getName() + "] instantiation exception", ex);
                }
            } else {
                throw new RepositoryException("Unsupported type of workflow class [" + clazz.getName() + "]");
            }
            if (WorkflowImpl.class.isAssignableFrom(clazz)) {
                Node rootSessionNode = rootSession.getNodeByIdentifier(uuid);
                ((WorkflowImpl) workflow).setWorkflowContext(new WorkflowContextNodeImpl(workflowDefinition, item.getSession(), item));
                ((WorkflowImpl) workflow).setNode(rootSessionNode);
            }
            return workflow;
        }
    }

    private Workflow createWorkflowProxy(final Class<? extends Workflow> workflowClass, final Class[] interfaces, final InvocationHandler handler) throws RepositoryException {
        try {
            Class proxyClass = Proxy.getProxyClass(workflowClass.getClassLoader(), interfaces);
            return (Workflow) proxyClass.getConstructor(new Class[]{InvocationHandler.class}).newInstance(handler);
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
            Class[] interfaces = klass.getInterfaces();
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
                        Document doc = (Document) returnObject;
                        if (doc.getNode() != null) {
                            returnObject = new Document(doc.getNode());
                        } else {
                            returnObject = new Document();
                            ((Document) returnObject).setIdentity(doc.getIdentity());
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
                    log.info(ex.getClass().getName() + ": " + ex.getMessage(), ex);
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
                    sb.append(upstream != null ? upstream.getClass().getName() : "<unknown>");
                    sb.append(".");
                    sb.append(method != null ? method.getName() : "<unknown>");
                    sb.append("(");
                    if (args != null) {
                        for (int i = 0; i < args.length; i++) {
                            if (i > 0) {
                                sb.append(", ");
                            }
                            sb.append(args[i] != null ? args[i].toString() : "null");
                        }
                    }
                    sb.append(") -> ");
                    if (returnException != null) {
                        sb.append(returnException.getClass().getName());
                    } else if (returnObject != null) {
                        sb.append(returnObject.toString());
                    } else {
                        sb.append("<<null>>");
                    }
                    if (method != null && method.getName().equals("hints")) {
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
        protected WorkflowDefinition workflowDefinition;
        WorkflowInvocationHandlerModule module;

        WorkflowChainHandler(WorkflowDefinition workflowDefinition, WorkflowInvocationHandlerModule module) {
            this.workflowDefinition = workflowDefinition;
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
            InvocationBinding other = (InvocationBinding) o;
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
        WorkflowDefinition workflowDefinition;
        WorkflowInvocationHandlerModule module;

        WorkflowContextImpl(WorkflowDefinition workflowDefinition, Session subjectSession) {
            this(workflowDefinition, subjectSession, null);
        }

        WorkflowContextImpl(WorkflowDefinition workflowDefinition, Session subjectSession, WorkflowInvocationHandlerModule module) {
            this.workflowDefinition = workflowDefinition;
            this.subjectSession = subjectSession;
            if (module == null) {
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
            for (InvocationBinding invocationHandler : invocationHandlers) {
                if (invocationHandler.contextClass.isInstance(specification)) {
                    return invocationHandler.handlerClass.createInvocationHandler(specification);
                }
            }
            return null;
        }

        public WorkflowContext getWorkflowContext(Object specification) throws MappingException, RepositoryException {
            if (specification instanceof Document) {
                String uuid = ((Document) specification).getIdentity();
                Node node = subjectSession.getNodeByUUID(uuid);
                return new WorkflowContextNodeImpl(workflowDefinition, subjectSession, node, module);
            } else if (specification == null) {
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
            WorkflowDefinition workflowDefinition = WorkflowManagerImpl.this.getWorkflowDefinition(category, document);
            if (workflowDefinition != null) {
                return WorkflowManagerImpl.this.getWorkflow(workflowDefinition,
                        new WorkflowChainHandler(workflowDefinition, module) {
                            @Override
                            public Object invoke(Method method, Object[] args) throws RepositoryException, WorkflowException {
                                return module.submit(WorkflowManagerImpl.this, new WorkflowInvocationImpl(WorkflowManagerImpl.this, this.workflowDefinition, rootSession, document, method, args));
                            }
                        });
            }
            log.debug("Workflow for category " + category + " on document is not available");
            throw new MappingException("Workflow for category " + category + " on document is not available");
        }

        protected abstract WorkflowContextImpl newContext(WorkflowDefinition workflowDefinition, Session subjectSession, WorkflowInvocationHandlerModule specification);

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
            return workflowDefinition.getWorkflowConfiguration();
        }
    }

    class WorkflowContextNodeImpl extends WorkflowContextImpl {
        Node subject;

        WorkflowContextNodeImpl(WorkflowDefinition workflowDefinition, Session subjectSession, Node subject) {
            super(workflowDefinition, subjectSession);
            this.subject = subject;
        }

        WorkflowContextNodeImpl(WorkflowDefinition workflowDefinition, Session subjectSession, Node subject, WorkflowInvocationHandlerModule module) {
            super(workflowDefinition, subjectSession, module);
            this.subject = subject;
        }

        public Workflow getWorkflow(String category) throws MappingException, WorkflowException, RepositoryException {
            WorkflowDefinition workflowDefinition = WorkflowManagerImpl.this.getWorkflowDefinition(category, subject);
            if (workflowDefinition != null) {
                return WorkflowManagerImpl.this.getWorkflow(workflowDefinition,
                        new WorkflowChainHandler(workflowDefinition, module) {
                            @Override
                            public Object invoke(Method method, Object[] args) throws RepositoryException, WorkflowException {
                                return module.submit(WorkflowManagerImpl.this, new WorkflowInvocationImpl(WorkflowManagerImpl.this, this.workflowDefinition, rootSession, subject, method, args));
                            }
                        });
            }
            log.debug("Workflow for category " + category + " on document is not available");
            throw new MappingException("Workflow for category " + category + " on document is not available");
        }

        @Override
        protected WorkflowContextImpl newContext(WorkflowDefinition workflowDefinition, Session subjectSession, WorkflowInvocationHandlerModule specification) {
            return new WorkflowContextNodeImpl(workflowDefinition, subjectSession, subject, specification);
        }
    }

}
