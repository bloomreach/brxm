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

import java.rmi.RemoteException;
import java.util.Map;
import java.util.TreeMap;

import javax.jcr.RepositoryException;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.ISO9075Helper;
import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.ext.WorkflowImpl;

public class TemplateEditorWorkflowImpl extends WorkflowImpl implements TemplateEditorWorkflow {

    public TemplateEditorWorkflowImpl() throws RemoteException {
        super();
    }
    
    public void createNamespace(String prefix, String namespace) throws WorkflowException, MappingException,
            RepositoryException, RemoteException {
        Document root = getWorkflowContext().getDocument("root","root");
        RepositoryWorkflow repositoryWorkflow = (RepositoryWorkflow) getWorkflowContext().getWorkflow("internal", root);
        repositoryWorkflow.createNamespace(prefix, namespace);
        FolderWorkflow folderWorkflow = (FolderWorkflow) getWorkflowContext().getWorkflow("internal");
        Map<String,String> replacements = new TreeMap<String,String>();
        replacements.put("./_name", prefix);
        replacements.put("name", prefix);
        folderWorkflow.add("Template Editor Namespace", "namespace", replacements);
    }

    public void createType(String name) throws WorkflowException, MappingException, RepositoryException, RemoteException {
        String encoded = ISO9075Helper.encodeLocalName(name);
        FolderWorkflow folderWorkflow = (FolderWorkflow) getWorkflowContext().getWorkflow("internal");
        Map<String,String> replacements = new TreeMap<String,String>();
        replacements.put("./_name", name);
        replacements.put("name", name);
        folderWorkflow.add("Template Editor Type", "type", replacements);
    }

    public void updateModel(String prefix, String cnd, Map<String, TypeUpdate> updates) throws WorkflowException, MappingException, RepositoryException, RemoteException {
        RepositoryWorkflow repositoryWorkflow = (RepositoryWorkflow) getWorkflowContext().getWorkflow("internal", getWorkflowContext().getDocument("root","root"));
        repositoryWorkflow.updateModel(prefix, cnd, "org.hippoecm.repository.standardworkflow.TemplateEditorWorkflowImpl", updates);
    }

}
