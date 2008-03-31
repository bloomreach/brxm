/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.repository;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.Workflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventLogger {
    final Logger log = LoggerFactory.getLogger(Workflow.class);

    private boolean enabled = false;
    private Node logFolder;
    private String appender;
    private long maxSize;

    public EventLogger(Session session) {
        try {
            String configPath = HippoNodeType.CONFIGURATION_PATH + "/" + HippoNodeType.LOGGING_PATH;
            if (session.itemExists("/" + configPath)) {
                Node configuration = session.getRootNode().getNode(configPath);
                enabled = configuration.getProperty(HippoNodeType.HIPPO_LOGENABLED).getBoolean();
                if (enabled) {
                    appender = configuration.getProperty(HippoNodeType.HIPPO_LOGAPPENDER).getString();
                    maxSize = configuration.getProperty(HippoNodeType.HIPPO_LOGMAXSIZE).getLong();

                    String logPath = configuration.getProperty(HippoNodeType.HIPPO_LOGPATH).getString();
                    if (!session.itemExists(logPath)) {
                        String logParentPath = logPath.substring(0, logPath.lastIndexOf("/"));
                        logParentPath = logParentPath.length() == 0 ? "/" : logParentPath;
                        if (session.itemExists(logParentPath)) {
                            Node logParent = (Node) session.getItem(logParentPath);
                            String logFolderName = logPath.substring(logPath.lastIndexOf("/") + 1, logPath.length());
                            logParent.addNode(logFolderName, HippoNodeType.NT_LOGFOLDER);
                            logParent.save();
                            log.info("Event logging configuration: created logging base node '" + logPath + "'");
                        } else {
                            enabled = false;
                            log.error("Event logging configuration failed: logging base node '" + logPath
                                    + "' does not exist.");
                            return;
                        }
                    }
                    logFolder = (Node) session.getItem(logPath);
                }                
            }
            if (!enabled) {
                log.info("Event logging disabled, workflow steps will not be logged");
            }
        } catch (RepositoryException e) {
            log.warn("Event logger configuration failed: " + e.getMessage());
        }
    }

    public void logEvent(String who, String className, String methodName, Object[] args, Object returnObject) {
        if (enabled) {
            try {
                applyAppender();
                long timestamp = System.currentTimeMillis();

                Node logNode = logFolder.addNode(String.valueOf(timestamp), "hippo:logitem");
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
                logFolder.save();
                logFolder.refresh(true);

            } catch (RepositoryException e) {
                log.error("Event logging failed: " + e.getMessage(), e);
            }
        }
    }

    private void applyAppender() throws RepositoryException {
        if (appender.equals("folding")) {
            log.warn("Folding appender not implemented yet, falling back to rolling appender");
        }
        NodeIterator logNodes = logFolder.getNodes();
        if (logNodes.getSize() > maxSize) {
            logNodes.skip(maxSize);
            while (logNodes.hasNext()) {
                Node toBeRemoved = logNodes.nextNode();
                toBeRemoved.remove();
            }
        }
    }
}
