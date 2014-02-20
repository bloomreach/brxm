/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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

import java.rmi.RemoteException;
import java.util.Map;
import java.util.TreeMap;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.editor.repository.EditmodelWorkflow;
import org.hippoecm.editor.repository.TemplateEditorWorkflow;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.ext.WorkflowImpl;
import org.hippoecm.repository.standardworkflow.FolderWorkflow;
import org.hippoecm.repository.standardworkflow.RepositoryWorkflow;
import org.hippoecm.repository.util.NodeIterable;

public class TemplateEditorWorkflowImpl extends WorkflowImpl implements TemplateEditorWorkflow {
    private static final long serialVersionUID = 1L;


    public TemplateEditorWorkflowImpl() throws RemoteException {
        super();
    }

    public String createNamespace(String prefix, String uri) throws WorkflowException, MappingException,
            RepositoryException, RemoteException {

        Document root = new Document(getWorkflowContext().getUserSession().getRootNode());
        RepositoryWorkflow repositoryWorkflow = (RepositoryWorkflow)getWorkflowContext().getWorkflow("internal", root);
        repositoryWorkflow.createNamespace(prefix, uri);

        FolderWorkflow folderWorkflow = (FolderWorkflow) getWorkflowContext().getWorkflow("internal");
        Map<String, String> replacements = new TreeMap<>();
        replacements.put("name", prefix);
        replacements.put("uri", uri);
        final String namespacePath = folderWorkflow.add("Template Editor Namespace", "namespace", replacements);
        final Node namespace = getWorkflowContext().getUserSession().getNode(namespacePath);
        for (Node node : new NodeIterable(namespace.getNodes())) {
            if (node.isNodeType("hipposysedit:templatetype")) {
                final EditmodelWorkflow editModelWorkflow = (EditmodelWorkflow) getWorkflowContext().
                        getWorkflow("editing", new Document(node));
                editModelWorkflow.edit();
                editModelWorkflow.save();
                editModelWorkflow.commit();
            }
        }
        return namespacePath;

    }

}
