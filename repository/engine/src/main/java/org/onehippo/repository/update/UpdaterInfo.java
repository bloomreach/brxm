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
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

import org.apache.tika.io.IOUtils;
import org.apache.xmlbeans.impl.common.IOUtil;
import org.codehaus.groovy.control.CompilationFailedException;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyCodeSource;

/**
 * Encapsulates meta data for running an {@link NodeUpdateVisitor}
 */
class UpdaterInfo {

    private static final Logger log = LoggerFactory.getLogger(UpdaterInfo.class);

    private static final int DEFAULT_THROTTLE = 1000;
    private static final int DEFAULT_BATCH_SIZE = 10;
    private static final String DEFAULT_QUERY_LANGUAGE = "xpath";

    private final String identifier;
    private final String name;
    private final String path;
    private final String query;
    private final String language;
    private final boolean revert;
    private final long throttle;
    private final long batchSize;
    private final boolean dryRun;
    private final String startedBy;
    private final NodeUpdateVisitor updater;
    private final Binary updatedNodes;

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
    UpdaterInfo(Node node) throws IllegalArgumentException, RepositoryException, IllegalAccessException, InstantiationException, ClassNotFoundException, CompilationFailedException {
        if (!node.isNodeType("hipposys:updaterinfo")) {
            throw new IllegalArgumentException("Node must be of type hipposys:updaterinfo");
        }
        identifier = node.getIdentifier();
        name = node.getName();
        path = JcrUtils.getStringProperty(node, HippoNodeType.HIPPOSYS_PATH, null);
        query = JcrUtils.getStringProperty(node, HippoNodeType.HIPPOSYS_QUERY, null);
        language = JcrUtils.getStringProperty(node, HippoNodeType.HIPPOSYS_LANGUAGE, DEFAULT_QUERY_LANGUAGE);

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
        Class clazz;
        if (klass != null && !klass.isEmpty()) {
            clazz = Class.forName(klass);
        } else {
            final GroovyClassLoader gcl = GroovyUpdaterClassLoader.createClassLoader();
            final GroovyCodeSource gcs = new GroovyCodeSource(script, "updater", "/hippo/updaters");
            clazz = gcl.parseClass(gcs, false);
        }
        final Object o = clazz.newInstance();
        if (!(o instanceof NodeUpdateVisitor)) {
            throw new IllegalArgumentException("Class must implement " + NodeUpdateVisitor.class.getName());
        }
        updater = (NodeUpdateVisitor) o;
        updatedNodes = JcrUtils.getBinaryProperty(node, HippoNodeType.HIPPOSYS_UPDATED, null);
    }

    /**
     *
     * @param id  the unique identifier of this updater configuration in the updater registry
     * @param path  the path that should be visited
     * @param query  the query that should be visited
     * @param updater  the {@link NodeUpdateVisitor} to execute
     * @throws IllegalArgumentException if both <code>path</code> and <code>query</code> are undefined
     */
    UpdaterInfo(String id, String path, String query, boolean revert, NodeUpdateVisitor updater) throws IllegalArgumentException {
        this.identifier = id;
        this.name = id;
        this.updater = updater;
        this.path = path;
        this.query = query;
        this.language = DEFAULT_QUERY_LANGUAGE;
        this.revert = revert;
        this.throttle = DEFAULT_THROTTLE;
        this.batchSize = DEFAULT_BATCH_SIZE;
        this.dryRun = false;
        this.startedBy = null;
        if ((path == null || path.isEmpty()) && (query == null || query.isEmpty())) {
            throw new IllegalArgumentException("Either path or query property must be defined");
        }
        this.updatedNodes = null;
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
