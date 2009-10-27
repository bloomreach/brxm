/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.repository.jackrabbit;

import javax.jcr.RepositoryException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.uuid.UUID;

public class HippoNodeId extends NodeId
{
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private final static Logger log = LoggerFactory.getLogger(HippoNodeId.class);

    public NodeId parentId;
    public Name name;

    HippoVirtualProvider provider;

    public HippoNodeId(HippoVirtualProvider provider, NodeId parent, Name name) {
        super(UUID.randomUUID());
        this.provider = provider;
        parentId = parent;
        this.name = name;
    }

    public NodeState populate() {
        try {
            NodeState nodeState = provider.populate(this, parentId);
            return nodeState;
        } catch(RepositoryException ex) {
            log.error(ex.getClass().getName()+": "+ex.getMessage(), ex);
            return null;
        }
    }

    public NodeState populate(NodeState state) {
        try {
            return provider.populate(state);
        } catch(RepositoryException ex) {
            log.error(ex.getClass().getName()+": "+ex.getMessage(), ex);
            return null;
        }
    }
}
