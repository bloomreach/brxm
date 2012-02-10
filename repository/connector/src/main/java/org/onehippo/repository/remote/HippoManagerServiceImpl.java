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
