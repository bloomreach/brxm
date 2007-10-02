/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.repository.jackrabbit;

import javax.security.auth.Subject;

import javax.jcr.AccessDeniedException;
import javax.jcr.RepositoryException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.jackrabbit.core.HierarchyManager;
import org.apache.jackrabbit.core.ItemManager;
import org.apache.jackrabbit.core.config.WorkspaceConfig;
import org.apache.jackrabbit.core.security.AuthContext;
import org.apache.jackrabbit.core.state.LocalItemStateManager;
import org.apache.jackrabbit.core.state.SessionItemStateManager;
import org.apache.jackrabbit.core.state.SharedItemStateManager;

import org.hippoecm.repository.FacetedNavigationEngine;

public class SessionImpl extends org.apache.jackrabbit.core.SessionImpl implements HippoSession {
    private static Logger log = LoggerFactory.getLogger(SessionImpl.class);

    protected SessionImpl(RepositoryImpl rep, AuthContext loginContext, WorkspaceConfig wspConfig)
            throws AccessDeniedException, RepositoryException {
        super(rep, loginContext, wspConfig);
    }

    protected SessionImpl(RepositoryImpl rep, Subject subject, WorkspaceConfig wspConfig) throws AccessDeniedException,
            RepositoryException {
        super(rep, subject, wspConfig);
    }

    protected SessionItemStateManager createSessionItemStateManager(LocalItemStateManager manager) {
        return new HippoSessionItemStateManager(((RepositoryImpl) rep).getRootNodeId(), manager, this);
    }

    protected WorkspaceImpl createWorkspaceInstance(WorkspaceConfig wspConfig, SharedItemStateManager stateMgr,
            org.apache.jackrabbit.core.RepositoryImpl rep, org.apache.jackrabbit.core.SessionImpl session) {
        return new WorkspaceImpl(wspConfig, stateMgr, rep, session);
    }

    protected ItemManager createItemManager(SessionItemStateManager itemStateMgr, HierarchyManager hierMgr) {
        return super.createItemManager(itemStateMgr, hierMgr);
        /* return new ItemManager(itemStateMgr, hierMgr, this,
           ntMgr.getRootNodeDefinition(), rep.getRootNodeId());
         */
    }

    protected FacetedNavigationEngine.Context facetedContext;

    public void setFacetedNavigationContext(FacetedNavigationEngine.Context context) {
        facetedContext = context;
    }

    public FacetedNavigationEngine.Context getFacetedNavigationContext() {
        return facetedContext;
    }
}
