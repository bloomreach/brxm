/*
 *  Copyright 2010 Hippo.
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
package org.hippoecm.repository.replication.filters;

import java.util.HashSet;
import java.util.Set;

import org.apache.jackrabbit.core.config.ConfigurationException;
import org.apache.jackrabbit.core.id.NodeId;
import org.hippoecm.repository.replication.Filter;
import org.hippoecm.repository.replication.ReplicatorContext;
import org.hippoecm.repository.replication.ReplicatorHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link PathFilter} filters which nodes to replicate based on the path of the  
 */
public class PathFilter implements Filter {

    /** Logger. */
    private static final Logger log = LoggerFactory.getLogger(PathFilter.class);

    /** The replicator helper. */
    private ReplicatorHelper helper;

    /** The string containing the configuration parameter "replicate". which is a comma separated list. */
    private String replicate;

    /** The string containing the configuration parameter "exclude". which is a comma separated list. */
    private String exclude;

    /** The {@link Set} of paths to replicate. */
    private Set<String> replicatePaths = new HashSet<String>();

    /** The {@link Set} of paths to exclude from replication. */
    private Set<String> excludePaths = new HashSet<String>();

    /** Whether to remove existing nodes in the remote repository that are excluded. */
    private boolean removeExisting = false;

    /**
     * {@inheritDoc}
     */
    public void destroy() {
        log.debug("Destroyed.");
    }

    /**
     * {@inheritDoc}
     */
    public boolean removeExistingExcludedNodes() {
        return removeExisting;
    }

    /**
     * {@inheritDoc}
     */
    public void init(ReplicatorContext context) throws ConfigurationException {
        this.helper = new ReplicatorHelper(context);
        setReplicatePaths();
        setExcludePaths();
        log.info("Initialized.");
    }

    /**
     * {@inheritDoc}
     */
    public boolean isNodeExcluded(NodeId id) {
        String path = helper.getJCRPath(id);
        if (path == null) {
            return true;
        }
        if (isPathExcluded(path)) {
            return true;
        }
        if (!isPathReplicated(path)) {
            return true;
        }
        return false;
    }

    /**
     * Check if the path matches any of the paths to replicate.
     * @param path the path to check
     * @return true if the path matches a path which has to be replicated.
     */
    private boolean isPathReplicated(String path) {
        for (String replPath : replicatePaths) {
            if (path.startsWith(replPath)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if the path matches any of the paths to exclude from replication.
     * @param path the path to check
     * @return true if the path matches a path which has to be excluded.
     */
    private boolean isPathExcluded(String path) {
        for (String exclPath : excludePaths) {
            if (path.startsWith(exclPath)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Set the Set of absolute paths to replicate.
     */
    public void setReplicatePaths() {
        log.info("Setting paths to replicate to: {}", getReplicate());
        replicatePaths = ReplicatorHelper.explodeCommaSeparated(getReplicate());
    }

    /**
     * Set the Set of absolute paths that should be excluded from replication.
     */
    public void setExcludePaths() {
        log.info("Setting paths to exclude to: {}", getExclude());
        excludePaths = ReplicatorHelper.explodeCommaSeparated(getExclude());
    }

    //----------------------------------- Bean setters & getters

    public void setExclude(String exclude) {
        this.exclude = exclude;
    }

    public String getExclude() {
        return exclude;
    }

    public void setReplicate(String replicate) {
        this.replicate = replicate;
    }

    public String getReplicate() {
        return replicate;
    }

    public boolean isRemoveExisting() {
        return removeExisting;
    }

    public void setRemoveExisting(boolean removeExisting) {
        this.removeExisting = removeExisting;
    }

}
