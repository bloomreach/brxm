package org.onehippo.repository.embeddedrmi;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import javax.jcr.LoginException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import org.hippoecm.repository.HierarchyResolverImpl;
import org.hippoecm.repository.api.DocumentManager;
import org.hippoecm.repository.api.HierarchyResolver;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.impl.DocumentManagerImpl;
import org.hippoecm.repository.impl.WorkflowManagerImpl;
import org.onehippo.repository.ManagerService;

public class ClientManagerService implements ManagerService {
    Session session;
    DocumentManager documentManager = null;
    WorkflowManager workflowManager = null;
    HierarchyResolver hierarchyResolver = null;
    RemoteManagerService remote = null;

    public ClientManagerService(RemoteManagerService remote, Session session) {
        this.remote = remote;
        this.session = session;
    }
    
    public ClientManagerService(String url, Session session) throws RemoteException {
        try {
            remote = (RemoteManagerService)Naming.lookup(url);
            this.session = session;
        } catch (MalformedURLException e) {
            throw new RemoteException("Malformed URL: " + url, e);
        } catch (NotBoundException e) {
            throw new RemoteException("No target found: " + url, e);
        } catch (ClassCastException e) {
            throw new RemoteException("Unknown target: " + url, e);
        }
    }

    public DocumentManager getDocumentManager() throws RepositoryException {
        if (documentManager == null) {
            try {
                UnicastRemoteObject.exportObject(remote);
                documentManager = new ClientDocumentManager(session, remote.getDocumentManager((String)session.getAttribute("sessionName")));
            } catch (RemoteException ex) {
                throw new RepositoryException("connection failure", ex);
            }
        }
        return documentManager;
    }

    public WorkflowManager getWorkflowManager() throws RepositoryException {
        try {
            if (workflowManager == null) {
                try {
                    workflowManager = new ClientWorkflowManager(session, remote.getWorkflowManager((String)session.getAttribute("sessionName")));
                } catch (RemoteException ex) {
                    throw new RepositoryException("connection failure", ex);
                }
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
        if (workflowManager instanceof WorkflowManagerImpl) {
            ((WorkflowManagerImpl)workflowManager).close();
        }
        if (documentManager instanceof DocumentManagerImpl) {
            ((DocumentManagerImpl)documentManager).close();
        }
        session = null;
        workflowManager = null;
        hierarchyResolver = null;
    }
}
