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

import org.apache.jackrabbit.core.config.WorkspaceConfig;
import org.apache.jackrabbit.core.state.LocalItemStateManager;
import org.apache.jackrabbit.core.state.SharedItemStateManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XAWorkspaceImpl extends org.apache.jackrabbit.core.XAWorkspace {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static Logger log = LoggerFactory.getLogger(XAWorkspaceImpl.class);

    protected XAWorkspaceImpl(WorkspaceConfig wspConfig, SharedItemStateManager stateMgr,
            org.apache.jackrabbit.core.RepositoryImpl rep, XASessionImpl session) {
        super(wspConfig, stateMgr, rep, session);
    }

    @Override
    protected LocalItemStateManager createItemStateManager(SharedItemStateManager shared) {
        LocalItemStateManager mgr = new HippoLocalItemStateManager(shared, this, rep.getItemStateCacheFactory(), null, ((RepositoryImpl)rep).getNodeTypeRegistry(), ((RepositoryImpl)rep).isStarted(), ((RepositoryImpl)rep).getRootNodeId());
        shared.addListener(mgr);
        return mgr;
    }
}
