/*
 * Copyright 2019 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.repository.jackrabbit;

import java.util.Collection;

import org.apache.jackrabbit.core.cluster.ClusterException;
import org.apache.jackrabbit.core.cluster.ClusterNode;
import org.apache.jackrabbit.core.cluster.NamespaceEventListener;
import org.apache.jackrabbit.core.cluster.NodeTypeEventListener;
import org.apache.jackrabbit.core.cluster.PrivilegeEventListener;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.PrivilegeDefinition;
import org.apache.jackrabbit.spi.QNodeTypeDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>HippoClusterNode extends the jackrabbit ClusterNode to suppress (ignore) Namespace, NodeType and Privilege
 * cluster events during <b>only</b> the startup (catchup) synchronization of a cluster node, when the registries
 * for those events are stored in the database.</p>
 * <p>
 * <br/>Those database persisted registries are <b>not</b> cluster node specific but shared across the whole cluster,
 * and therefore do not, and <b>SHOULD NOT</b> need to be (re)updated during an initial startup cluster sync, as those
 * 'events' already have been persisted.</p>
 * <p>Normally,'replaying' such change events, and in particular for NodeType changes, are not problematic, as already
 * applied 'trivial' (forwards compatible) node type changes can/will be ignored as resulting in zero changes.</p>
 * <p>But non-trivial node type changes like node type or property/child removals <b>may</b> cause problems when
 * replayed <b>after</b> (first) a trivial node type change event is 'replayed'.</p>
 * <p>
 * <br/>A NodeTypeRecord in the Journal contains the <b>full</b> definition of a changed nodetype at the time the change
 * was executed. Now imagine the following sequence of nodetype changes on a nodetype 'foo' with property 'a':
 * <ol>
 *     <li>first a trivial nodetype change is executed, like adding property 'b'</li>
 *     <li>then a non-trivial nodetype change is executed, the removal of property 'a'
 *     (using HippoNodeTypeRegistry#ignoreNextConflictingContent()</li>
 * </ol>
 * The persisted resulting nodetype 'foo' will then only have property 'b'.</p>
 * <p>Now when replaying <b>both</b> the NodeTypeRecords for these two changes on a different clusterNode results in
 * the following (during startup):
 * <ol>
 *     <li>First the NodeTypeRegistry is loaded from the database, containing nodetype 'foo' with only property 'b'</li>
 *     <li>Then the first (trivial) NodeTypeRecord for adding property 'b' will be replayed. And because the full nodetype
 *     for 'foo' <b>as it was that point in time</b>, the nodetype 'foo' will be updated by <b>(re)adding</b> property 'a'!
 *     </li>
 *     <li>Now, the second (non-trivial) NodeTypeRecord for removing property 'a' will be attempted, and <b>fail</b>
 *     because now HippoNodeTypeRegistry#ignoreNextConflictingContext() will <b>not be set!</b></li>
 *     <li>The second NodeTypeRecord failure will be logged, but otherwise ignored</li>
 *     <li>The end result however is that the removal of property 'a' has been <b>undone!</b></li>
 * </ol></p>
 * <p>The real cause of this problem is that the Jackrabbit ClusterNode is replaying these journal 'events' against a
 * single/shared persisted NodeType Registry stored in the database. This <b>is</b> needed for 'live' events broadcasted
 * to running cluster nodes, but clearly not needed and shouldn't be done during startup cluster synchronization.</p>
 * <p>
 * <br/>The HippoClusterNode provides special handling to suppress/ignore these specifics events (only) during
 * the startup synchronization of a cluster node, if/when the registries are persisted in the database.</p>
 */
public class HippoClusterNode extends ClusterNode {

    private final static Logger log = LoggerFactory.getLogger(HippoClusterNode.class);

    private static final NodeTypeEventListener startupSyncIgnoringNodeTypeEventsListener = new NodeTypeEventListener() {
        @Override
        public void externalRegistered(final Collection<QNodeTypeDefinition> collection) {
            log.debug("Ignoring nodeType changes from journal table event during cluster node startup sync");
        }
        @Override
        public void externalReregistered(final QNodeTypeDefinition qNodeTypeDefinition) {
            log.debug("Ignoring nodeType changes from journal table event during cluster node startup sync");
        }
        @Override
        public void externalUnregistered(final Collection<Name> collection) {
            log.debug("Ignoring nodeType changes from journal table event during cluster node startup sync");
        }
    };

    private static final NamespaceEventListener startupSyncIgnoringNamespaceEventsListener = new NamespaceEventListener() {
        @Override
        public void externalRemap(final String s, final String s1, final String s2) {
            log.debug("Ignoring namespace changes from journal table event during cluster node startup sync");
        }
    };

    private static final PrivilegeEventListener startupSyncIgnoringPrivilegeEventsListener = new PrivilegeEventListener() {
        @Override
        public void externalRegisteredPrivileges(final Collection<PrivilegeDefinition> collection) {
            log.debug("Ignoring privelege changes from journal table event during cluster node startup sync");
        }
    };

    private final boolean databaseRepositoryRegistries;
    private boolean startupSyncDone;
    private NodeTypeEventListener postStartupSyncNodeTypeEventListener;
    private NamespaceEventListener postStartupSyncNamespaceEventListener;
    private PrivilegeEventListener postStartupSyncPrivilegeEventListener;

    public HippoClusterNode(final boolean databaseRepositoryRegistries) {
        this.databaseRepositoryRegistries = databaseRepositoryRegistries;
    }

    @Override
    public void setListener(final NodeTypeEventListener listener) {
        if (!startupSyncDone && databaseRepositoryRegistries) {
            postStartupSyncNodeTypeEventListener = listener;
            super.setListener(startupSyncIgnoringNodeTypeEventsListener);
        } else {
            super.setListener(listener);
        }
    }

    @Override
    public void setListener(final NamespaceEventListener listener) {
        if (!startupSyncDone && databaseRepositoryRegistries) {
            postStartupSyncNamespaceEventListener = listener;
            super.setListener(startupSyncIgnoringNamespaceEventsListener);
        } else {
            super.setListener(listener);
        }
    }

    @Override
    public void setListener(final PrivilegeEventListener listener) {
        if (!startupSyncDone && databaseRepositoryRegistries) {
            postStartupSyncPrivilegeEventListener = listener;
            super.setListener(startupSyncIgnoringPrivilegeEventsListener);
        } else {
            super.setListener(listener);
        }
        super.setListener(listener);
    }

    @Override
    public void syncOnStartup() throws ClusterException {
        super.syncOnStartup();
        startupSyncDone = true;
        if (postStartupSyncNodeTypeEventListener != null) {
            super.setListener(postStartupSyncNodeTypeEventListener);
            postStartupSyncNodeTypeEventListener = null;
        }
        if (postStartupSyncNamespaceEventListener != null) {
            super.setListener(postStartupSyncNamespaceEventListener);
            postStartupSyncNamespaceEventListener = null;
        }
        if (postStartupSyncPrivilegeEventListener != null) {
            super.setListener(postStartupSyncPrivilegeEventListener);
            postStartupSyncPrivilegeEventListener = null;
        }
    }
}
