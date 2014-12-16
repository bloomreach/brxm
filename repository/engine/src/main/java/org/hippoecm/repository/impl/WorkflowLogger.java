/*
 *  Copyright 2012-2014 Hippo B.V. (http://www.onehippo.com)
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

import java.util.Arrays;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.util.NodeIterable;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.eventbus.HippoEventBus;
import org.onehippo.repository.events.HippoWorkflowEvent;
import org.onehippo.repository.security.SecurityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.repository.api.HippoNodeType.NT_HANDLE;

public class WorkflowLogger {

    private static final Logger log = LoggerFactory.getLogger(WorkflowLogger.class);

    private final Session session;

    public WorkflowLogger(Session session) throws RepositoryException {
        this.session = session;
    }

    public void logWorkflowStep(final String userName, final String className, final String methodName, final Object[] args,
                                final Object returnObject, final String subjectId, final String subjectPath,
                                final String interaction, String interactionId,
                                final String category, final String workflowName, final Throwable exception) {
        HippoEventBus eventBus = HippoServiceRegistry.getService(HippoEventBus.class);
        if (eventBus != null) {
            final HippoWorkflowEvent event = new HippoWorkflowEvent();
            final String returnValue = getReturnValue(returnObject);
            final String handleUuid = getHandleUuid(subjectId);
            final String returnType = getReturnType(returnObject);
            final String[] arguments = replaceObjectsWithStrings(args);
            final Boolean system = isSystemUser(userName);
            final String documentType = getDocumentType(subjectId);
            event.user(userName).action(methodName).result(returnValue).system(system);
            event.className(className).methodName(methodName).handleUuid(handleUuid).subjectId(subjectId)
                    .returnType(returnType).returnValue(returnValue).documentPath(subjectPath).subjectPath(subjectPath)
                    .interactionId(interactionId).interaction(interaction).workflowCategory(category)
                    .workflowName(workflowName).exception(exception).documentType(documentType);
            if (arguments != null) {
                event.arguments(Arrays.asList(arguments));
            }
            eventBus.post(event);
        }

    }

    private Boolean isSystemUser(String userName) {
        try {
            final SecurityService securityService = ((HippoWorkspace) session.getWorkspace()).getSecurityService();
            return securityService.hasUser(userName) && securityService.getUser(userName).isSystemUser();
        } catch (ItemNotFoundException ignore) {
            // If hasUser returns true, we expect getUser to return a user, so it is very unlikely that
            // securityService.getUser(userName) will throw this exception
        } catch (RepositoryException e) {
            log.error("Failed to determine system status of event", e);
        }
        return null;
    }

    private String getHandleUuid(final String subjectId) {
        if (subjectId == null) {
            return null;
        }
        try {
            final Node node = session.getNodeByIdentifier(subjectId);
            if (node.isNodeType(HippoNodeType.NT_HANDLE)) {
                return node.getIdentifier();
            }
            if (node.getParent().isNodeType(HippoNodeType.NT_HANDLE)) {
                return node.getParent().getIdentifier();
            }
        } catch (RepositoryException ignore) {
        }
        return null;
    }

    private String getDocumentType(final String subjectId) {
        if (subjectId == null) {
            return null;
        }
        try {
            final Node subject = session.getNodeByIdentifier(subjectId);
            if (subject.isNodeType(NT_HANDLE)) {
                for (Node child : new NodeIterable(subject.getNodes())) {
                    if (child.getName().equals(subject.getName())) {
                        return child.getPrimaryNodeType().getName();
                    }
                }
            } else if (subject.getParent().isNodeType(NT_HANDLE)) {
                return subject.getPrimaryNodeType().getName();
            }
        } catch (RepositoryException ignore) {
        }
        return null;
    }

    private String getReturnValue(Object returnObject) {
        if (returnObject == null) {
            return null;
        }
        if (returnObject instanceof Document) {
            Document document = (Document) returnObject;
            StringBuilder sb = new StringBuilder();
            sb.append("document[uuid=");
            sb.append(document.getIdentity());
            if (document.getIdentity() != null) {
                sb.append(",path='");
                try {
                    sb.append(document.getNode(session).getPath());
                } catch (RepositoryException e) {
                    sb.append("error:").append(e.getMessage());
                }
            }
            sb.append("']");
            return sb.toString();
        } else {
            return returnObject.toString();
        }
    }

    private static String getReturnType(Object returnObject) {
        if (returnObject == null) {
            return null;
        }
        if (returnObject instanceof Document) {
            return "document";
        } else {
            return returnObject.getClass().getName();
        }
    }

    private static String[] replaceObjectsWithStrings(Object[] args) {
        if (args == null) {
            return null;
        }
        String[] arguments = new String[args.length];
        for (int i = 0; i < args.length; i++) {
            if (args[i] != null) {
                arguments[i] = args[i].toString();
            } else {
                arguments[i] = "<null>";
            }
        }
        return arguments;
    }

}
