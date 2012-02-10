package org.onehippo.repository.embeddedrmi;

import java.rmi.Remote;
import java.rmi.RemoteException;
import javax.jcr.RepositoryException;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.DocumentManager;

public interface RemoteDocumentManager extends Remote {
    public Document getDocument(String category, String identifier) throws RepositoryException, RemoteException;    
}
