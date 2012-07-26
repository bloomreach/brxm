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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.jackrabbit.core.config.ConfigurationException;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.spi.Name;
import org.hippoecm.repository.replication.Filter;
import org.hippoecm.repository.replication.ReplicatorContext;
import org.hippoecm.repository.replication.ReplicatorHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link PropertyFilter} filters nodes to replicate based on the properties of a node.
 */
public class PropertyFilter implements Filter {
    /** @exclude */

    /** Logger. */
    private static final Logger log = LoggerFactory.getLogger(PropertyFilter.class);

    /** The replicator helper. */
    private ReplicatorHelper helper;

    /** Whether to remove existing nodes in the remote repository that are excluded. */
    private boolean removeExisting = true;

    /**
     * The string containing the configuration parameter "exclude". which is a comma
     * separated list with "key=value" pairs.
     */
    private String exclude;

    /**
     * The {@link Map} of properties and values to exclude from replication.
     */
    private Map<String, String> excludedProperties = new HashMap<String, String>();

    public void destroy() {
        log.info("Destroyed.");
    }

    public boolean removeExistingExcludedNodes() {
        return removeExisting;
    }

    public void init(ReplicatorContext context) throws ConfigurationException {
        this.helper = new ReplicatorHelper(context);
        parseExcludeString();
        log.info("Initialized.");
    }

    public void parseExcludeString() {
        log.info("Setting paths to replicate to: {}", getExclude());
        Set<String> keyValues = ReplicatorHelper.explodeCommaSeparated(getExclude());
        for (String keyValue : keyValues) {
            excludedProperties.putAll(ReplicatorHelper.explodeKeyValue(keyValue));
        }
    }

    public boolean isNodeExcluded(NodeId id) {
        if (matchExcludedProperties(id)) {
            log.debug("Node exclude by filter for replication: {}", id);
            return true;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private boolean matchExcludedProperties(NodeId id) {
        NodeState state = helper.getNodeState(id);
        if (state == null) {
            return false;
        }
        Set<Name> propNames = state.getPropertyNames();
        for (Name propName : propNames) {
            if (matchExcludedProperty(id, propName)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchExcludedProperty(NodeId id, Name propName) {
        PropertyState propState = helper.getPropertyState(id, propName);
        if (propState == null) {
            return false;
        }
        for (Map.Entry<String, String> exProp : excludedProperties.entrySet()) {
            if (matchNameAndValue(propState, exProp.getKey(), exProp.getValue())) {
                return true;
            }
        }
        return false;
    }

    private boolean matchNameAndValue(PropertyState propState, String name, String value) {
        String propName = helper.getJCRName(propState.getName());
        if (name.equals(propName)) {
            if (matchValue(propState, value)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchValue(PropertyState propState, String value) {
        if (value == null) {
            // null means match all
            return true;
        }
        for (InternalValue val : propState.getValues()) {
            if (value.equals(val.toString())) {
                return true;
            }
        }
        return false;
    }

    //----------------------------------- Bean setters & getters

    public void setExclude(String exclude) {
        this.exclude = exclude;
    }

    public String getExclude() {
        return exclude;
    }

    public boolean isRemoveExisting() {
        return removeExisting;
    }

    public void setRemoveExisting(boolean removeExisting) {
        this.removeExisting = removeExisting;
    }

}
