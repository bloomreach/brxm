/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.replication;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.jcr.ItemNotFoundException;
import javax.jcr.NamespaceException;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.apache.jackrabbit.core.HierarchyManagerImpl;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.id.PropertyId;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.NoSuchItemStateException;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.value.ValueFormat;
import org.hippoecm.repository.replication.filters.PathFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link ReplicatorHelper} is a convenience class for dealing with item states and
 * some miscellaneous parsing methods.
 */
public class ReplicatorHelper {

    /** Logger. */
    private static final Logger log = LoggerFactory.getLogger(PathFilter.class);

    private final ReplicatorContext context;

    public ReplicatorHelper(ReplicatorContext context) {
        this.context = context;
    }

    /**
     * Try to find the JCR string representation of the name for the given {@link Name}.
     * @param name the {@link Name}
     * @return the string representation of the name or null when not found
     */
    public String getJCRName(Name name) {
        try {
            return context.getNamePathResolver().getJCRName(name);
        } catch (NamespaceException e) {
            log.warn("Error while trying to resolve name '{}' : {}.", name, e.getMessage());
            log.debug("Error trace: ", e);
            return null;
        }
    }

    /**
     * Try to find the JCR string representation of the path for the given {@link NodeId}.
     * @param id the {@link NodeId}
     * @return the string representation of the path or null when not found
     */
    public String getJCRPath(NodeId id) {
        try {
            return context.getNamePathResolver().getJCRPath(context.getHierarchyManager().getPath(id));
        } catch (NamespaceException e) {
            log.warn("Unable to resolve path name for NodeId '{}': {}.", id, e.getMessage());
        } catch (ItemNotFoundException e) {
            log.debug("Unable to find NodeId '{}': {}. Node is probably removed locally.", id, e.getMessage());
        } catch (RepositoryException e) {
            log.error("Error during the path lookup for NodeId '{}': {}", id, e.getMessage());
            log.debug("Error trace: ", e);
        }
        return null;
    }

    public Value[] getPropertyValues(PropertyState propState, SessionImpl targetSession) {
        InternalValue[] iValues = propState.getValues();
        Value[] values = new Value[propState.getValues().length];
        for (int i = 0; i < values.length; i++) {
            try {
                values[i] = ValueFormat.getJCRValue(iValues[i], targetSession, targetSession.getValueFactory());                        
            } catch (RepositoryException e) {
                log.warn("Unable to resolve jcr value of '{}': {}", values[i], e.getMessage());
            }
        }
        return values;
    }
    
    public String getSingleStringValue(NodeId id, Name propName) {
        PropertyState propState = getPropertyState(id, propName);
        if (propState == null) {
            return null;
        }
        if (propState.getValues().length > 0) {
            return propState.getValues()[0].toString();
        } else {
            return null;
        }
    }

    public String getNodeType(NodeId id) {
        NodeState state = getNodeState(id);
        if (state == null) {
            return null;
        }
        return getJCRName(state.getNodeTypeName());
    }
    
    public NodeState getNodeState(NodeId id) {
        if (id == null) {
            return null;
        }
        try {
            return (NodeState) context.getItemStateManager().getItemState(id);
        } catch (NoSuchItemStateException e) {
            log.debug("Unable to retrieve node state of node id '{}'. Node is probably removed locally.", id);
            return null;
        } catch (ItemStateException e) {
            log.warn("Error while trying to retrieve node state of node id '{}'.", id);
            log.debug("Error trace: ", e);
            return null;
        }
    }

    public PropertyState getPropertyState(NodeId id, Name propName) {
        PropertyId propId = new PropertyId(id, propName);
        try {
            return (PropertyState) context.getItemStateManager().getItemState(propId);
        } catch (NoSuchItemStateException e) {
            log.info("Unable to retrieve property state of node id '{}' with name '{}'.", id, propName);
            return null;
        } catch (ItemStateException e) {
            log.warn("Error while trying to retrieve property state of node id '{}' with name '{}'.", id, propName);
            log.debug("Error trace: ", e);
            return null;
        }
    }
    /**
     * Helper method to get the node {@link Name} from the {@link NodeState}.
     * 
     * @param state the {@link NodeState}
     * @return the node's {@link Name}
     * @throws FatalReplicationException
     */
    public Name getNodeName(NodeState state) {
        try {
            return context.getHierarchyManager().getName(state.getNodeId());
        } catch (ItemNotFoundException e) {
            log.warn("Unable to find node name for node id '{}': {}", state.getId(), e.getMessage());
        } catch (RepositoryException e) {
            log.warn("Unable to find node name for node id '{}': {}", state.getId(), e.getMessage());
            log.debug("Error trace: ", e);
        }
        return null;
    }
    
    public boolean isRootNodeId(NodeId id) {
        return ((HierarchyManagerImpl) context.getHierarchyManager()).getRootNodeId().equals(id);
    }

    //----------------------------------- Miscellaneous

    /**
     * Parse a comma separated string to a {@link Set} of {@link String}s.
     * @param commaSeparated The comma separated string.
     * @return the {@link Set} of {@link String}s
     */
    public static Set<String> explodeCommaSeparated(String commaSeparated) {
        Set<String> pathSet = new HashSet<String>();
        if (commaSeparated == null) {
            return pathSet;
        }

        String paths = commaSeparated.trim();
        if (paths.length() < 1) {
            return pathSet;
        }

        int pos = 0;
        int comma = 0;
        while (pos < paths.length()) {
            comma = paths.indexOf(',', pos);
            if (comma < 0) {
                // last one
                comma = paths.length();
            }
            String path = paths.substring(pos, comma).trim();
            pathSet.add(path);
            pos = comma + 1;
        }
        return pathSet;
    }

    public static Map<String, String> explodeKeyValue(String keyValue) {
        Map<String, String> kvmap = new HashMap<String, String>(1);
        int pos = keyValue.indexOf('=') + 1;
        if (pos < 1) {
            kvmap.put(keyValue.trim(), null);
        } else if (pos == keyValue.length()) {
            // strip trailing '='
            kvmap.put(keyValue.substring(0, keyValue.length() - 1).trim(), null);
        } else {
            String key = keyValue.substring(0, pos - 1).trim();
            String value = keyValue.substring(pos).trim();
            kvmap.put(key, value);
        }
        return kvmap;
    }
}
