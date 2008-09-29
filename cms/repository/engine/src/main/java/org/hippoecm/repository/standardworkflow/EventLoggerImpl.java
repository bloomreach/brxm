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

import java.rmi.RemoteException;
import java.util.NoSuchElementException;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.ext.InternalWorkflow;
import org.hippoecm.repository.standardworkflow.EventLoggerWorkflow;

public class EventLoggerImpl implements EventLoggerWorkflow, InternalWorkflow {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    final Logger log = LoggerFactory.getLogger(Workflow.class);

    private boolean enabled = false;
    private Node logFolder;
    private String appender;
    private long maxSize;

    public EventLoggerImpl(Session userSession, Session rootSession, Node subject) throws RemoteException {
        try {
            logFolder = rootSession.getRootNode().getNode(subject.getPath().substring(1));
            enabled = logFolder.getProperty("hippolog:enabled").getBoolean();
            appender = logFolder.getProperty("hippolog:appender").getString();
            maxSize = logFolder.getProperty("hippolog:maxsize").getLong();
        } catch(RepositoryException ex) {
            enabled = false;
            log.error("Event logger configuration failed: " + ex.getMessage());
        }
        if (!enabled) {
            log.info("Event logging disabled, workflow steps will not be logged");
        }
    }

    public EventLoggerImpl(Session rootSession) throws RemoteException, RepositoryException {
        this(rootSession, rootSession, rootSession.getRootNode().getNode("hippo:log"));
    }

    public void logWorkflowStep(String who, String className, String methodName, Object[] args, Object returnObject, String documentPath) {
        if (enabled) {
            try {
                applyAppender();
                long timestamp = System.currentTimeMillis();

                Node logNode = logFolder.addNode(String.valueOf(timestamp), "hippolog:item");
                if (logFolder.hasNodes()) {
                    Node firstNode = logFolder.getNodes().nextNode();
                    logFolder.orderBefore(logNode.getName(), firstNode.getName());
                }

                logNode.setProperty("hippo:timestamp", timestamp);
                logNode.setProperty("hippo:eventUser", who == null ? "null" : who);
                logNode.setProperty("hippo:eventClass", className == null ? "null" : className);
                logNode.setProperty("hippo:eventMethod", methodName == null ? "null" : methodName);

                if (args != null) {
                    String[] arguments = new String[args.length];
                    for (int i = 0; i < args.length; i++) {
                        arguments[i] = args[i].toString();
                    }
                    logNode.setProperty("hippo:eventArguments", arguments);
                }

                if (returnObject != null) {
                    logNode.setProperty("hippo:eventReturnType", returnObject.getClass().getName());
                    logNode.setProperty("hippo:eventReturnValue", returnObject.toString());
                }

                if (documentPath != null) {
                    logNode.setProperty("hippo:eventDocument", documentPath);
                }

                logFolder.save();
                logFolder.refresh(true);

            } catch (RepositoryException e) {
                log.error("Event logging failed: " + e.getMessage(), e);
            }
        } else {
            log.info("Event log: [" + who + " -> " + className + "." + methodName + "]");
        }
    }

    public void logEvent(String who, String className, String methodName) {
        if (enabled) {
            try {
                applyAppender();
                long timestamp = System.currentTimeMillis();

                Node logNode = logFolder.addNode(String.valueOf(timestamp), "hippolog:item");
                if (logFolder.hasNodes()) {
                    Node firstNode = logFolder.getNodes().nextNode();
                    logFolder.orderBefore(logNode.getName(), firstNode.getName());
                }

                logNode.setProperty("hippo:timestamp", timestamp);
                logNode.setProperty("hippo:eventUser", who == null ? "null" : who);
                logNode.setProperty("hippo:eventClass", className == null ? "null" : className);
                logNode.setProperty("hippo:eventMethod", methodName == null ? "null" : methodName);

                logFolder.save();
                logFolder.refresh(true);

            } catch (RepositoryException e) {
                log.warn("Event logging failed: [" + who + " -> " + className + "." + methodName + "] : " + e.getMessage());
            }
        } else {
            log.info("Event log: [" + who + " -> " + className + "." + methodName + "]");
        }
    }


    private void applyAppender() throws RepositoryException {
        if (appender.equals("folding")) {
            log.warn("Folding appender not implemented yet, falling back to rolling appender");
        }
        try {
            NodeIterator logNodes = logFolder.getNodes();
            if (logNodes.getSize() > maxSize) {
                logNodes.skip(maxSize);
                while (logNodes.hasNext()) {
                    Node toBeRemoved = logNodes.nextNode();
                    toBeRemoved.remove();
                }
            }
        } catch (NoSuchElementException e) {
            throw new RepositoryException("Skipped past last element in logFolder", e);
        }
    }
}
