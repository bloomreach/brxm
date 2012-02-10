package org.onehippo.repository.embeddedrmi;

import java.rmi.Remote;
import java.rmi.RemoteException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import org.apache.jackrabbit.rmi.remote.RemoteSession;

public interface RemoteManagerService extends Remote {
    public RemoteDocumentManager getDocumentManager(String sessionName) throws RepositoryException, RemoteException;
    public RemoteWorkflowManager getWorkflowManager(String sessionName) throws RepositoryException, RemoteException;
}
