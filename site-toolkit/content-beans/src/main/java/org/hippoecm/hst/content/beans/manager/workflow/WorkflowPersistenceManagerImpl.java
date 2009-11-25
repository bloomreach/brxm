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
package org.hippoecm.hst.content.beans.manager.workflow;

import java.rmi.RemoteException;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.uuid.UUID;
import org.hippoecm.hst.content.beans.ContentNodeBinder;
import org.hippoecm.hst.content.beans.ObjectBeanPersistenceException;
import org.hippoecm.hst.content.beans.manager.ObjectBeanManagerImpl;
import org.hippoecm.hst.content.beans.manager.ObjectConverter;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.standardworkflow.EditableWorkflow;
import org.hippoecm.repository.standardworkflow.FolderWorkflow;

/**
 * An implementation for {@link WorkflowPersistenceManager} interface with Hippo Repository Workflow API.
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
public class WorkflowPersistenceManagerImpl extends ObjectBeanManagerImpl implements WorkflowPersistenceManager {

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
    protected String folderNodeWorkflowCategory = "internal"; 
    
    /**
     * The workflow category name to get a document workflow. 
     */
    protected String documentNodeWorkflowCategory = "default"; 
    
    /**
     * The workflow category name to add a new document.
     */
    protected String documentAdditionWorkflowCategory = "new-document"; 
    
    /**
     * The workflow category name to add a new folder.
     */
    protected String folderAdditionWorkflowCategory = "new-folder"; 

    /**
     * Workflow callback handler
     */
    protected WorkflowCallbackHandler workflowCallbackHandler;
    
    /**
     * Constructor
     * @param session the session for this manager context
     * @param objectConverter the object converter to do mapping from JCR nodes to content POJO objects
     */
    public WorkflowPersistenceManagerImpl(Session session, ObjectConverter objectConverter) {
        this(session, objectConverter, null);
    }
     
    /**
     * Constructor
     * @param session the session for this manager context
     * @param objectConverter the object converter to do mapping from JCR nodes to content POJO objects
     * @param contentNodeBinders the predefined content node binders map which item is node type name key and custom binder object value.
     */
    public WorkflowPersistenceManagerImpl(Session session, ObjectConverter objectConverter, Map<String, ContentNodeBinder> contentNodeBinders) {
        super(session, objectConverter);
        this.contentNodeBinders = contentNodeBinders;
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
     * @throws ObjectBeanPersistenceException
     */
    public void create(String absPath, String nodeTypeName, String name) throws ObjectBeanPersistenceException {
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
     * @throws ObjectBeanPersistenceException
     */
    public void create(String absPath, String nodeTypeName, String name, boolean autoCreateFolders) throws ObjectBeanPersistenceException {
        try {
            if (!session.itemExists(absPath)) {
                if (!autoCreateFolders) {
                    throw new ObjectBeanPersistenceException("The folder node not found on the path: " + absPath);
                } else {
                    createMissingFolders(absPath);
                }
            }
            
            createNodeByWorkflow((Node) session.getItem(absPath), nodeTypeName, name);
        } catch (ObjectBeanPersistenceException e) {
            throw e;
        } catch (Exception e) {
            throw new ObjectBeanPersistenceException(e);
        }
    }
    
    protected void createMissingFolders(String absPath) throws ObjectBeanPersistenceException {
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

                    if (curNode.isNodeType(HippoNodeType.NT_FACETSELECT) || curNode.isNodeType(HippoNodeType.NT_MIRROR )) {
                        String docbaseUuid = curNode.getProperty("hippo:docbase").getString();
                        // check whether docbaseUuid is a valid uuid, otherwise a runtime IllegalArgumentException is thrown
                        try {
                            UUID.fromString(docbaseUuid);
                        } catch (IllegalArgumentException e){
                            throw new ObjectBeanPersistenceException("hippo:docbase in mirror does not contain a valid uuid", e);
                        }
                        // this is always the canonical
                        curNode = session.getNodeByUUID(docbaseUuid);
                    } else if (curNode instanceof HippoNode) {
                        Node canonical = ((HippoNode) curNode).getCanonicalNode();
                        if(canonical == null) {
                            throw new ObjectBeanPersistenceException("Cannot create folders because there is no canonical node for '"+curNode.getPath()+"'");
                        }
                        curNode =  canonical;
                    }
                }
            }
        } catch (ObjectBeanPersistenceException e) {
            throw e;
        } catch (Exception e) {
            throw new ObjectBeanPersistenceException(e);
        }
    }
    
    protected void createNodeByWorkflow(Node folderNode, String nodeTypeName, String name)
            throws ObjectBeanPersistenceException {
        try {
            if (folderNode instanceof HippoNode) {
                Node canonical = ((HippoNode) folderNode).getCanonicalNode();
                if(canonical == null) {
                    throw new ObjectBeanPersistenceException("Cannot createNodeByWorkflow because there is no canonical node for '"+folderNode.getPath()+"'");
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
                    throw new ObjectBeanPersistenceException("Failed to add document/folder for type '" + nodeTypeName
                            + "'. Make sure there is a prototype.");
                }
            } else {
                throw new ObjectBeanPersistenceException("The workflow is not a FolderWorkflow for "
                        + folderNode.getPath() + ": " + wf);
            }
        } catch (RepositoryException e) {
            throw new ObjectBeanPersistenceException(e);
        } catch (RemoteException e) {
            throw new ObjectBeanPersistenceException(e);
        } catch (WorkflowException e) {
            throw new ObjectBeanPersistenceException(e);
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
     * @throws ObjectBeanPersistenceException
     */
    public void update(Object content) throws ObjectBeanPersistenceException {
        if (content instanceof HippoBean) {
            ContentNodeBinder binder = null;

            if (contentNodeBinders != null && !contentNodeBinders.isEmpty()) {
                HippoBean contentBean = (HippoBean) content;
                Node contentNode = contentBean.getNode();
                
                try {
                    if (contentNode instanceof HippoNode) {
                        Node canonical = ((HippoNode) contentNode).getCanonicalNode();
                        if(canonical == null) {
                            throw new ObjectBeanPersistenceException("Cannot update HippoBean because there is no canonical node for '"+contentNode.getPath()+"'");
                        }
                        contentNode = canonical;
                    }
                    
                    binder = contentNodeBinders.get(contentNode.getPrimaryNodeType().getName());
                } catch (Exception e) {
                    throw new ObjectBeanPersistenceException(e);
                }
            }
            
            if (binder == null && content instanceof ContentNodeBinder) {
                binder = (ContentNodeBinder) content;
            }
            
            update(content, binder);
        } else {
            throw new ObjectBeanPersistenceException("The content object parameter should be an instance of HippoBean.");
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
     * @throws ObjectBeanPersistenceException
     */
    public void update(Object content, ContentNodeBinder customContentNodeBinder) throws ObjectBeanPersistenceException {
        String path = null; 
        if (content instanceof HippoBean) {
            try {
                HippoBean contentBean = (HippoBean) content;
                Node contentNode = contentBean.getNode();
                path = contentNode.getPath();
                if (contentNode instanceof HippoNode) {
                    Node canonical = ((HippoNode) contentNode).getCanonicalNode();
                    if(canonical == null) {
                        throw new ObjectBeanPersistenceException("Cannot update HippoBean because there is no canonical node for '"+contentNode.getPath()+"'");
                    }
                    contentNode = canonical;
                }
                
                WorkflowManager wfm = ((HippoWorkspace) session.getWorkspace()).getWorkflowManager();
                Workflow wf = wfm.getWorkflow(documentNodeWorkflowCategory, contentNode);
                
                //String handleUuid = contentNode.getParent().getUUID();
                
                Document document = null;
                if(customContentNodeBinder != null) {
                    if (wf instanceof EditableWorkflow) {
                        EditableWorkflow ewf = (EditableWorkflow) wf;
                        document = ewf.obtainEditableInstance();
                        String uuid = document.getIdentity();
                        
                        if (uuid != null && !"".equals(uuid)) {
                            contentNode = session.getNodeByUUID(uuid);
                        }
                        boolean changed = customContentNodeBinder.bind(content, contentNode);
                        
                        if (changed) {
                            contentNode.save();
                            // we need to recreate the EditableWorkflow because the node has changed
                            ewf = (EditableWorkflow)wfm.getWorkflow(documentNodeWorkflowCategory, contentNode);
                            document = ewf.commitEditableInstance();
                        } else {
                            document = ewf.disposeEditableInstance();
                        }
                    } else {
                        throw new ObjectBeanPersistenceException("The workflow is not a EditableWorkflow for " + contentBean.getPath() + ": " + wf);
                    }
                }
                
                if (workflowCallbackHandler != null) {
                    // recreate the wf 
                    wf = wfm.getWorkflow(documentNodeWorkflowCategory, document);
                    if (wf != null) {
                        workflowCallbackHandler.processWorkflow(wf);
                    } else {
                        throw new ObjectBeanPersistenceException("Callback cannot be called because the workflow is not applicable: " + wf);
                    }
                }
            } catch (Exception e) {
                if(path != null) {
                    throw new ObjectBeanPersistenceException("Exception while trying to update '"+path+"'" ,e);
                } else {
                    throw new ObjectBeanPersistenceException(e);
                }
            }
        } else {
            throw new ObjectBeanPersistenceException("The content object parameter should be an instance of HippoBean.");
        }
    }
    
    /**
     * Removes the content node which is mapped to the object.
     * @param content
     * @throws ObjectBeanPersistenceException
     */
    public void remove(Object content) throws ObjectBeanPersistenceException {
        if (content instanceof HippoBean) {
            try {
                HippoBean contentBean = (HippoBean) content;
                
                Node canonical = ((HippoNode)contentBean.getNode()).getCanonicalNode();
                if(canonical == null) {
                    throw new ObjectBeanPersistenceException("Cannot remove HippoBean because there is no canonical node for '"+contentBean.getPath()+"'");
                }
             
                Node handleNode = canonical.getParent();
                String nodeName = handleNode.getName();
                HippoBean folderBean = contentBean.getParentBean();
                Node folderNode = folderBean.getNode();
                canonical = ((HippoNode)folderNode).getCanonicalNode();
                if(canonical == null) {
                    throw new ObjectBeanPersistenceException("Cannot remove HippoBean because there is no canonical node for '"+folderNode.getPath()+"'");
                }
                folderNode = canonical;
                
                // TODO when HREPTWO-2844 is fixed, this code can be removed
                if(handleNode.isNodeType(HippoNodeType.NT_HANDLE)) {
                    handleNode.checkout();
                    NodeIterator it = handleNode.getNodes();
                    while(it.hasNext()) {
                        Node doc = it.nextNode();
                        if(doc == null) { 
                            continue;
                        }
                        if(doc.isNodeType("mix:versionable")) {
                            doc.checkout();
                        }
                    }
                } else {
                    // TODO : check for childs all being checked out??
                }
                    
                
                WorkflowManager wfm = ((HippoWorkspace) session.getWorkspace()).getWorkflowManager();
                Workflow wf = wfm.getWorkflow(folderNodeWorkflowCategory, folderNode);
                
                if (wf instanceof FolderWorkflow) {
                    FolderWorkflow fwf = (FolderWorkflow) wf;
                    fwf.delete(nodeName);
                } else {
                    throw new ObjectBeanPersistenceException("The workflow is not a FolderWorkflow for " + folderBean.getPath() + ": " + wf);
                }
            } catch (Exception e) {
                throw new ObjectBeanPersistenceException(e);
            }
        } else {
            throw new ObjectBeanPersistenceException("The content object parameter should be an instance of HippoBean.");
        }
    }

    /**
     * Saves all pending changes. 
     * @throws ObjectBeanPersistenceException
     */
    public void save() throws ObjectBeanPersistenceException {
        try {
            session.save();
        } catch (Exception e) {
            throw new ObjectBeanPersistenceException(e);
        }
    }

    /**
     * Invokes {@link javax.jcr.Session#refresh(boolean)} with <CODE>false</CODE> parameter.  
     * @param keepChanges
     * @throws ObjectBeanPersistenceException
     */
    public void refresh() throws ObjectBeanPersistenceException {
        refresh(false);
    }
    
    /**
     * Invokes {@link javax.jcr.Session#refresh(boolean)}.  
     * @param keepChanges
     * @throws ObjectBeanPersistenceException
     */
    public void refresh(boolean keepChanges) throws ObjectBeanPersistenceException {
        try {
            session.refresh(keepChanges);
        } catch (Exception e) {
            throw new ObjectBeanPersistenceException(e);
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
