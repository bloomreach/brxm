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
package org.hippoecm.editor.repository.impl;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.editor.repository.NamespaceWorkflow;
import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.WorkflowContext;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.ext.InternalWorkflow;
import org.hippoecm.repository.standardworkflow.Change;
import org.hippoecm.repository.standardworkflow.FolderWorkflow;
import org.hippoecm.repository.standardworkflow.RepositoryWorkflow;

public class NamespaceWorkflowImpl implements NamespaceWorkflow, InternalWorkflow {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private final String prefix;

    private final Session session;
    private final Node subject;
    private final WorkflowContext workflowContext;

    public NamespaceWorkflowImpl(WorkflowContext context, Session userSession, Session rootSession, Node subject)
            throws RemoteException, RepositoryException {
        this.workflowContext = context;
        this.session = rootSession;
        this.subject = subject;
        this.prefix = subject.getName();
    }

    @Override
    public Map<String, Serializable> hints() {
        Map<String, Serializable> hints = new TreeMap<String, Serializable>();
        hints.put("prefix", prefix);
        return hints;
    }

    @Override
    public void addCompoundType(String name) throws WorkflowException, MappingException, RepositoryException,
            RemoteException {
        WorkflowContext context = workflowContext.getWorkflowContext(null);

        NamespaceRegistry nsReg = session.getWorkspace().getNamespaceRegistry();

        FolderWorkflow folderWorkflow = (FolderWorkflow) context.getWorkflow("internal");
        Map<String, String> replacements = new TreeMap<String, String>();
        replacements.put("name", name);
        replacements.put("uri", nsReg.getURI(prefix));
        replacements.put("supertype", "hippo:compound");
        replacements.put("type", prefix + ":" + name);

        // ignore return type, as workflow chaining implies that the folder workflow
        // isn't executed until current method completes
        folderWorkflow.add("new-type", "compound", replacements);
    }

    @Override
    public void addDocumentType(String name) throws WorkflowException, MappingException, RepositoryException,
            RemoteException {
        WorkflowContext context = workflowContext.getWorkflowContext(null);

        NamespaceRegistry nsReg = session.getWorkspace().getNamespaceRegistry();

        FolderWorkflow folderWorkflow = (FolderWorkflow) context.getWorkflow("internal");
        Map<String, String> replacements = new TreeMap<String, String>();
        replacements.put("name", name);
        replacements.put("uri", nsReg.getURI(prefix));
        replacements.put("supertype", prefix + ":basedocument");
        replacements.put("type", prefix + ":" + name);

        // ignore return type, as workflow chaining implies that the folder workflow
        // isn't executed until current method completes  
        folderWorkflow.add("new-type", "document", replacements);
    }

    public void updateModel(String cnd, Map<String, List<Change>> updates) throws WorkflowException, MappingException,
            RepositoryException, RemoteException {
        RepositoryWorkflow repositoryWorkflow = (RepositoryWorkflow) workflowContext.getWorkflow("internal",
                workflowContext.getDocument("root", "root"));
        repositoryWorkflow.updateModel(prefix, cnd, "org.hippoecm.editor.repository.impl.TemplateConverter", updates);
    }
}
