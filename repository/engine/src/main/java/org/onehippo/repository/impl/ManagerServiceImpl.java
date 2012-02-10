package org.onehippo.repository.impl;

import javax.jcr.LoginException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import org.hippoecm.repository.HierarchyResolverImpl;
import org.hippoecm.repository.api.DocumentManager;
import org.hippoecm.repository.api.HierarchyResolver;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.impl.DocumentManagerImpl;
import org.hippoecm.repository.impl.WorkflowManagerImpl;
import org.onehippo.repository.ManagerService;

public class ManagerServiceImpl implements ManagerService {
    Session session, rootSession;
    DocumentManagerImpl documentManager = null;
    WorkflowManagerImpl workflowManager = null;
    HierarchyResolver hierarchyResolver = null;

    public ManagerServiceImpl(Session session) {
        this.session = session;
    }

    public DocumentManager getDocumentManager() throws RepositoryException {
        if (documentManager == null) {
            documentManager = new DocumentManagerImpl(session);
        }
        return documentManager;
    }

    public WorkflowManager getWorkflowManager() throws RepositoryException {
        try {
            if (workflowManager == null) {
                rootSession = session.impersonate(new SimpleCredentials("workflowuser", new char[] {}));
                workflowManager = new WorkflowManagerImpl(session, rootSession);
            }
            return workflowManager;
        } catch (LoginException ex) {
            throw ex;
        } catch (RepositoryException ex) {
            throw ex;
        }
    }

    @Override
    public HierarchyResolver getHierarchyResolver() throws RepositoryException {
        if (hierarchyResolver == null) {
            hierarchyResolver = new HierarchyResolverImpl();
        }
        return hierarchyResolver;
    }

    @Override
    public void close() {
        if (workflowManager != null) {
            workflowManager.close();
        }
        if (documentManager != null) {
            documentManager.close();
        }
        if (rootSession != null) {
            rootSession.logout();
        }
        session = rootSession = null;
        workflowManager = null;
        hierarchyResolver = null;
    }
}
