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

import java.util.HashSet;
import java.util.Set;

import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeDefinition;
import org.apache.jackrabbit.core.id.NodeId;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.jackrabbit.core.HierarchyManager;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.state.ItemStateManager;
import org.apache.jackrabbit.core.state.SessionItemStateManager;

public class ItemManager extends org.apache.jackrabbit.core.ItemManager {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static Logger log = LoggerFactory.getLogger(ItemManager.class);

    private NodeId rootNodeId;

    protected ItemManager(SessionItemStateManager sism,
                          HierarchyManager hierMgr,
                          SessionImpl session,
                          org.apache.jackrabbit.spi.commons.nodetype.NodeDefinitionImpl rootNodeDef,
                          NodeId rootNodeId) {
        super(sism, hierMgr, session, rootNodeDef, rootNodeId);
        this.rootNodeId = rootNodeId;
    }

    public static ItemManager createInstance(
            SessionItemStateManager itemStateProvider,
            HierarchyManager hierMgr,
            SessionImpl session,
            org.apache.jackrabbit.spi.commons.nodetype.NodeDefinitionImpl rootNodeDef,
            NodeId rootNodeId) {
        ItemManager mgr = new ItemManager(itemStateProvider, hierMgr,
                session, rootNodeDef, rootNodeId);
        itemStateProvider.addListener(mgr);
        return mgr;
    }
}
