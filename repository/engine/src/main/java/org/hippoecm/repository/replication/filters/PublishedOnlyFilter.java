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

import java.util.Iterator;
import java.util.Set;

import org.apache.jackrabbit.core.config.ConfigurationException;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.state.ChildNodeEntry;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.spi.Name;
import org.hippoecm.repository.replication.Filter;
import org.hippoecm.repository.replication.ReplicatorContext;
import org.hippoecm.repository.replication.ReplicatorHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link PublishedOnlyFilter} filters nodes to replicate based on the properties of a node.
 */
public class PublishedOnlyFilter implements Filter {
    /** @exclude */

    /** Logger. */
    private static final Logger log = LoggerFactory.getLogger(PublishedOnlyFilter.class);

    /** The replicator helper. */
    private ReplicatorHelper helper;

    private static final String HARD_HANDLE = "hippo:hardhandle";
    private static final String STATE_PROPERTY = "hippostd:state";
    private static final String PUBLISHED_VALUE = "published";
    private static final String NT_REQUEST = "hippo:request";
    private static final String NT_REQUEST_PUBWF = "hippostdpubwf:request";
    private static final String NT_TRANSLATION = "hippo:translation";
    private static final String NT_SCHEDULED_JOB = "hipposched:job";

    public void destroy() {
        log.info("Destroyed.");
    }

    public boolean removeExistingExcludedNodes() {
        return true;
    }

    public void init(ReplicatorContext context) throws ConfigurationException {
        this.helper = new ReplicatorHelper(context);
        log.info("Initialized.");
    }

    public boolean isNodeExcluded(NodeId id) {
        if (!isInContentPath(id)) {
            return false;
        }

        if (isPublishable(id)) {
            if (isPublished(id)) {
                return false;
            } else {
                return true;
            }
        } else if (isHandle(id)) {
            if (handleContainsOnlyNonPublishedDocuments(id)) {
                // cleanup empty handles
                return true;
            } else {
                return false;
            }
        } else if (isRequest(id)) {
            return true;
        } else if (isScheduledJob(id)) {
            return true;
        } else {
            return false;
        }
    }

    private boolean isInContentPath(NodeId id) {
        String path = helper.getJCRPath(id);
        if (path != null && path.startsWith("/content")) {
            return true;
        }
        return false;
    }
    private boolean isPublishable(NodeId id) {
        String wfState = getWorkflowStateProperty(id);
        if (wfState != null) {
            return true;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private String getWorkflowStateProperty(NodeId id) {
        NodeState state = helper.getNodeState(id);
        if (state == null) {
            return null;
        }
        Set<Name> propNames = state.getPropertyNames();
        for (Name propName : propNames) {
            String name = helper.getJCRName(propName);
            if (STATE_PROPERTY.equals(name)) {
                return helper.getSingleStringValue(id, propName);
            }
        }
        return null;
    }

    private boolean isPublished(NodeId id) {
        String wfState = getWorkflowStateProperty(id);
        if (PUBLISHED_VALUE.equals(wfState)) {
            return true;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private boolean isHandle(NodeId id) {
        NodeState state = helper.getNodeState(id);
        if (state != null) {
            for (Name mixinName : (Set<Name>) state.getMixinTypeNames()) {
                String mixin = helper.getJCRName(mixinName);
                if (HARD_HANDLE.equals(mixin)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isRequest(NodeId id) {
        String type = helper.getNodeType(id);
        if (NT_REQUEST.equals(type)) {
            return true;
        }
        if (NT_REQUEST_PUBWF.equals(type)) {
            return true;
        }
        return false;
    }

    private boolean isTranslation(NodeId id) {
        String type = helper.getNodeType(id);
        if (NT_TRANSLATION.equals(type)) {
            return true;
        }
        return false;
    }

    private boolean isScheduledJob(NodeId id) {
        String type = helper.getNodeType(id);
        if (NT_SCHEDULED_JOB.equals(type)) {
            return true;
        }
        return false;
    }
    @SuppressWarnings("unchecked")
    private boolean handleContainsOnlyNonPublishedDocuments(NodeId id) {
        NodeState state = helper.getNodeState(id);
        Iterator<ChildNodeEntry> iter = state.getChildNodeEntries().iterator();
        while (iter.hasNext()) {
            ChildNodeEntry entry = iter.next();
            if (isRequest(entry.getId())) {
                continue;
            } else if (isTranslation(entry.getId())) {
                continue;
            } else if (isPublishable(entry.getId())) {
                if (isPublished(entry.getId())) {
                    return false;
                }
            } else {
                // handle contains something else
                return false;
            }
        }
        return true;
    }

}
