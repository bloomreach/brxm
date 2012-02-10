package org.onehippo.repository.embeddedrmi;

import java.io.Serializable;
import java.util.Map;
import javax.jcr.RepositoryException;
import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowDescriptor;

class ClientWorkflowDescriptor implements WorkflowDescriptor {
    RemoteWorkflowDescriptor remote;
    Class<Workflow>[] interfaces;

    public ClientWorkflowDescriptor(RemoteWorkflowDescriptor workflowDescriptor) throws MappingException {
        this.remote = workflowDescriptor;
        if(workflowDescriptor.interfaces == null) {
            this.interfaces = null;
        } else {
            this.interfaces = new Class[workflowDescriptor.interfaces.length];
        }
        for (int i = 0; i < interfaces.length; i++) {
            try {
                interfaces[i] = (Class<Workflow>)Class.forName(workflowDescriptor.interfaces[i]);
            } catch (ClassNotFoundException ex) {
                throw new MappingException(workflowDescriptor.interfaces[i] + " class not found", ex);
            }
        }
    }

    public String getDisplayName() throws RepositoryException {
        return remote.display;
    }

    public Class<Workflow>[] getInterfaces() throws ClassNotFoundException, RepositoryException {
        return interfaces;
    }

    public String getAttribute(String name) throws RepositoryException {
        return remote.attributes.get(name);
    }

    public Map<String, Serializable> hints() throws RepositoryException {
        return remote.hints;
    }
}
