package org.onehippo.repository.embeddedrmi;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import javax.jcr.LoginException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import org.onehippo.repository.JackrabbitRepository;
import org.onehippo.repository.ManagerServiceFactory;

public class ServerManagerService extends UnicastRemoteObject implements RemoteManagerService {
    JackrabbitRepository repository;
    ServerDocumentManager documentManager = null;
    ServerWorkflowManager workflowManager = null;
    int port;

    public ServerManagerService(JackrabbitRepository repository, int port) throws RemoteException {
        this.repository = repository;
        this.port = port;
    }

    public RemoteDocumentManager getDocumentManager(String sessionName) throws RepositoryException, RemoteException {
        if (documentManager == null) {
            Session localSession = repository.getSession(sessionName);
            documentManager = new ServerDocumentManager(ManagerServiceFactory.getManagerService(localSession).getDocumentManager());
        }
        return documentManager;
    }

    public RemoteWorkflowManager getWorkflowManager(String sessionName) throws RepositoryException, RemoteException {
        try {
            if (workflowManager == null) {
                Session localSession = repository.getSession(sessionName);
                workflowManager = new ServerWorkflowManager(ManagerServiceFactory.getManagerService(localSession).getWorkflowManager(), port);
            }
            return workflowManager;
        } catch (LoginException ex) {
            throw ex;
        } catch (RepositoryException ex) {
            throw ex;
        }
    }
}
