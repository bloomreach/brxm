/*
 *  Copyright 2008-2017 Hippo B.V. (http://www.onehippo.com)
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.repository.standardworkflow;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.Map;

import javax.jcr.NamespaceException;
import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.ext.InternalWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RepositoryWorkflowImpl implements RepositoryWorkflow, InternalWorkflow {

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(RepositoryWorkflowImpl.class);

    private Session session;
    private Node subject;

    public RepositoryWorkflowImpl(Session userSession, Session rootSession, Node subject) throws RemoteException, RepositoryException {
        this.session = rootSession;
        if(subject.getDepth() == 0)
            this.subject = rootSession.getRootNode();
        else
            this.subject = rootSession.getRootNode().getNode(subject.getPath().substring(1));
    }

    public Map<String,Serializable> hints() {
        return null;
    }

    /**
     * The hints method is not an actual workflow call, but a method by which information can be retrieved from the
     * workflow.  All implementations must implement this call as a pure function, no modification may be made, nor no
     * state may be maintained and and in principle no additional lookups of data is allowed.  This allows for caching
     * the result as long as the document on which the workflow operates isn't modified. By convention, keys that are
     * names or signatures of methods implemented by the workflow provide information to the application program whether
     * the workflow method is available this time, or will result in a WorkflowException.  The value for these keys will
     * often be a {@link Boolean} to indicate the enabled status of the method.<p/> Non-standard keys in this map should
     * be prefixed with the implementation package name using dot seperations.
     *
     * @param initializationPayload a map containing user context information relevant for the workflow
     * @return a map containing hints given by the workflow, the data in this map may be considered valid until the
     * document itself changes
     * @throws WorkflowException   thrown in case the implementing workflow encounters an error, this exception should
     *                             normally never be thrown by implementations for the hints method.
     * @throws RemoteException     a connection error with the repository
     * @throws RepositoryException a generic error communicating with the repository
     */
    @Override
    public Map<String, Serializable> hints(final Map<String, Serializable> initializationPayload) throws WorkflowException, RemoteException, RepositoryException {
        return hints();
    }

    public void createNamespace(String prefix, String uri) throws WorkflowException, MappingException,
            RepositoryException {
        try {
            NamespaceRegistry nsreg = session.getWorkspace().getNamespaceRegistry();
            nsreg.registerNamespace(prefix, uri);
        } catch (NamespaceException ex) {
            log.error(ex.getMessage() + " For: " + prefix + ":" + uri);
            throw new WorkflowException("Cannot create new namespace", ex);
        }
    }

}
