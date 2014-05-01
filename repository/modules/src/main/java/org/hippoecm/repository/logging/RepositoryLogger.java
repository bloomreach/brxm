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
package org.hippoecm.repository.logging;

import java.util.Calendar;
import java.util.Collection;
import java.util.Map;
import java.util.Random;

import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFactory;

import org.hippoecm.repository.api.NodeNameCodec;
import org.onehippo.cms7.event.HippoEvent;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.eventbus.HippoEventBus;
import org.onehippo.cms7.services.eventbus.Subscribe;
import org.onehippo.repository.modules.DaemonModule;
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
            log.warn("Events will not be logged in the repository: no log folder exists");
            return;
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

        HippoServiceRegistry.registerService(this, HippoEventBus.class);
    }

    @Subscribe
    public void logHippoEvent(HippoEvent event) {
        if (session == null) {
            return;
        }
        if (logFolder == null) {
            return;
        }

        try {
            final char[] randomChars = generateRandomCharArray(HIERARCHY_DEPTH);
            final Node folder = getOrCreateFolder(charArrayToRelPath(randomChars, HIERARCHY_DEPTH - 1));
            final Node logNode = folder.addNode(String.valueOf(randomChars[HIERARCHY_DEPTH - 1]), "hippolog:item");
            for (Object o : event.getValues().entrySet()) {
                Map.Entry<String, Object> entry = (Map.Entry<String, Object>) o;
                setProperty(logNode, getPropertyName(entry.getKey()), entry.getValue());
            }
            session.save();
        } catch (RepositoryException e) {
            log.warn("Logging of event {} failed", event, e);
            try {
                session.refresh(false);
            } catch (RepositoryException ex) {
                log.error("Event logging failed in failure", ex);
            }
        }


    }

    private String getPropertyName(final String key) {
        return "hippolog:" + NodeNameCodec.encode(key);
    }

    private void setProperty(final Node logNode, final String key, final Object value) throws RepositoryException {
        if (value == null) {
            return;
        }
        if (value instanceof Collection) {
            Collection collection = (Collection) value;
            if (collection.isEmpty()) {
                return;
            }
            final Object item = collection.iterator().next();
            int propertyType = getPropertyType(item);
            logNode.setProperty(key, getValues(collection, propertyType), propertyType);
        } else {
            int propertyType = getPropertyType(value);
            logNode.setProperty(key, getValue(value, propertyType), propertyType);
        }
    }

    private Value getValue(final Object value, final int propertyType) throws RepositoryException {
        final ValueFactory valueFactory = session.getValueFactory();
        switch (propertyType) {
            case PropertyType.STRING : return valueFactory.createValue(value.toString());
            case PropertyType.LONG : return valueFactory.createValue((Long) value);
            case PropertyType.BOOLEAN : return valueFactory.createValue((Boolean) value);
            case PropertyType.DATE : return valueFactory.createValue((Calendar) value);
        }
        return null;
    }

    private int getPropertyType(final Object next) {
        if (next instanceof Long) {
            return PropertyType.LONG;
        }
        if (next instanceof Calendar) {
            return PropertyType.DATE;
        }
        if (next instanceof Boolean) {
            return PropertyType.BOOLEAN;
        }
        return PropertyType.STRING;
    }

    private Value[] getValues(final Collection collection, final int propertyType) throws RepositoryException {
        Value[] values = new Value[collection.size()];
        int count = 0;
        final ValueFactory valueFactory = session.getValueFactory();
        for (Object o : collection) {
            switch (propertyType) {
                case PropertyType.STRING : values[count] = valueFactory.createValue(o.toString()); break;
                case PropertyType.BOOLEAN : values[count] = valueFactory.createValue((Boolean) o); break;
                case PropertyType.DATE : values[count] = valueFactory.createValue((Calendar) o); break;
                case PropertyType.LONG : values[count] = valueFactory.createValue((Long) o); break;
            }
        }
        return values;
    }

    private Node getOrCreateFolder(String itemRelPath) throws RepositoryException {
        if (!logFolder.hasNode(itemRelPath)) {
            if (itemRelPath.length() > 1) {
                getOrCreateFolder(itemRelPath.substring(0, itemRelPath.lastIndexOf('/')));
            }
            final Node descendantFolder = logFolder.addNode(itemRelPath, "hippolog:folder");
            if (log.isDebugEnabled()) {
                log.debug("Created folder " + descendantFolder.getPath());
            }
            return descendantFolder;
        }
        return logFolder.getNode(itemRelPath);
    }


    private String getClusterNodeId() {
        String clusterNodeId = session.getRepository().getDescriptor("jackrabbit.cluster.id");
        if (clusterNodeId == null) {
            clusterNodeId = DEFAULT_CLUSTER_NODE_ID;
        }
        return clusterNodeId;
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
        HippoServiceRegistry.unregisterService(this, HippoEventBus.class);
    }
}
