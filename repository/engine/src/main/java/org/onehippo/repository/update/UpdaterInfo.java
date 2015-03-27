/*
 *  Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.repository.update;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.commons.io.IOUtils;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.RepoUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyCodeSource;

/**
 * Encapsulates meta data for running an {@link NodeUpdateVisitor}
 */
class UpdaterInfo {

    private static final Logger log = LoggerFactory.getLogger(UpdaterInfo.class);

    private static final long DEFAULT_THROTTLE = 1000;
    private static final long DEFAULT_BATCH_SIZE = 10;
    private static final String DEFAULT_QUERY_LANGUAGE = "xpath";

    private final String identifier;
    private final String name;
    private final String description;
    private final String path;
    private final String query;
    private final String language;

    /**
     * Parameters in JSON string
     */
    private final String parameters;

    private final boolean revert;
    private final long throttle;
    private final long batchSize;
    private final boolean dryRun;
    private final String startedBy;
    private final NodeUpdateVisitor updater;
    private final Binary updatedNodes;
    private final String nodeType;
    private final Class<? extends NodeUpdateVisitor> updaterClass;

    /**
     * @param node  a node of type <code>hipposys:updaterinfo</code> carrying the meta data of the {@link NodeUpdateVisitor}
     * @throws IllegalArgumentException if the node is not of type <code>hipposys:updaterinfo</code>
     * or does not carry the property <code>hipposys:path</code> nor the property <code>hipposys:query</code>
     * or does not carry the <code>hipposys:script</code> nor the property <code>hipposys:class</code>
     * @throws IllegalAccessException if the {@link NodeUpdateVisitor} class could not be instantiated
     * @throws InstantiationException if the {@link NodeUpdateVisitor} class could not be instantiated
     * @throws ClassNotFoundException if the the updater class could not be found.
     * @throws RepositoryException if something went wrong while reading the node.
     */
    UpdaterInfo(Node node) throws Exception {
        if (!node.isNodeType("hipposys:updaterinfo")) {
            throw new IllegalArgumentException("Node must be of type hipposys:updaterinfo");
        }
        identifier = node.getIdentifier();
        name = node.getName();
        description = JcrUtils.getStringProperty(node, HippoNodeType.HIPPOSYS_DESCRIPTION, null);
        path = JcrUtils.getStringProperty(node, HippoNodeType.HIPPOSYS_PATH, null);
        String queryString = JcrUtils.getStringProperty(node, HippoNodeType.HIPPOSYS_QUERY, null);
        if (!Strings.isNullOrEmpty(queryString)) {
            queryString = RepoUtils.encodeXpath(queryString);
        }
        query = queryString;
        language = JcrUtils.getStringProperty(node, HippoNodeType.HIPPOSYS_LANGUAGE, DEFAULT_QUERY_LANGUAGE);
        parameters = JcrUtils.getStringProperty(node, HippoNodeType.HIPPOSYS_PARAMETERS, null);

        boolean hasPath = path != null && !path.isEmpty();
        boolean hasQuery = query != null && !query.isEmpty();
        if (!hasPath && !hasQuery) {
            throw new IllegalArgumentException("Either path or query property must be present, you specified neither");
        }
        if (hasPath && hasQuery) {
            throw new IllegalArgumentException("Either path or query property must be present, you specified both");
        }
        revert = JcrUtils.getBooleanProperty(node, HippoNodeType.HIPPOSYS_REVERT, false);
        throttle = JcrUtils.getLongProperty(node, HippoNodeType.HIPPOSYS_THROTTLE, DEFAULT_THROTTLE);
        batchSize = JcrUtils.getLongProperty(node, HippoNodeType.HIPPOSYS_BATCHSIZE, DEFAULT_BATCH_SIZE);
        dryRun = JcrUtils.getBooleanProperty(node, HippoNodeType.HIPPOSYS_DRYRUN, false);
        startedBy = JcrUtils.getStringProperty(node, HippoNodeType.HIPPOSYS_STARTEDBY, null);
        final String script = JcrUtils.getStringProperty(node, HippoNodeType.HIPPOSYS_SCRIPT, null);
        final String klass = JcrUtils.getStringProperty(node, HippoNodeType.HIPPOSYS_CLASS, null);
        if ((script == null || script.isEmpty()) && (klass == null || klass.isEmpty())) {
            throw new IllegalArgumentException("Either script or class property must be present");
        }
        if (klass != null && !klass.isEmpty()) {
            updaterClass = (Class<? extends NodeUpdateVisitor>) Class.forName(klass);
        } else {
            final GroovyClassLoader gcl = GroovyUpdaterClassLoader.createClassLoader();
            final GroovyCodeSource gcs = new GroovyCodeSource(script, "updater", "/hippo/updaters");
            updaterClass = gcl.parseClass(gcs, false);
        }
        if (!NodeUpdateVisitor.class.isAssignableFrom(updaterClass)) {
            throw new IllegalArgumentException("Class must implement " + NodeUpdateVisitor.class.getName());
        }

        final Object o = updaterClass.newInstance();
        updater = (NodeUpdateVisitor) o;
        updatedNodes = JcrUtils.getBinaryProperty(node, HippoNodeType.HIPPOSYS_UPDATED, null);
        nodeType = JcrUtils.getStringProperty(node, HippoNodeType.HIPPOSYS_NODETYPE, null);
    }

    /**
     * The unique identifier of this updater configuration
     */
    String getIdentifier() {
        return identifier;
    }

    /**
     * The human readable name of this updater
     */
    String getName() {
        return name;
    }

    /**
     * The description of this updater
     */
    String getDescription() {
        return name;
    }

    /**
     * The path that should be visited
     */
    String getPath() {
        return path;
    }

    /**
     * The query that returns the nodes that should be visited
     */
    String getQuery() {
        return query;
    }

    /**
     * The language of the query, if any. Defaults to 'xpath'
     */
    String getLanguage() {
        return language;
    }

    /**
     * The parameters as JSON string.
     */
    String getParameters() {
        return parameters;
    }

    /**
     * Whether a revert must be done instead of an update
     */
    boolean isRevert() {
        return revert;
    }

    /**
     * @return either <code>doUpdate</code> or <code>undoUpdate</code> depending on
     * the value of {@link #isRevert()}
     */
    String getMethod() {
        return isRevert() ? "doUpdate" : "undoUpdate";
    }

    /**
     * Number of milliseconds to sleep after running through one batch
     */
    long getThrottle() {
        return throttle;
    }

    /**
     * How many items to visit before saving and throttling
     */
    long getBatchSize() {
        return batchSize;
    }

    /**
     * Whether to save changes made during this updater run or to discard them
     */
    boolean isDryRun() {
        return dryRun;
    }

    /**
     * The {@link NodeUpdateVisitor} to execute
     */
    NodeUpdateVisitor getUpdater() {
        return updater;
    }

    /**
     * @return the value of the <code>hipposys:startedby</code> property
     */
    String getStartedBy() {
        return startedBy;
    }

    /**
     * @return the node type the updater updates
     */
    String getNodeType() {
        return nodeType;
    }

    Class<? extends NodeUpdateVisitor> getUpdaterClass() {
        return updaterClass;
    }

    Iterator<String> getUpdatedNodes() {
        if (updatedNodes != null) {
            try {
                final BufferedReader reader = new BufferedReader(new InputStreamReader(updatedNodes.getStream()));
                return new Iterator<String>() {

                    private String next;
                    private boolean closed;

                    private void fetchNext() {
                        if (next == null && !closed) {
                            try {
                                next = reader.readLine();
                            } catch (IOException e) {
                                log.error("Failed to read next line of updated paths", e);
                            }
                            if (next == null) {
                                IOUtils.closeQuietly(reader);
                                closed = true;
                            }
                        }
                    }

                    @Override
                    public boolean hasNext() {
                        fetchNext();
                        return next != null;
                    }

                    @Override
                    public String next() {
                        if (hasNext()) {
                            final String result = next;
                            next = null;
                            return result;
                        }
                        throw new NoSuchElementException();
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            } catch (RepositoryException e) {
                log.error("Failed to read updated nodes property", e);
            }
        }
        return Collections.<String>emptyList().iterator();
    }
}
