/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.dataprovider;

import java.util.UUID;
import javax.jcr.InvalidItemStateException;

import javax.jcr.RepositoryException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.spi.Name;

/**
 * THIS CLASS IS NOT PART OF THE PUBLIC API.  DO NOT USE.
 */
public class HippoNodeId extends NodeId
{

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(HippoNodeId.class);

    public NodeId parentId;
    public Name name;
    private final HippoVirtualProvider provider;
    private StateProviderContext context;
    
    public HippoNodeId(HippoVirtualProvider provider, NodeId parent, StateProviderContext context, Name name) {
        super(provider.getDataProviderContext().generateUuid(context, null));
        this.provider = provider;
        if(context != null) {
            this.context = context;
        } else if (parent instanceof HippoNodeId && ((HippoNodeId)parent).context != null) {
            this.context = ((HippoNodeId)parent).context;
        } else {
            this.context = null;
        }
        parentId = parent;
        this.name = name;
    }

    public NodeState populate(StateProviderContext context) {
        if (context == null) {
            context = this.context;
        } else if (this.context == null) {
            this.context = context;
        }
        try {
            NodeState nodeState = provider.populate(context, this, parentId);
            return nodeState;
        } catch (RepositoryException ex) {
            log.error(ex.getClass().getName() + ": " + ex.getMessage(), ex);
            return null;
        }
    }

    public final NodeState populate(StateProviderContext context, NodeState state) throws InvalidItemStateException {
        if (context == null) {
            context = this.context;
        } else if (this.context == null) {
            this.context = context;
        }
        if (context == null) {
            context = new StateProviderContext();
        }
        try {
            return provider.populate(context, state);
        } catch (InvalidItemStateException ex) {
            throw ex;
        } catch (RepositoryException ex) {
            log.error(ex.getClass().getName() + ": " + ex.getMessage(), ex);
            return null;
        }
    }

    public final NodeState populate(HippoVirtualProvider provider, NodeState state) throws InvalidItemStateException, ItemStateException {
        try {
            if (provider != null) {
                provider.populate((context != null ? context : new StateProviderContext()), state);
            } else {
                state = populate(context, state);
            }
        } catch (InvalidItemStateException ex) {
            throw ex;
        } catch (RepositoryException ex) {
            log.error(ex.getClass().getName() + ": " + ex.getMessage(), ex);
            throw new ItemStateException("Failed to populate node state", ex);
        }
        return state;
    }

    protected final HippoVirtualProvider getProvider() {
        return provider;
    }

}
