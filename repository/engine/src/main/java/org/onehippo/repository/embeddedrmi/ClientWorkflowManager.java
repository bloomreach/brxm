package org.onehippo.repository.embeddedrmi;

import java.rmi.RemoteException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowDescriptor;
import org.hippoecm.repository.api.WorkflowManager;

class ClientWorkflowManager implements WorkflowManager {
    Session session;
    RemoteWorkflowManager remote;

    ClientWorkflowManager(Session session, RemoteWorkflowManager remote) {
        this.session = session;
        this.remote = remote;
    }

    public Session getSession() throws RepositoryException {
        return session;
    }

    public WorkflowDescriptor getWorkflowDescriptor(String category, Node item) throws RepositoryException {
        try {
            RemoteWorkflowDescriptor remoteWorkflowDescriptor = remote.getWorkflowDescriptor(category, item.getIdentifier());
            if (remoteWorkflowDescriptor == null) {
                return null;
            } else {
                return new ClientWorkflowDescriptor(remoteWorkflowDescriptor);
            }
        } catch (RemoteException ex) {
            throw new RepositoryException(ex);
        }
    }

    public WorkflowDescriptor getWorkflowDescriptor(String category, Document document) throws RepositoryException {
        try {
            RemoteWorkflowDescriptor remoteWorkflowDescriptor = remote.getWorkflowDescriptor(category, document.getIdentity());
            if (remoteWorkflowDescriptor == null) {
                return null;
            } else {
                return new ClientWorkflowDescriptor(remoteWorkflowDescriptor);
            }
        } catch (RemoteException ex) {
            throw new RepositoryException(ex);
        }
    }

    public Workflow getWorkflow(String category, Node item) throws MappingException, RepositoryException {
        try {
            RemoteWorkflowDescriptor remoteWorkflowDescriptor = remote.getWorkflowDescriptor(category, item.getIdentifier());
            if (remoteWorkflowDescriptor == null) {
                return null;
            } else {
                return remote.getWorkflow(remoteWorkflowDescriptor);
            }
        } catch (RemoteException ex) {
            throw new RepositoryException(ex);
        }
    }

    public Workflow getWorkflow(String category, Document document) throws MappingException, RepositoryException {
        try {
            RemoteWorkflowDescriptor remoteWorkflowDescriptor = remote.getWorkflowDescriptor(category, document.getIdentity());
            if (remoteWorkflowDescriptor == null) {
                return null;
            } else {
                return remote.getWorkflow(remoteWorkflowDescriptor);
            }
        } catch (RemoteException ex) {
            throw new RepositoryException(ex);
        }
    }

    public Workflow getWorkflow(WorkflowDescriptor descriptor) throws MappingException, RepositoryException {
        try {
            return remote.getWorkflow((RemoteWorkflowDescriptor)descriptor);
        } catch (RemoteException ex) {
            throw new RepositoryException(ex);
        }
    }

    public WorkflowManager getContextWorkflowManager(Object specification) throws MappingException, RepositoryException {
        try {
            return new ClientWorkflowManager(session, remote.getContextWorkflowManager(specification));
        } catch (RemoteException ex) {
            throw new RepositoryException(ex);
        }
    }
    
}
