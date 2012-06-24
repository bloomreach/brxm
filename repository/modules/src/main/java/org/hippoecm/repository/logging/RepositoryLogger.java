/*
 *  Copyright 2012 Hippo.
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
package org.hippoecm.repository.logging;

import java.util.List;
import java.util.Random;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.repository.ext.DaemonModule;
import org.onehippo.cms7.event.HippoEvent;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.eventbus.HippoEventBus;
import org.onehippo.cms7.services.eventbus.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link HippoEventBus} listener that logs events in a random hierarchy of folders.
 * Each cluster has its own dedicated folder to avoid collisions (would be very rare) and to allow listeners
 * for events on specific clusters only. The event log can contain 5 million log entries easily per cluster node.
 * This means that for very active CMSes in large organisations with 2000 actions per day per cluster node you
 * should start thinking about purging your logs after about 7 years. {@link EventLogCleanupModule} can do
 * that for you.
 */
public class RepositoryLogger implements DaemonModule {

    private static final Logger log = LoggerFactory.getLogger(RepositoryLogger.class);

    private static final Random random = new Random();

    private static final String ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyz";
    private static final String DEFAULT_CLUSTER_NODE_ID = "default";
    private static final int HIERARCHY_DEPTH = 4;

    private Session session;
    private Node logFolder;

    @Override
    public void initialize(final Session session) throws RepositoryException {
        this.session = session;

        Node rootLogFolder;
        if (session.nodeExists("/hippo:log")) {
            rootLogFolder = session.getNode("/hippo:log");
        } else {
            throw new RepositoryException("No log to write to");
        }
        if (!rootLogFolder.isNodeType("hippolog:folder")) {
            throw new RepositoryException("Root log folder is not of the expected type");
        }
        String clusterId = getClusterNodeId();
        if (rootLogFolder.hasNode(clusterId)) {
            logFolder = rootLogFolder.getNode(clusterId);
        } else {
            logFolder = rootLogFolder.addNode(clusterId, "hippolog:folder");
            session.save();
        }

        final HippoEventBus eventBus = HippoServiceRegistry.getService(HippoEventBus.class);
        if (eventBus != null) {
            eventBus.register(this);
        }

    }

    @Subscribe
    public void logHippoEvent(HippoEvent event) {
        if (session == null) {
            return;
        }

        if (!"workflow".equals(event.category())) {
            return;
        }

        long timestamp = event.timestamp();
        String userName = event.user();
        String returnValue = event.result();
        String methodName = (String) event.get("methodName");
        String className = (String) event.get("className");
        String documentPath = (String) event.get("documentPath");
        String returnType = (String) event.get("returnType");
        List<String> arguments = (List<String>) event.get("arguments");

        try {
            char[] randomChars = generateRandomCharArray(HIERARCHY_DEPTH);
            Node folder = getOrCreateFolder(charArrayToRelPath(randomChars, HIERARCHY_DEPTH - 1));

            Node logNode = folder.addNode(String.valueOf(randomChars[HIERARCHY_DEPTH - 1]), "hippolog:item");
            logNode.setProperty("hippolog:timestamp", timestamp);
            logNode.setProperty("hippolog:eventUser", userName == null ? "null" : userName);
            logNode.setProperty("hippolog:eventClass", className == null ? "null" : className);
            logNode.setProperty("hippolog:eventMethod", methodName == null ? "null" : methodName);
            // conditional properties
            if (documentPath != null) {
                logNode.setProperty("hippolog:eventDocument", documentPath);
            }
            if (returnType != null) {
                logNode.setProperty("hippolog:eventReturnType", returnType);
            }
            if (returnValue != null) {
                logNode.setProperty("hippolog:eventReturnValue", returnValue);
            }
            if (arguments != null) {
                logNode.setProperty("hippolog:eventArguments", arguments.toArray(new String[arguments.size()]));
            }
            session.save();
        } catch (RepositoryException e) {
            String strEvent = "userName: " + userName + "; eventMethod: " + methodName + "; eventClass: " + className;
            log.warn("Logging of event " + strEvent + " failed: " + e.getMessage(), e);
            try {
                session.refresh(false);
            } catch (RepositoryException ex) {
                log.error("Event logging failed in failure", ex);
            }
        }

    }

    private Node getOrCreateFolder(String itemRelPath) throws RepositoryException {
        if (!logFolder.hasNode(itemRelPath)) {
            if (itemRelPath.length() > 1) {
                getOrCreateFolder(itemRelPath.substring(0, itemRelPath.lastIndexOf('/')));
            }
            return logFolder.addNode(itemRelPath, "hippolog:folder");
        }
        return logFolder.getNode(itemRelPath);
    }


    private String getClusterNodeId() {
        String clusteNodeId = session.getRepository().getDescriptor("jackrabbit.cluster.id");
        if (clusteNodeId == null) {
            clusteNodeId = DEFAULT_CLUSTER_NODE_ID;
        }
        return clusteNodeId;
    }

    private static String charArrayToRelPath(char[] chars, int len) {
        StringBuilder sb = new StringBuilder((2*len)-1);
        for (int i = 0; i < len - 1; i++) {
            sb.append(chars[i]).append('/');
        }
        sb.append(chars[len-1]);
        return sb.toString();
    }

    private static char[] generateRandomCharArray(int len) {
        char[] result = new char[len];
        for (int i = 0; i < len; i++) {
            result[i] = ALPHABET.charAt(random.nextInt(ALPHABET.length()));
        }
        return result;
    }

    @Override
    public void shutdown() {
        final HippoEventBus eventBus = HippoServiceRegistry.getService(HippoEventBus.class);
        if (eventBus != null) {
            eventBus.unregister(this);
        }
    }
}
