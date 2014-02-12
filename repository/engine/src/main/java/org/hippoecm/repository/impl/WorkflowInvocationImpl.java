/*
 *  Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.version.VersionException;

import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.ext.WorkflowImpl;
import org.hippoecm.repository.ext.WorkflowInvocation;
import org.onehippo.repository.api.annotation.WorkflowAction;
import org.onehippo.repository.util.AnnotationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.repository.util.RepoUtils.PRIMITIVE_TO_OBJECT_TYPES;

public class WorkflowInvocationImpl implements WorkflowInvocation {

    static final Logger log = LoggerFactory.getLogger(WorkflowInvocationImpl.class);

    private WorkflowManager workflowManager;
    private WorkflowDefinition workflowDefinition;
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


    WorkflowInvocationImpl(WorkflowManager workflowManager, WorkflowDefinition workflowDefinition, Session rootSession, Document workflowSubject, Method method, Object[] args) throws RepositoryException {
        this(workflowDefinition.getCategory(), workflowDefinition.getName(), workflowSubject.getIdentity(), method.getName(), method.getParameterTypes(),
                (args != null ? args.clone() : null), WorkflowManagerImpl.INTERACTION_ID.get(), WorkflowManagerImpl.INTERACTION.get());
        this.workflowManager = workflowManager;
        this.workflowDefinition = workflowDefinition;
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

    WorkflowInvocationImpl(WorkflowManager workflowManager, WorkflowDefinition workflowDefinition, Session rootSession, Node workflowSubject, Method method, Object[] args) throws RepositoryException {
        this(workflowDefinition.getCategory(), workflowDefinition.getName(), workflowSubject.getIdentifier(), method.getName(), method.getParameterTypes(),
                (args != null ? args.clone() : null), WorkflowManagerImpl.INTERACTION_ID.get(), WorkflowManagerImpl.INTERACTION.get());
        this.workflowManager = workflowManager;
        this.workflowDefinition = workflowDefinition;
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
            workflowManager = ((HippoWorkspace) node.getSession().getWorkspace()).getWorkflowManager();
        } catch (RepositoryException ex) {
            log.error(ex.getClass().getName() + ": " + ex.getMessage(), ex);
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
        for (int methodIndex = 0; methodIndex < methods.length && method == null; ++methodIndex) {
            if (!methods[methodIndex].getName().equals(methodName)) {
                continue;
            }
            Class[] types = methods[methodIndex].getParameterTypes();
            if (types.length != parameterTypes.length) {
                continue;
            }
            method = methods[methodIndex];
            for (int i = 0; i < types.length; i++) {
                Class<?> type = types[i];
                if (type.isPrimitive()) {
                    type = PRIMITIVE_TO_OBJECT_TYPES.get(type);
                }
                if (!type.equals(parameterTypes[i])) {
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
        } catch (IllegalAccessException ex) {
            log.debug(ex.getMessage(), ex);
            throw new RepositoryException(ex.getMessage(), ex);
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

            Node item = workflowSubjectNode;
            if (item == null) {
                item = manager.rootSession.getNodeByUUID(workflowSubject.getIdentity());
            }
            String path = item.getPath();
            String userId = item.getSession().getUserID();
            postActions = WorkflowPostActionsImpl.createPostActions(manager, category, method, item.getIdentifier());
            workflow = manager.createWorkflow(item, workflowDefinition);
            boolean objectPersist = workflow instanceof WorkflowImpl;

            Method targetMethod = workflow.getClass().getMethod(method.getName(), method.getParameterTypes());
            synchronized (SessionDecorator.unwrap(manager.rootSession)) {
                Object returnObject = targetMethod.invoke(workflow, arguments);
                WorkflowAction wfActionAnno = AnnotationUtils.findMethodAnnotation(targetMethod, WorkflowAction.class);
                if (objectPersist && (wfActionAnno == null || wfActionAnno.mutates())) {
                    manager.rootSession.save();
                }
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
            log.error("Workflow specification corrupt on node " + workflowDefinition.getPath());
            throw new RepositoryException("workflow specification corrupt", ex);
        } catch (ValueFormatException ex) {
            log.error("Workflow specification corrupt on node " + workflowDefinition.getPath());
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
