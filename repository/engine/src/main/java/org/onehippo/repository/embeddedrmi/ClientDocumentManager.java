package org.onehippo.repository.embeddedrmi;

import java.rmi.RemoteException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.DocumentManager;

class ClientDocumentManager implements DocumentManager {
    Session session;
    RemoteDocumentManager remote;

    public ClientDocumentManager(Session session, RemoteDocumentManager remote) {
        this.session = session;
        this.remote = remote;
    }

    public Session getSession() throws RepositoryException {
        return session;
    }

    public Document getDocument(String category, String identifier) throws RepositoryException {
        try {
            return remote.getDocument(category, identifier);
        } catch (RemoteException ex) {
            throw new RepositoryException(ex);
        }
    }
    
}
