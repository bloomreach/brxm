package org.onehippo.repository;

import javax.jcr.RepositoryException;
import org.hippoecm.repository.api.DocumentManager;
import org.hippoecm.repository.api.HierarchyResolver;
import org.hippoecm.repository.api.WorkflowManager;

/**
 * DO NOT USE, THIS INTERFACE IS NOT YET PART OF THE PUBLIC API.
 * @exclude
 */
public interface ManagerService {
    public DocumentManager getDocumentManager() throws RepositoryException;
    public WorkflowManager getWorkflowManager() throws RepositoryException;
    public HierarchyResolver getHierarchyResolver() throws RepositoryException;
    public void close();
}
