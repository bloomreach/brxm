package org.hippoecm.repository.reviewedactions;

import java.rmi.RemoteException;

import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowException;

public interface UnlockWorkflow extends Workflow {

    /**
     * Unlock document, i.e. take ownership of draft
     */
    void unlock()
        throws WorkflowException, MappingException, RepositoryException, RemoteException;

}
