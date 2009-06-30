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
package org.hippoecm.hst.content.beans.manager;

import javax.jcr.Node;
import javax.jcr.Session;

import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.persistence.ContentPersistenceException;
import org.hippoecm.hst.persistence.ContentPersistenceManager;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.standardworkflow.EditableWorkflow;
import org.hippoecm.repository.standardworkflow.FolderWorkflow;

public class PersistableObjectBeanManagerImpl extends ObjectBeanManagerImpl implements ContentPersistenceManager {

    private static final String HIPPOSTD_FOLDER = "hippostd:folder";
    
    // TODO: Are these category names correct?
    protected String workflowCategory = "default"; // just fairy declaration
    protected String folderAdditionWorkflowCategory = "new-folder"; // just fairy declaration
    protected String folderRemovalWorkflowCategory = "remove-folder"; // just fairy declaration
    protected String documentAdditionWorkflowCategory = "new-document"; // found in Niels's example
    protected String documentRemovalWorkflowCategory = "remove-document"; // just fairy declaration

    public PersistableObjectBeanManagerImpl(Session session, ObjectConverter objectConverter) {
        super(session, objectConverter);
    }
    
    public void create(String absPath, String nodeTypeName, String name) throws ContentPersistenceException {
        try {
            Node folderNode = (Node) session.getItem(absPath);
            WorkflowManager wfm = ((HippoWorkspace) session.getWorkspace()).getWorkflowManager();
            Workflow wf = wfm.getWorkflow(workflowCategory, folderNode);
            
            if (wf instanceof FolderWorkflow) {
                FolderWorkflow fwf = (FolderWorkflow) wf;
                
                String category = documentAdditionWorkflowCategory;
                
                // TODO: Is there any folder addition workflow category?
                if (HIPPOSTD_FOLDER.equals(nodeTypeName)) {
                    category = folderAdditionWorkflowCategory;
                }
                
                fwf.add(category, nodeTypeName, name);
            } else {
                throw new ContentPersistenceException("The workflow is not a FolderWorkflow for " + absPath + ": " + wf);
            }
        } catch (Exception e) {
            throw new ContentPersistenceException(e);
        }
    }
    
    public void update(Object content) throws ContentPersistenceException {
        if (content instanceof HippoBean) {
            try {
                HippoBean contentBean = (HippoBean) content;
                WorkflowManager wfm = ((HippoWorkspace) session.getWorkspace()).getWorkflowManager();
                Workflow wf = wfm.getWorkflow(workflowCategory, contentBean.getNode());
                
                // TODO: Can we use EditableWorkflow or any other here?
                if (wf instanceof EditableWorkflow) {
                    EditableWorkflow ewf = (EditableWorkflow) wf;
                    Document document = ewf.obtainEditableInstance();
                    // TODO: How can document be updated?
                    ewf.commitEditableInstance();
                } else {
                    throw new ContentPersistenceException("The workflow is not a EditableWorkflow for " + contentBean.getPath() + ": " + wf);
                }
            } catch (Exception e) {
                throw new ContentPersistenceException(e);
            }
        } else {
            throw new ContentPersistenceException("The content object parameter should be an instance of HippoBean.");
        }
    }
    
    public void remove(Object content) throws ContentPersistenceException {
        if (content instanceof HippoBean) {
            try {
                HippoBean contentBean = (HippoBean) content;
                HippoBean folderBean = contentBean.getParentBean();
                
                WorkflowManager wfm = ((HippoWorkspace) session.getWorkspace()).getWorkflowManager();
                Workflow wf = wfm.getWorkflow(workflowCategory, folderBean.getNode());
                
                // TODO: Is it right to invoke delete() from FolderWorkflow to delete document or subfolder?
                if (wf instanceof FolderWorkflow) {
                    FolderWorkflow fwf = (FolderWorkflow) wf;
                    fwf.delete(contentBean.getName());
                } else {
                    throw new ContentPersistenceException("The workflow is not a FolderWorkflow for " + folderBean.getPath() + ": " + wf);
                }
            } catch (Exception e) {
                throw new ContentPersistenceException(e);
            }
        } else {
            throw new ContentPersistenceException("The content object parameter should be an instance of HippoBean.");
        }
    }

    public void save() throws ContentPersistenceException {
        try {
            session.save();
        } catch (Exception e) {
            throw new ContentPersistenceException(e);
        }
    }

    public void reset() throws ContentPersistenceException {
        try {
            session.refresh(false);
        } catch (Exception e) {
            throw new ContentPersistenceException(e);
        }
    }
    
}
