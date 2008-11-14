/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.repository.jackrabbit.version;

import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.version.InternalFrozenVersionHistory;
import org.apache.jackrabbit.core.version.InternalVersion;
import org.apache.jackrabbit.core.version.InternalVersionHistory;
import org.apache.jackrabbit.core.version.InternalVersionItem;
import org.apache.jackrabbit.core.version.NodeStateEx;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.name.NameConstants;

import javax.jcr.RepositoryException;
import javax.jcr.version.VersionException;
import org.apache.jackrabbit.core.version.VersionManager;

/**
 * Implements a <code>InternalFrozenVersionHistory</code>
 */
class InternalFrozenVHImpl extends InternalFreezeImpl
        implements InternalFrozenVersionHistory {

    /**
     * Creates a new frozen version history.
     *
     * @param node
     */
    public InternalFrozenVHImpl(HippoVersionManager vMgr, NodeStateEx node,
                                InternalVersionItem parent) {
        super(vMgr, node, parent);
    }


    /**
     * {@inheritDoc}
     */
    public Name getName() {
        return node.getName();
    }

    /**
     * {@inheritDoc}
     */
    public NodeId getId() {
        return node.getNodeId();
    }

    /**
     * {@inheritDoc}
     */
    public NodeId getVersionHistoryId() {
        return new NodeId(node.getPropertyValue(NameConstants.JCR_CHILDVERSIONHISTORY).getUUID());
    }

    /**
     * {@inheritDoc}
     */
    public InternalVersionHistory getVersionHistory()
            throws VersionException {
        try {
            return vMgr.getVersionHistory(getVersionHistoryId());
        } catch (RepositoryException e) {
            throw new VersionException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public NodeId getBaseVersionId() {
        return new NodeId(node.getPropertyValue(NameConstants.JCR_BASEVERSION).getUUID());
    }

    /**
     * {@inheritDoc}
     */
    public InternalVersion getBaseVesion()
            throws VersionException {
        try {
            InternalVersionHistory history = vMgr.getVersionHistory(getVersionHistoryId());
            return history.getVersion(getBaseVersionId());
        } catch (RepositoryException e) {
            throw new VersionException(e);
        }
    }
}
