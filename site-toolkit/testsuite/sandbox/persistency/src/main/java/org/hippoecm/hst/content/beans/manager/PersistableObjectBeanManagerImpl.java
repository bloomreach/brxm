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

import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Session;

import org.hippoecm.hst.content.beans.ObjectBeanManagerException;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.persistence.ContentPersistenceBinder;
import org.hippoecm.hst.persistence.ContentPersistenceException;
import org.hippoecm.hst.persistence.ContentPersistenceManager;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.reviewedactions.BasicReviewedActionsWorkflow;
import org.hippoecm.repository.standardworkflow.EditableWorkflow;
import org.hippoecm.repository.standardworkflow.FolderWorkflow;

public class PersistableObjectBeanManagerImpl implements ContentPersistenceManager {

    private static final String HIPPOSTD_FOLDER_NODE_TYPE = "hippostd:folder";
    
    protected ObjectBeanManager obm;
    protected Session session;
    protected ObjectConverter objectConverter;
    protected Map<String, ContentPersistenceBinder> nodeTypeBinders;
    
    protected String folderNodeWorkflowCategory = "internal"; // found in Niels's example
    protected String documentNodeWorkflowCategory = "default"; // found in Niels's example
    protected String documentAdditionWorkflowCategory = "new-document"; // found in Niels's example
    protected String folderAdditionWorkflowCategory = "new-folder"; // fairy declaration, but it works!!
    
    protected boolean publishAfterUpdate;
    
    public PersistableObjectBeanManagerImpl(Session session, ObjectConverter objectConverter) {
        this(session, objectConverter, null);
    }
    
    public PersistableObjectBeanManagerImpl(Session session, ObjectConverter objectConverter, Map<String, ContentPersistenceBinder> nodeTypeBinders) {
        obm = new ObjectBeanManagerImpl(session, objectConverter);
        this.session = session;
        this.objectConverter = objectConverter;
        this.nodeTypeBinders = nodeTypeBinders;
    }
    
    public Object getObject(String absPath) throws ContentPersistenceException {
        try {
            return obm.getObject(absPath);
        } catch (ObjectBeanManagerException e) {
            throw new ContentPersistenceException(e);
        }
    }
    
    public void create(String absPath, String nodeTypeName, String name) throws ContentPersistenceException {
        try {
            Node folderNode = (Node) session.getItem(absPath);
            
            if (folderNode instanceof HippoNode) {
                folderNode = ((HippoNode) folderNode).getCanonicalNode();
            }
            
            WorkflowManager wfm = ((HippoWorkspace) session.getWorkspace()).getWorkflowManager();
            Workflow wf = wfm.getWorkflow(folderNodeWorkflowCategory, folderNode);
            
            if (wf instanceof FolderWorkflow) {
                FolderWorkflow fwf = (FolderWorkflow) wf;
                
                String category = documentAdditionWorkflowCategory;
                
                if (HIPPOSTD_FOLDER_NODE_TYPE.equals(nodeTypeName)) {
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
            ContentPersistenceBinder binder = null;

            if (nodeTypeBinders != null && !nodeTypeBinders.isEmpty()) {
                HippoBean contentBean = (HippoBean) content;
                Node contentNode = contentBean.getNode();
                
                try {
                    if (contentNode instanceof HippoNode) {
                        contentNode = ((HippoNode) contentNode).getCanonicalNode();
                    }
                    
                    binder = nodeTypeBinders.get(contentNode.getPrimaryNodeType().getName());
                    
                    if (binder == null && content instanceof ContentPersistenceBinder) {
                        binder = (ContentPersistenceBinder) content;
                    }
                } catch (Exception e) {
                    throw new ContentPersistenceException(e);
                }
            }
            
            update(content, binder);
        } else {
            throw new ContentPersistenceException("The content object parameter should be an instance of HippoBean.");
        }
    }
    
    public void update(Object content, ContentPersistenceBinder customBinder) throws ContentPersistenceException {
        if (content instanceof HippoBean) {
            try {
                HippoBean contentBean = (HippoBean) content;
                Node contentNode = contentBean.getNode();
                
                if (contentNode instanceof HippoNode) {
                    contentNode = ((HippoNode) contentNode).getCanonicalNode();
                }
                
                WorkflowManager wfm = ((HippoWorkspace) session.getWorkspace()).getWorkflowManager();
                Workflow wf = wfm.getWorkflow(documentNodeWorkflowCategory, contentNode);
                
                if (wf instanceof EditableWorkflow) {
                    EditableWorkflow ewf = (EditableWorkflow) wf;
                    Document document = ewf.obtainEditableInstance();
                    String uuid = document.getIdentity();
                    
                    if (uuid != null && !"".equals(uuid)) {
                        contentNode = session.getNodeByUUID(uuid);
                    }
                    
                    if (customBinder != null) {
                        customBinder.bind(content, contentNode);
                    }
                    
                    if (publishAfterUpdate) {
                        if (wf instanceof BasicReviewedActionsWorkflow) {
                            ((BasicReviewedActionsWorkflow) wf).requestPublication();
                        }
                    }
                    
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
                Node folderNode = folderBean.getNode();
                
                if (folderNode instanceof HippoNode) {
                    folderNode = ((HippoNode) folderNode).getCanonicalNode();
                }
                
                WorkflowManager wfm = ((HippoWorkspace) session.getWorkspace()).getWorkflowManager();
                Workflow wf = wfm.getWorkflow(folderNodeWorkflowCategory, folderNode);
                
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

    public String getFolderNodeWorkflowCategory() {
        return folderNodeWorkflowCategory;
    }

    public void setFolderNodeWorkflowCategory(String folderNodeWorkflowCategory) {
        this.folderNodeWorkflowCategory = folderNodeWorkflowCategory;
    }

    public String getDocumentNodeWorkflowCategory() {
        return documentNodeWorkflowCategory;
    }

    public void setDocumentNodeWorkflowCategory(String documentNodeWorkflowCategory) {
        this.documentNodeWorkflowCategory = documentNodeWorkflowCategory;
    }

    public String getFolderAdditionWorkflowCategory() {
        return folderAdditionWorkflowCategory;
    }

    public void setFolderAdditionWorkflowCategory(String folderAdditionWorkflowCategory) {
        this.folderAdditionWorkflowCategory = folderAdditionWorkflowCategory;
    }

    public String getDocumentAdditionWorkflowCategory() {
        return documentAdditionWorkflowCategory;
    }

    public void setDocumentAdditionWorkflowCategory(String documentAdditionWorkflowCategory) {
        this.documentAdditionWorkflowCategory = documentAdditionWorkflowCategory;
    }

    public void setPublishAfterUpdate(boolean publishAfterUpdate) {
        this.publishAfterUpdate = publishAfterUpdate;
    }
    
    public boolean getPublishAfterUpdate() {
        return publishAfterUpdate;
    }
    
}
