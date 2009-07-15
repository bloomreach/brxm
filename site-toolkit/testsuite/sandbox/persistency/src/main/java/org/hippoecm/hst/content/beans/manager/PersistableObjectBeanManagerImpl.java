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

import java.rmi.RemoteException;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.uuid.UUID;
import org.hippoecm.hst.content.beans.ObjectBeanManagerException;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.persistence.ContentNodeBinder;
import org.hippoecm.hst.persistence.ContentPersistenceException;
import org.hippoecm.hst.persistence.ContentPersistenceManager;
import org.hippoecm.hst.persistence.workflow.WorkflowCallbackHandler;
import org.hippoecm.hst.persistence.workflow.WorkflowPersistenceManager;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.reviewedactions.FullReviewedActionsWorkflow;
import org.hippoecm.repository.standardworkflow.EditableWorkflow;
import org.hippoecm.repository.standardworkflow.FolderWorkflow;

/**
 * An implementation for {@link ContentPersistenceManager} interface with Hippo Repository Workflow API.
 * <P>
 * This implementation does not provide automatic bindings from content object to JCR node(s).
 * So, client codes should provide custom binders for their own node types. These custom binders map can be
 * given by a constructor argument, or a custom binder can be given by an argument of {@link #update(Object, ContentNodeBinder)} method.
 * </P>
 * <P>
 * Another useful option is to make a content POJO object implement {@link ContentNodeBinder} interface.
 * When client codes invoke {@link #update(Object)} method, this implementation will look up the custom binder 
 * from the internal map at first. If there's nothing found, then this implementation will check if the content POJO
 * object is an instance of {@link ContentNodeBinder}. If it is, this implementation will use the content POJO object itself
 * as a <CODE>ContentNodeBinder</CODE>.
 * </P>
 * <P>
 * If this implementation cannot find any <CODE>ContentNodeBinder</CODE>, it will do updating the content without any bindings.
 * </P>
 * 
 */
public class PersistableObjectBeanManagerImpl implements WorkflowPersistenceManager {

    /**
     * {@link ObjectBeanManager} instance provided by HST content-beans to provide {@link #getObject(String)} facility. 
     */
    protected ObjectBeanManager obm;
    
    /**
     * JCR session in this manager object context.
     */
    protected Session session;
    
    /**
     * Custom content node binders map, which is used to look up a custom binder for a node type.
     */
    protected Map<String, ContentNodeBinder> contentNodeBinders;
    
    /**
     * Hippo Repository specific predefined folder node type name
     */
    protected String folderNodeTypeName = "hippostd:folder";

    /**
     * The workflow category name to get a folder workflow.
     */
    protected String folderNodeWorkflowCategory = "internal"; // found in Niels's example
    
    /**
     * The workflow category name to get a document workflow. 
     */
    protected String documentNodeWorkflowCategory = "default"; // found in Niels's example
    
    /**
     * The workflow category name to add a new document.
     */
    protected String documentAdditionWorkflowCategory = "new-document"; // found in Niels's example
    
    /**
     * The workflow category name to add a new folder.
     */
    protected String folderAdditionWorkflowCategory = "new-folder"; // found in a test case for FolderWorkflow

    /**
     * Workflow callback handler
     */
    protected WorkflowCallbackHandler workflowCallbackHandler;
    
    /**
     * Constructor
     * @param session the session for this manager context
     * @param objectConverter the object converter to do mapping from JCR nodes to content POJO objects
     */
    public PersistableObjectBeanManagerImpl(Session session, ObjectConverter objectConverter) {
        this(session, objectConverter, null);
    }
    
    /**
     * Constructor
     * @param session the session for this manager context
     * @param objectConverter the object converter to do mapping from JCR nodes to content POJO objects
     * @param contentNodeBinders the predefined content node binders map which item is node type name key and custom binder object value.
     */
    public PersistableObjectBeanManagerImpl(Session session, ObjectConverter objectConverter, Map<String, ContentNodeBinder> contentNodeBinders) {
        obm = new ObjectBeanManagerImpl(session, objectConverter);
        this.session = session;
        this.contentNodeBinders = contentNodeBinders;
    }
   

    /**
     * Get an object from the JCR repository
     * 
     * @see {@link ObjectBeanManager#getObject(String)}
     */
    public Object getObject(String absPath) throws ContentPersistenceException {
        try {
            return obm.getObject(absPath);
        } catch (ObjectBeanManagerException e) {
            throw new ContentPersistenceException(e);
        }
    }
    
    /**
     * Creates content node(s) with the specified node type at the specified absolute path.
     * <P>
     * The absolute path could be regarded differently according to physical implementations.
     * For example, an implementation can regard the path as a simple one to create a simple JCR node.
     * On the other hand, a sophisticated implementation can regard the path as an input for 
     * a workflow-enabled document/folder path. 
     * </P>
     * 
     * @param absPath the absolute node path
     * @param nodeTypeName the node type name of the content object
     * @param name the content node name
     * @throws ContentPersistenceException
     */
    public void create(String absPath, String nodeTypeName, String name) throws ContentPersistenceException {
        create(absPath, nodeTypeName, name, false);
    }
    
    /**
     * Creates content node(s) with the specified node type at the specified absolute path.
     * <P>
     * The absolute path could be regarded differently according to physical implementations.
     * For example, an implementation can regard the path as a simple one to create a simple JCR node.
     * On the other hand, a sophisticated implementation can regard the path as an input for 
     * a workflow-enabled document/folder path. 
     * </P>
     * <P>
     * If <CODE>autoCreateFolders</CODE> is true, then folders will be automatically created.
     * </P>
     * 
     * @param absPath the absolute node path
     * @param nodeTypeName the node type name of the content object
     * @param name the content node name
     * @param autoCreateFolders the flag to create folders
     * @throws ContentPersistenceException
     */
    public void create(String absPath, String nodeTypeName, String name, boolean autoCreateFolders) throws ContentPersistenceException {
        try {
            if (!session.itemExists(absPath)) {
                if (!autoCreateFolders) {
                    throw new ContentPersistenceException("The folder node not found on the path: " + absPath);
                } else {
                    createMissingFolders(absPath);
                }
            }
            
            createNodeByWorkflow((Node) session.getItem(absPath), nodeTypeName, name);
        } catch (ContentPersistenceException e) {
            throw e;
        } catch (Exception e) {
            throw new ContentPersistenceException(e);
        }
    }
    
    protected void createMissingFolders(String absPath) throws ContentPersistenceException {
        try {
            String [] folderNames = absPath.split("/");
            
            Node rootNode = session.getRootNode();
            Node curNode = rootNode;
            String folderNodePath = null;
            
            for (String folderName : folderNames) {
                if (!"".equals(folderName)) {
                    if (curNode == rootNode) {
                        folderNodePath = "/" + folderName;
                    } else {
                        folderNodePath = curNode.getPath() + "/" + folderName;
                    }
                    
                    if (!session.itemExists(folderNodePath)) {
                        createNodeByWorkflow(curNode, folderNodeTypeName, folderName);
                    }
                    
                    curNode = curNode.getNode(folderName);

                    if (curNode.isNodeType(HippoNodeType.NT_FACETSELECT)) {
                        String docbaseUuid = curNode.getProperty("hippo:docbase").getString();
                        // check whether docbaseUuid is a valid uuid, otherwise a runtime IllegalArgumentException is thrown
                        try {
                            UUID.fromString(docbaseUuid);
                        } catch (IllegalArgumentException e){
                            throw new ContentPersistenceException("hippo:docbase in facetselect does not contain a valid uuid", e);
                        }
                        // this is always the canonical
                        curNode = session.getNodeByUUID(docbaseUuid);
                    } else if (curNode instanceof HippoNode) {
                        Node canonical = ((HippoNode) curNode).getCanonicalNode();
                        if(canonical == null) {
                            throw new ContentPersistenceException("Cannot create folders because there is no canonical node for '"+curNode.getPath()+"'");
                        }
                        curNode =  canonical;
                    }
                }
            }
        } catch (ContentPersistenceException e) {
            throw e;
        } catch (Exception e) {
            throw new ContentPersistenceException(e);
        }
    }
    
    protected void createNodeByWorkflow(Node folderNode, String nodeTypeName, String name)
            throws ContentPersistenceException {
        try {
            if (folderNode instanceof HippoNode) {
                Node canonical = ((HippoNode) folderNode).getCanonicalNode();
                if(canonical == null) {
                    throw new ContentPersistenceException("Cannot createNodeByWorkflow because there is no canonical node for '"+folderNode.getPath()+"'");
                }
                folderNode = canonical;
            }

            WorkflowManager wfm = ((HippoWorkspace) session.getWorkspace()).getWorkflowManager();
            Workflow wf = wfm.getWorkflow(folderNodeWorkflowCategory, folderNode);

            if (wf instanceof FolderWorkflow) {
                FolderWorkflow fwf = (FolderWorkflow) wf;

                String category = documentAdditionWorkflowCategory;

                if (nodeTypeName.equals(folderNodeTypeName)) {
                    category = folderAdditionWorkflowCategory;
                }

                String added = fwf.add(category, nodeTypeName, name);
                if (added == null) {
                    throw new ContentPersistenceException("Failed to add document/folder for type '" + nodeTypeName
                            + "'. Make sure there is a prototype.");
                }
            } else {
                throw new ContentPersistenceException("The workflow is not a FolderWorkflow for "
                        + folderNode.getPath() + ": " + wf);
            }
        } catch (RepositoryException e) {
            throw new ContentPersistenceException(e);
        } catch (RemoteException e) {
            throw new ContentPersistenceException(e);
        } catch (WorkflowException e) {
            throw new ContentPersistenceException(e);
        }

    }
    
    /**
     * Updates the content node which is mapped to the object.
     * <P>
     * This will look up a propery custom content node binder from the internal map. ({@link #contentNodeBinders}).
     * If it is not found there, this implementation will check if the content object is an instance of {@link ContentNodeBinder} interface.
     * If so, the content object will be used as a custom binder.
     * </P>
     * <P>
     * If there's no content node binder found, then this implementation will do updating
     * only without any bindings.
     * </P>
     * @param content
     * @throws ContentPersistenceException
     */
    public void update(Object content) throws ContentPersistenceException {
        if (content instanceof HippoBean) {
            ContentNodeBinder binder = null;

            if (contentNodeBinders != null && !contentNodeBinders.isEmpty()) {
                HippoBean contentBean = (HippoBean) content;
                Node contentNode = contentBean.getNode();
                
                try {
                    if (contentNode instanceof HippoNode) {
                        Node canonical = ((HippoNode) contentNode).getCanonicalNode();
                        if(canonical == null) {
                            throw new ContentPersistenceException("Cannot update HippoBean because there is no canonical node for '"+contentNode.getPath()+"'");
                        }
                        contentNode = canonical;
                    }
                    
                    binder = contentNodeBinders.get(contentNode.getPrimaryNodeType().getName());
                } catch (Exception e) {
                    throw new ContentPersistenceException(e);
                }
            }
            
            if (binder == null && content instanceof ContentNodeBinder) {
                binder = (ContentNodeBinder) content;
            }
            
            update(content, binder);
        } else {
            throw new ContentPersistenceException("The content object parameter should be an instance of HippoBean.");
        }
    }
    
    /**
     * Updates the content node which is mapped to the object by the <CODE>customContentNodeBinder</CODE>
     * provided by client.
     * <P>
     * Unlike {@link #update(Object)}, the implementation should not try to do automatic or predefined bindings.
     * Instead, it should invoke <CODE>customContentNodeBinder</CODE> to do bindings.
     * </P>
     * <P>
     * Therefore, if a developer wants to customize the bindings, the developer should provide a <CODE>customContentNodeBinder</CODE>.
     * </P>
     * @param content
     * @param customContentNodeBinder
     * @throws ContentPersistenceException
     */
    public void update(Object content, ContentNodeBinder customContentNodeBinder) throws ContentPersistenceException {
        if (content instanceof HippoBean) {
            try {
                HippoBean contentBean = (HippoBean) content;
                Node contentNode = contentBean.getNode();
                
                if (contentNode instanceof HippoNode) {
                    Node canonical = ((HippoNode) contentNode).getCanonicalNode();
                    if(canonical == null) {
                        throw new ContentPersistenceException("Cannot update HippoBean because there is no canonical node for '"+contentNode.getPath()+"'");
                    }
                    contentNode = canonical;
                }
                
                WorkflowManager wfm = ((HippoWorkspace) session.getWorkspace()).getWorkflowManager();
                Workflow wf = wfm.getWorkflow(documentNodeWorkflowCategory, contentNode);
                
                if (wf instanceof EditableWorkflow && customContentNodeBinder != null) {
                    EditableWorkflow ewf = (EditableWorkflow) wf;
                    Document document = ewf.obtainEditableInstance();
                    String uuid = document.getIdentity();
                    
                    if (uuid != null && !"".equals(uuid)) {
                        contentNode = session.getNodeByUUID(uuid);
                    }
                
                    boolean changed = customContentNodeBinder.bind(content, contentNode);
                    
                    if (changed) {
                        contentNode.save();
                        ewf.commitEditableInstance();
                    } else {
                        ewf.disposeEditableInstance();
                    }
                } else {
                    throw new ContentPersistenceException("The workflow is not a EditableWorkflow for " + contentBean.getPath() + ": " + wf);
                }

                if (workflowCallbackHandler != null) {
                    if (wf instanceof FullReviewedActionsWorkflow) {
                        workflowCallbackHandler.processWorkflow((FullReviewedActionsWorkflow) wf);
                    } else {
                        throw new ContentPersistenceException("Callback cannot be called because the workflow is not applicable: " + wf);
                    }
                }
            } catch (Exception e) {
                throw new ContentPersistenceException(e);
            }
        } else {
            throw new ContentPersistenceException("The content object parameter should be an instance of HippoBean.");
        }
    }
    
    /**
     * Removes the content node which is mapped to the object.
     * @param content
     * @throws ContentPersistenceException
     */
    public void remove(Object content) throws ContentPersistenceException {
        if (content instanceof HippoBean) {
            try {
                HippoBean contentBean = (HippoBean) content;
                HippoBean folderBean = contentBean.getParentBean();
                Node folderNode = folderBean.getNode();
                
                if (folderNode instanceof HippoNode) {
                    Node canonical = ((HippoNode) folderNode).getCanonicalNode();
                    if(canonical == null) {
                        throw new ContentPersistenceException("Cannot remove HippoBean because there is no canonical node for '"+folderNode.getPath()+"'");
                    }
                    folderNode = canonical;
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

    /**
     * Saves all pending changes. 
     * @throws ContentPersistenceException
     */
    public void save() throws ContentPersistenceException {
        try {
            session.save();
        } catch (Exception e) {
            throw new ContentPersistenceException(e);
        }
    }

    /**
     * Invokes {@link javax.jcr.Session#refresh(boolean)} with <CODE>false</CODE> parameter.  
     * @param keepChanges
     * @throws ContentPersistenceException
     */
    public void refresh() throws ContentPersistenceException {
        refresh(false);
    }
    
    /**
     * Invokes {@link javax.jcr.Session#refresh(boolean)}.  
     * @param keepChanges
     * @throws ContentPersistenceException
     */
    public void refresh(boolean keepChanges) throws ContentPersistenceException {
        try {
            session.refresh(keepChanges);
        } catch (Exception e) {
            throw new ContentPersistenceException(e);
        }
    }
    
    /**
     * Sets the folder node type name which is used to create folders.
     * @param folderNodeTypeName
     */
    public void setFolderNodeTypeName(String folderNodeTypeName) {
        this.folderNodeTypeName = folderNodeTypeName;
    }

    /**
     * Gets the folder node type name which is used to create folders.
     * @return
     */
    public String getFolderNodeTypeName() {
        return folderNodeTypeName;
    }
    
    /**
     * Gets the workflow category name used to get a folder workflow.
     * @return
     */
    public String getFolderNodeWorkflowCategory() {
        return folderNodeWorkflowCategory;
    }

    /**
     * Sets the workflow category name used to get a folder workflow.
     * @param folderNodeWorkflowCategory
     */
    public void setFolderNodeWorkflowCategory(String folderNodeWorkflowCategory) {
        this.folderNodeWorkflowCategory = folderNodeWorkflowCategory;
    }

    /**
     * Gets the workflow category name used to get a document workflow.
     * @return
     */
    public String getDocumentNodeWorkflowCategory() {
        return documentNodeWorkflowCategory;
    }

    /**
     * Sets the workflow category name used to get a document workflow.
     * @param documentNodeWorkflowCategory
     */
    public void setDocumentNodeWorkflowCategory(String documentNodeWorkflowCategory) {
        this.documentNodeWorkflowCategory = documentNodeWorkflowCategory;
    }

    /**
     * Gets the workflow category name used to add a folder.
     * @return
     */
    public String getFolderAdditionWorkflowCategory() {
        return folderAdditionWorkflowCategory;
    }

    /**
     * Sets the workflow category name used to add a folder.
     * @param folderAdditionWorkflowCategory
     */
    public void setFolderAdditionWorkflowCategory(String folderAdditionWorkflowCategory) {
        this.folderAdditionWorkflowCategory = folderAdditionWorkflowCategory;
    }

    /**
     * Gets the workflow category name used to add a document.
     * @return
     */
    public String getDocumentAdditionWorkflowCategory() {
        return documentAdditionWorkflowCategory;
    }

    /**
     * Sets the workflow category name used to add a document.
     * @param documentAdditionWorkflowCategory
     */
    public void setDocumentAdditionWorkflowCategory(String documentAdditionWorkflowCategory) {
        this.documentAdditionWorkflowCategory = documentAdditionWorkflowCategory;
    }
    
    public void setWorkflowCallbackHandler(WorkflowCallbackHandler<? extends Workflow> workflowCallbackHandler) {
        this.workflowCallbackHandler = workflowCallbackHandler;
    }
    
}
