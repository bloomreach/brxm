/*
 *  Copyright 2008 Hippo.
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
import java.io.StringReader;
import java.rmi.RemoteException;
import java.util.HashMap;
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

public class RemodelWorkflowImpl implements RepositoryWorkflow, InternalWorkflow {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    final static Logger log = LoggerFactory.getLogger(RemodelWorkflowImpl.class);

    private Session session;
    private Node subject;

    public RemodelWorkflowImpl(Session userSession, Session rootSession, Node subject) throws RemoteException, RepositoryException {
        this.session = rootSession;
        if(subject.getDepth() == 0)
            this.subject = rootSession.getRootNode();
        else
            this.subject = rootSession.getRootNode().getNode(subject.getPath().substring(1));
    }

    public Map<String,Serializable> hints() {
        return null;
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

    public void updateModel(String prefix, String cnd) throws WorkflowException, MappingException,
            RepositoryException, RemoteException
    {
         try {
            StringReader istream = new StringReader(cnd);
            Remodeling.remodel(session, prefix, istream, "", new HashMap());
        } catch (NamespaceException ex) {
             throw new WorkflowException("failed to update, corruption possible", ex);
        }
   }

    public void updateModel(String prefix, String cnd, String contentUpdater, Object contentUpdaterCargo) throws WorkflowException, MappingException,
            RepositoryException, RemoteException
    {
        try {
            StringReader istream = new StringReader(cnd);
            Remodeling remodel = Remodeling.remodel(session, prefix, istream, contentUpdater, contentUpdaterCargo);
        } catch (NamespaceException ex) {
            throw new WorkflowException("failed to update, corruption possible", ex);
        }
    }
}
