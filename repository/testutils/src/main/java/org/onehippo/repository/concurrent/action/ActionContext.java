/*
 *  Copyright 2011-2014 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.repository.concurrent.action;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.standardworkflow.FolderWorkflow;
import org.slf4j.Logger;

public final class ActionContext {

    private static final String ASSET_BASE_PATH = "/content/assets";
    private static final String DOCUMENT_BASE_PATH = "/content/documents";

    private final Session session;
    private final Logger log;
    private final String documentBasePath;
    private final String assetBasePath;
    private final Map<Class<? extends Action>, Action> actions = new HashMap<>();

    public ActionContext(Session session, Logger log) {
        this.session = session;
        this.log = log;
        this.documentBasePath = DOCUMENT_BASE_PATH + "/" + getClusterNodeId();
        this.assetBasePath = ASSET_BASE_PATH + "/" + getClusterNodeId();
    }

    public void start() throws Exception {
        try {
            getDocumentBase();
        } catch (PathNotFoundException e) {
            final Node documents = session.getNode(DOCUMENT_BASE_PATH);
            getFolderWorkflow(documents).add("new-folder", "hippostd:folder", getClusterNodeId());
        }
        try {
            getAssetBase();
        } catch (PathNotFoundException e) {
            final Node assets = session.getNode(ASSET_BASE_PATH);
            getFolderWorkflow(assets).add("new-file-folder", "asset gallery", getClusterNodeId());
        }
    }

    public void stop() throws Exception {
        new Thread(new Runnable() {
            @Override
            public void run() {
                session.logout();
            }
        }).start();
    }

    public Session getSession() {
        return session;
    }

    public String getDocumentBasePath() {
        return documentBasePath;
    }
    
    public String getAssetBasePath() {
        return assetBasePath;
    }

    public Node getDocumentBase() throws RepositoryException {
        return session.getNode(documentBasePath);
    }

    public Node getAssetBase() throws RepositoryException {
        return session.getNode(assetBasePath);
    }

    public boolean isBasePath(String path) {
        return path.equals(documentBasePath) || path.equals(assetBasePath);
    }

    public Action getAction(Class<? extends Action> actionClass) {
        if (!actions.containsKey(actionClass)) {
            actions.put(actionClass, createAction(actionClass));
        }
        return actions.get(actionClass);
    }

    public Logger getLog() {
        return log;
    }

    private Action createAction(final Class<? extends Action> actionClass) {
        try {
            return actionClass.getConstructor(ActionContext.class).newInstance(this);
        } catch (Exception e) {
            log.error("Failed to instantiate Action: " + e.toString());
            return null;
        }
    }

    public String getClusterNodeId() {
        String clusteNodeId = session.getRepository().getDescriptor("jackrabbit.cluster.id");
        if (clusteNodeId == null) {
            clusteNodeId = "default";
        }
        return clusteNodeId;
    }

    public FolderWorkflow getFolderWorkflow(final Node folder) throws RepositoryException {
        return (FolderWorkflow) getWorkflowManager().getWorkflow("internal", folder);
    }

    private WorkflowManager getWorkflowManager() throws RepositoryException {
        return ((HippoWorkspace) session.getWorkspace()).getWorkflowManager();
    }
}
