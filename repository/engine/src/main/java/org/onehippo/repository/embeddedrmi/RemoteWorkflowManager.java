package org.onehippo.repository.embeddedrmi;

import java.rmi.Remote;
import java.rmi.RemoteException;
import javax.jcr.RepositoryException;
import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.Workflow;

public interface RemoteWorkflowManager extends Remote {

    public RemoteWorkflowDescriptor getWorkflowDescriptor(String category, String identifier) throws MappingException, RepositoryException, RemoteException;

    public RemoteWorkflowManager getContextWorkflowManager(Object specification) throws MappingException, RepositoryException, RemoteException;

    public Workflow getWorkflow(RemoteWorkflowDescriptor descriptor) throws MappingException, RepositoryException, RemoteException;
    
}
