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
package org.hippoecm.repository.standardworkflow;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.ext.InternalWorkflow;
import org.onehippo.cms7.event.HippoEvent;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.eventbus.HippoEventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// FIXME: this class has prior knowledge of the hippolog namespace

public class EventLoggerImpl implements EventLoggerWorkflow, InternalWorkflow {
    @SuppressWarnings("unused")
    private static final String SVN_ID = "$Id$";

    final Logger log = LoggerFactory.getLogger(Workflow.class);

    private boolean enabled = false;
    private Session rootSession;
    private Node logFolder;
    private String appender;
    private long maxSize;

    public EventLoggerImpl(Session userSession, Session rootSession, Node subject) throws RemoteException {
        this.rootSession = rootSession;
        if (subject != null) {
            try {
                logFolder = rootSession.getRootNode().getNode(subject.getPath().substring(1));
                enabled = logFolder.getProperty("hippolog:enabled").getBoolean();
                appender = logFolder.getProperty("hippolog:appender").getString();
                maxSize = logFolder.getProperty("hippolog:maxsize").getLong();
            } catch (RepositoryException ex) {
                log.error("Event logger configuration failed: " + ex.getMessage());
            }
        }
        if (!enabled) {
            log.info("Event logging to the repository disabled.");
        }
    }

    public EventLoggerImpl(Session rootSession) throws RemoteException, RepositoryException {
        this(rootSession, rootSession, getDefaultLogFolder(rootSession));
    }

    private static Node getDefaultLogFolder(Session rootSession) throws RepositoryException {
        if (rootSession.getRootNode().hasNode("hippo:log")) {
            return rootSession.getRootNode().getNode("hippo:log");
        }
        return null;
    }

    public Map<String, Serializable> hints() {
        return new TreeMap<String, Serializable>();
    }

    public void logEvent(String who, String className, String methodName) {
        postMessage(who, className, methodName);
        addLogNode(who, className, methodName);
    }

    public void logWorkflowStep(String who, String className, String methodName, Object[] args, Object returnObject, String documentPath) {
        String returnType = getReturnType(returnObject);
        String returnValue = getReturnValue(returnObject);
        String[] arguments = replaceObjectsWithStrings(args);

        postEvent(who, className, methodName, documentPath, returnType, returnValue, arguments);
        addLogNode(who, className, methodName, documentPath, returnType, returnValue, arguments);
    }

    private String getReturnValue(Object returnObject) {
        if (returnObject != null) {
            if (returnObject instanceof Document) {
                StringBuffer sb = new StringBuffer();
                Document document = (Document) returnObject;
                sb.append("document[uuid=");
                sb.append(document.getIdentity());
                if (document.getIdentity() != null) {
                    sb.append(",path='");
                    try {
                        sb.append(rootSession.getNodeByIdentifier(document.getIdentity()).getPath());
                    } catch (RepositoryException e) {
                        sb.append("error:").append(e.getMessage());
                    }
                }
                sb.append("']");
                return sb.toString();
            } else if (returnObject != null) {
                return returnObject.toString();
            }

        }
        return null;
    }

    private String getReturnType(Object returnObject) {
        if (returnObject != null) {
            if (returnObject instanceof Document) {
                return "document";
            } else if (returnObject != null) {
                return returnObject.getClass().getName();
            }
        }
        return null;
    }

    private String[] replaceObjectsWithStrings(Object[] args) {
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

    private void postMessage(String who, String className, String methodName) {
        postEvent(who, className, methodName, null, null, null, null);
    }

    private void postEvent(String who, String className, String methodName, String documentPath, String returnType, String returnValue, String[] arguments) {
        HippoEventBus eventBus = HippoServiceRegistry.getService(HippoEventBus.class);
        if (eventBus != null) {
            HippoEvent event = new HippoEvent("repository");
            event.user(who).action(className + "." + methodName).result(returnValue);
            event.set("className", className).set("methodName", methodName);
            event.set("returnType", returnType).set("returnValue", returnValue).set("documentPath", documentPath);
            if (arguments != null) {
                event.set("arguments", Arrays.asList(arguments));
            }
            eventBus.post(event);
        }
    }

    private void addLogNode(String who, String className, String methodName) {
        addLogNode(who, className, methodName, null, null, null, null);
    }

    private void addLogNode(String who, String className, String methodName, String documentPath, String returnType, String returnValue, String[] arguments) {
        if (!enabled) {
            return;
        }
        try {
            applyAppender();
            long timestamp = System.currentTimeMillis();
            Node logNode = logFolder.addNode(String.valueOf(timestamp), "hippolog:item");
            logNode.setProperty("hippolog:timestamp", timestamp);
            logNode.setProperty("hippolog:eventUser", who == null ? "null" : who);
            logNode.setProperty("hippolog:eventClass", className == null ? "null" : className);
            logNode.setProperty("hippolog:eventMethod", methodName == null ? "null" : methodName);

            // conditional properties
            if (documentPath != null) {
                logNode.setProperty("hippolog:eventDocument", documentPath);
            }
            if (returnType != null) {
                logNode.setProperty("hippolog:eventReturnType", returnType);
                logNode.setProperty("hippolog:eventReturnValue", returnValue);
            }
            if (arguments != null) {
                logNode.setProperty("hippolog:eventArguments", arguments);
            }
            logFolder.save();
        } catch (RepositoryException ex) {
            log.warn("Event logging failed: " + ex.getMessage(), ex);
            try {
                logFolder.refresh(false);
            } catch (RepositoryException ex2) {
                log.error("Event logging fails in failure: " + ex2.getMessage(), ex2);
            }
        }

    }

    private void applyAppender() {
        if (appender.equals("folding")) {
            log.warn("Folding appender not implemented yet, falling back to rolling appender");
        }
        try {
            logFolder.refresh(false);
            NodeIterator logNodes = logFolder.getNodes();
            if (logNodes.getSize() > maxSize) {
                long count = logNodes.getSize() - maxSize;
                while (logNodes.hasNext()) {
                    Node logEntry = logNodes.nextNode();
                    if (logEntry != null) {
                        try {
                            logEntry.remove();
                            if (--count <= 0) {
                                break;
                            }
                        } catch (ItemNotFoundException ex) {
                            // item was apparently already deleted
                            --count;
                        }
                    }
                }
                logFolder.save();
            }
        } catch (RepositoryException ex) {
            /* normally the cause of this exception is a org.apache.jackrabbit.core.state.NoSuchItemStateException
             * indicating that the item has been deleted already.  There is no good way to detect this from
             * occurring and what other kind of error occurs. Therefore we will log only at debug level this problem.
             */
            log.debug("Event logging appender failed: " + ex.getClass().getName() + ": " + ex.getMessage(), ex);
            try {
                logFolder.refresh(false);
            } catch (RepositoryException ex2) {
                log.error("Event appender fails in failure: " + ex2.getMessage(), ex2);
            }
        }
    }

}
