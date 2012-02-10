package org.onehippo.repository.embeddedrmi;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import javax.jcr.RepositoryException;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.DocumentManager;

class ServerDocumentManager extends UnicastRemoteObject implements RemoteDocumentManager {
    DocumentManager local;

    ServerDocumentManager(DocumentManager documentManager) throws RemoteException {
        local = documentManager;
    }

    public Document getDocument(String category, String identifier) throws RepositoryException {
        return local.getDocument(category, identifier);
    }
   
}
