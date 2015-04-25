/*
 *  Copyright 2008-2015 Hippo B.V. (http://www.onehippo.com)
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
import java.util.Set;
import java.util.UUID;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.Value;

import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.api.RepositoryMap;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowContext;
import org.hippoecm.repository.api.WorkflowDescriptor;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.ext.InternalWorkflow;
import org.hippoecm.repository.ext.WorkflowImpl;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.NodeIterable;
import org.onehippo.repository.api.annotation.WorkflowAction;
import org.onehippo.repository.util.AnnotationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.repository.api.HippoNodeType.CONFIGURATION_PATH;
import static org.hippoecm.repository.api.HippoNodeType.WORKFLOWS_PATH;

public class WorkflowManagerImpl implements WorkflowManager {

    private static final Logger log = LoggerFactory.getLogger(WorkflowManagerImpl.class);

    private static final ThreadLocal<String> tlInteractionId = new ThreadLocal<>();
    private static final ThreadLocal<String> tlInteraction = new ThreadLocal<>();

    /**
     * userSession is the session from which this WorkflowManager instance was created.
     * <p>
     * It is used to look up which workflows are active for a user.
     * Unless a workflow is {@link #getWorkflow(String, Node) requested} for a specific JCR Node, in which case
     * the provide node its session is used for that purpose.
     * </p>
     * <p>
     *  A separate internal workflow session is used to instantiate the actual workflows, as execution context
     *  when performing a workflow step (i.e. method invocation), and to persist changes.
     * </p>
     */
    private final Session userSession;
    private Session rootSession;
    private final String configurationId;
    private final WorkflowLogger workflowLogger;

    public WorkflowManagerImpl(Session session) throws RepositoryException {
        this.userSession = session;
        this.rootSession = session.impersonate(new SimpleCredentials("workflowuser", new char[] {}));
        ((HippoSession) rootSession).disableVirtualLayers();
        configurationId = session.getRootNode().getNode(CONFIGURATION_PATH + "/" + WORKFLOWS_PATH).getIdentifier();
        workflowLogger = new WorkflowLogger(rootSession);
    }

    public Session getSession() throws RepositoryException {
        return userSession;
    }

    private WorkflowDefinition  getWorkflowDefinition(String category, Node item) {
        if (configurationId == null) {
            return null;
        }
        if (item == null) {
            log.error("Cannot retrieve workflow for null node");
            return null;
        }

        try {
            log.debug("Looking for workflow in category {} for node {}", category, item.getPath());

            Node configuration = JcrUtils.getNodeIfExists(rootSession.getNodeByIdentifier(configurationId), category);
            if (configuration != null) {
                for (Node workflowNode : new NodeIterable(configuration.getNodes())) {
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
                            log.warn("Unsupported property '{}' on nodetype '{}'", HippoNodeType.HIPPOSYS_SUBTYPE, nodeTypeName);
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

    private Node getDocumentNode(Session session, Document document) {
        if (document == null || document.getIdentity() == null) {
            log.error("Cannot retrieve workflow for null document");
            return null;
        }
        try {
            return document.getNode(session);
        } catch (ItemNotFoundException e) {
            log.error("Document not found {}", e.getMessage());
        } catch (RepositoryException e) {
            log.error("Unexpected error {}", e.getMessage(), e);
        }
        return null;
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
        WorkflowDefinition workflowDefinition =  getWorkflowDefinition(category, getDocumentNode(userSession, document));
        if (workflowDefinition != null) {
            return new WorkflowDescriptorImpl(this, category, workflowDefinition, document);
        }
        log.debug("Workflow for category " + category + " on " + document.getIdentity() + " is not available");
        return null;
    }

    public Workflow getWorkflow(WorkflowDescriptor descriptor) throws RepositoryException {
        WorkflowDescriptorImpl descriptorImpl = (WorkflowDescriptorImpl) descriptor;
        try {
            Node node = userSession.getNodeByIdentifier(descriptorImpl.getUuid());
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
        log.debug("Workflow for category {} on {} is not available", category, item != null ? item.getPath() : null);
        return null;
    }

    public Workflow getWorkflow(String category, Document document) throws RepositoryException {
        return getWorkflow(category, getDocumentNode(userSession, document));
    }

    private Workflow createProxiedWorkflow(WorkflowDefinition definition, Node subject) throws RepositoryException {
        final Workflow workflow = createWorkflow(subject, definition);
        final InvocationHandler handler = new WorkflowInvocationHandler(definition, workflow, subject);
        final Class[] interfaces = getRemoteInterfaces(workflow.getClass());
        return createWorkflowProxy(workflow.getClass(), interfaces, handler);
    }

    private Workflow createWorkflow(Node item, WorkflowDefinition workflowDefinition) throws RepositoryException {
        if (workflowDefinition == null) {
            return null;
        }

        String uuid = item.getIdentifier();
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
                                new WorkflowContextImpl(workflowDefinition, item.getSession(), item), userSession, rootSession, item);
                        break;
                    } else if (params.length == 3 && Session.class.isAssignableFrom(params[0])
                            && Session.class.isAssignableFrom(params[1])
                            && Node.class.isAssignableFrom(params[2])) {
                        workflow = (Workflow) constructor.newInstance(getSession(), rootSession, item);
                        break;
                    }
                }
                if (workflow == null) {
                    throw new RepositoryException("No valid constructor found for " + clazz.getName());
                }
            } catch (IllegalAccessException | InstantiationException ex) {
                throw new RepositoryException("Workflow class [" + clazz.getName() + "] instantiation exception", ex);
            } catch (InvocationTargetException e) {
                throw new RepositoryException("Workflow class [" + clazz.getName() + "] instantiation exception", e.getCause());
            }
        } else if (Workflow.class.isAssignableFrom(clazz)) {
            try {
                workflow = clazz.newInstance();
            } catch (InstantiationException | IllegalAccessException ex) {
                throw new RepositoryException("Workflow class [" + clazz.getName() + "] instantiation exception", ex);
            }
        } else {
            throw new RepositoryException("Class is not of type Workflow [" + clazz.getName() + "]");
        }
        if (WorkflowImpl.class.isAssignableFrom(clazz)) {
            Node rootSessionNode = rootSession.getNodeByIdentifier(uuid);
            ((WorkflowImpl) workflow).setWorkflowContext(new WorkflowContextImpl(workflowDefinition, item.getSession(), item));
            ((WorkflowImpl) workflow).setNode(rootSessionNode);
        }
        return workflow;
    }

    @SuppressWarnings("unchecked")
    private Workflow createWorkflowProxy(final Class<? extends Workflow> workflowClass, final Class[] interfaces, final InvocationHandler handler) throws RepositoryException {
        try {
            Class proxyClass = Proxy.getProxyClass(workflowClass.getClassLoader(), interfaces);
            return (Workflow) proxyClass.getConstructor(new Class[]{InvocationHandler.class}).newInstance(handler);
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException ex) {
            throw new RepositoryException("Unable to create proxy for workflow", ex);
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

    public void close() {
        if (rootSession != null && rootSession.isLive()) {
            rootSession.logout();
            rootSession = null;
        }
    }

    private class WorkflowInvocationHandler implements InvocationHandler {
        private final String category;
        private final String workflowName;
        private final Workflow upstream;
        private final String subjectId;
        private final String subjectPath;
        private final boolean objectPersist;

        private WorkflowInvocationHandler(WorkflowDefinition definition, Workflow upstream, Node subject) throws RepositoryException {
            this.category = definition.getCategory();
            this.workflowName = definition.getName();
            this.upstream = upstream;
            this.subjectId = subject.getIdentifier();
            this.subjectPath = subject.getPath();
            this.objectPersist = upstream instanceof WorkflowImpl;
        }

        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            Object returnObject = null;
            Throwable exception = null;

            if ("toString".equals(method.getName()) && (args == null || args.length == 0)) {
                return "WorkflowInvocationHandler[" + category + ", " + workflowName + "]";
            }

            boolean resetInteraction = false;
            String interaction = tlInteraction.get();
            String interactionId = tlInteractionId.get();
            if (interactionId == null) {
                interactionId = UUID.randomUUID().toString();
                tlInteractionId.set(interactionId);
                interaction = category + ":" + workflowName + ":" + method.getName();
                tlInteraction.set(interaction);
                resetInteraction = true;
            }
            Method targetMethod = null;
            try {
                targetMethod = upstream.getClass().getMethod(method.getName(), method.getParameterTypes());
                returnObject = targetMethod.invoke(upstream, args);
                if (objectPersist && !targetMethod.getName().equals("hints")) {
                    rootSession.save();
                }
                if (returnObject instanceof Document) {
                    // only return a simple Document instance
                    returnObject = new Document((Document)returnObject);
                }
                return returnObject;
            } catch (NoSuchMethodException | IllegalAccessException e) {
                throw exception = new RepositoryException("Failed to execute workflow action " + interaction, e);
            } catch (InvocationTargetException e) {
                exception = e.getCause();
                if (exception instanceof RepositoryException) {
                    rootSession.refresh(false);
                }
                throw exception;
            } catch (RepositoryException e) {
                rootSession.refresh(false);
                throw e;
            } finally {
                if (resetInteraction) {
                    tlInteraction.remove();
                    tlInteractionId.remove();
                }
                WorkflowAction wfActionAnno = AnnotationUtils.findMethodAnnotation(targetMethod, WorkflowAction.class);
                if (wfActionAnno == null || wfActionAnno.loggable()) {
                    logWorkflowStep(targetMethod, args, returnObject, interaction, interactionId, exception);
                }
            }
        }

        private void logWorkflowStep(final Method targetMethod, final Object[] args, final Object returnObject,
                                     final String interaction, final String interactionId, final Throwable exception) {
            final String className = targetMethod != null ? targetMethod.getDeclaringClass().getName() : null;
            final String methodName = targetMethod != null ? targetMethod.getName() : null;
            workflowLogger.logWorkflowStep(userSession.getUserID(), className,
                    methodName, args, returnObject, subjectId, subjectPath, interaction, interactionId,
                    category, workflowName, exception);
        }

    }

    private class WorkflowContextImpl implements WorkflowContext {
        private final Session subjectSession;
        private final WorkflowDefinition workflowDefinition;
        private final Node subject;

        private WorkflowContextImpl(WorkflowDefinition workflowDefinition, Session subjectSession, Node subject) {
            this.workflowDefinition = workflowDefinition;
            this.subjectSession = subjectSession;
            this.subject = subject;
        }

        @Override
        public Workflow getWorkflow(String category, final Document document) throws WorkflowException, RepositoryException {
            return WorkflowManagerImpl.this.getWorkflow(category, getDocumentNode(rootSession, document));
        }

        @Override
        public Workflow getWorkflow(String category) throws WorkflowException, RepositoryException {
            return WorkflowManagerImpl.this.getWorkflow(category, subject);
        }

        @Override
        public String getUserIdentity() {
            return userSession.getUserID();
        }

        @Override
        public Session getUserSession() {
            return userSession;
        }

        @Override
        public Node getSubject() {
            return subject;
        }

        @Override
        public Session getSubjectSession() {
            return subjectSession;
        }

        @Override
        public Session getInternalWorkflowSession() {
            return rootSession;
        }

        @Override
        public RepositoryMap getWorkflowConfiguration() {
            return workflowDefinition.getWorkflowConfiguration();
        }

        @Override
        public String getInteraction() {
            return tlInteraction.get();
        }

        @Override
        public String getInteractionId() {
            return tlInteractionId.get();
        }
    }
}
