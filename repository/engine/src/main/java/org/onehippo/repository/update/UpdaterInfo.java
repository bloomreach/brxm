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
package org.onehippo.repository.update;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.codehaus.groovy.control.CompilationFailedException;
import org.hippoecm.repository.util.JcrUtils;

import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyCodeSource;

/**
 * Encapsulates meta data for running an {@link Updater}
 */
class UpdaterInfo {

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
    private final Updater updater;

    /**
     * @param node  a node of type <code>hipposys:updaterinfo</code> carrying the meta data of the {@link Updater}
     * @throws IllegalArgumentException if the node is not of type <code>hipposys:updaterinfo</code>
     * or does not carry the property <code>hipposys:path</code> nor the property <code>hipposys:query</code>
     * or does not carry the <code>hipposys:script</code> nor the property <code>hipposys:class</code>
     * @throws IllegalAccessException if the {@link Updater} class could not be instantiated
     * @throws InstantiationException if the {@link Updater} class could not be instantiated
     * @throws ClassNotFoundException if the the updater class could not be found.
     * @throws RepositoryException if something went wrong while reading the node.
     */
    UpdaterInfo(Node node) throws IllegalArgumentException, RepositoryException, IllegalAccessException, InstantiationException, ClassNotFoundException, CompilationFailedException {
        if (!node.isNodeType("hipposys:updaterinfo")) {
            throw new IllegalArgumentException("Node must be of type hipposys:updaterinfo");
        }
        identifier = node.getIdentifier();
        name = node.getName();
        path = JcrUtils.getStringProperty(node, "hipposys:path", null);
        query = JcrUtils.getStringProperty(node, "hipposys:query", null);
        language = JcrUtils.getStringProperty(node, "hipposys:language", DEFAULT_QUERY_LANGUAGE);
        if ((path == null || path.isEmpty()) && (query == null || query.isEmpty())) {
            throw new IllegalArgumentException("Either path or query property must be present");
        }
        revert = JcrUtils.getBooleanProperty(node, "hipposys:revert", false);
        throttle = JcrUtils.getLongProperty(node, "hipposys:throttle", DEFAULT_THROTTLE);
        batchSize = JcrUtils.getLongProperty(node, "hipposys:batchsize", DEFAULT_BATCH_SIZE);
        dryRun = JcrUtils.getBooleanProperty(node, "hipposys:dryrun", false);
        startedBy = JcrUtils.getStringProperty(node, "hipposys:startedby", null);
        final String script = JcrUtils.getStringProperty(node, "hipposys:script", null);
        final String klass = JcrUtils.getStringProperty(node, "hipposys:class", null);
        if ((script == null || script.isEmpty()) && (klass == null || klass.isEmpty())) {
            throw new IllegalArgumentException("Either script or class property must be present");
        }
        Class clazz;
        if (klass != null && !klass.isEmpty()) {
            clazz = Class.forName(klass);
        } else {
            GroovyClassLoader gcl = GroovyUpdaterClassLoader.createClassLoader();
            GroovyCodeSource gcs = new GroovyCodeSource(script, "updater", "/hippo/updaters");
            clazz = gcl.parseClass(gcs, false);
        }
        Object o = clazz.newInstance();
        if (!(o instanceof Updater)) {
            throw new IllegalArgumentException("Class must implement " + Updater.class.getName());
        }
        updater = (Updater) o;
    }

    /**
     *
     * @param id  the unique identifier of this updater configuration in the updater registry
     * @param path  the path that should be visited
     * @param query  the query that should be visited
     * @param updater  the {@link Updater} to execute
     * @throws IllegalArgumentException if both <code>path</code> and <code>query</code> are undefined
     */
    UpdaterInfo(String id, String path, String query, boolean revert, Updater updater) throws IllegalArgumentException {
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
     * @return either <code>update</code> or <code>revert</code> depending on
     * the value of {@link #isRevert()}
     */
    String getMethod() {
        return isRevert() ? "revert" : "update";
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
     * The {@link Updater} to execute
     */
    Updater getUpdater() {
        return updater;
    }

    /**
     * @return the value of the <code>hipposys:startedby</code> property
     */
    String getStartedBy() {
        return startedBy;
    }
}
