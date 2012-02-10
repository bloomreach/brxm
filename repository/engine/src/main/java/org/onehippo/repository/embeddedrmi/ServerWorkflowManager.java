package org.onehippo.repository.embeddedrmi;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import javax.jcr.RepositoryException;
import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowManager;

public class ServerWorkflowManager extends UnicastRemoteObject implements RemoteWorkflowManager {
    WorkflowManager local;
    int port;

    public ServerWorkflowManager(WorkflowManager workflowManager, int port) throws RemoteException {
        local = workflowManager;
        this.port = port;
    }

    public RemoteWorkflowDescriptor getWorkflowDescriptor(String category, String identifier) throws MappingException, RepositoryException, RemoteException {
        return new RemoteWorkflowDescriptor(local.getWorkflowDescriptor(category, local.getSession().getNodeByIdentifier(identifier)), category, identifier);
    }

    @Override
    public RemoteWorkflowManager getContextWorkflowManager(Object specification) throws MappingException, RepositoryException, RemoteException {
        return new ServerWorkflowManager(local.getContextWorkflowManager(specification), port);
    }

    @Override
    public Workflow getWorkflow(RemoteWorkflowDescriptor descriptor) throws MappingException, RepositoryException, RemoteException {
        Workflow workflow = local.getWorkflow(descriptor.category, local.getSession().getNodeByIdentifier(descriptor.identifier));
        try {
            UnicastRemoteObject.exportObject(workflow, port);
        } catch (RemoteException ex) {
            throw new RepositoryException("Problem creating workflow proxy", ex);
        }
        return workflow;
    }
    
}
