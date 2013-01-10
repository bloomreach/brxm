/*
 * Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *  http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.repository.remote;

import javax.jcr.RepositoryException;
import org.hippoecm.repository.api.DocumentManager;
import org.hippoecm.repository.api.HierarchyResolver;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.WorkflowManager;
import org.onehippo.repository.ManagerService;

public class HippoManagerServiceImpl implements ManagerService {
    HippoWorkspace workspace;

    public HippoManagerServiceImpl(HippoSession session) {
        workspace = (HippoWorkspace) session.getWorkspace();
    }

    public DocumentManager getDocumentManager() throws RepositoryException {
        return workspace.getDocumentManager();
    }

    public WorkflowManager getWorkflowManager() throws RepositoryException {
        return workspace.getWorkflowManager();
    }

    public HierarchyResolver getHierarchyResolver() throws RepositoryException {
        return workspace.getHierarchyResolver();
    }

    public void close() {
    }
}
