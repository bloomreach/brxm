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
package org.hippoecm.hst.content.beans.manager.workflow;

import java.rmi.RemoteException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.content.beans.ContentNodeBinder;
import org.hippoecm.hst.content.beans.ObjectBeanPersistenceException;
import org.hippoecm.hst.content.beans.manager.ObjectBeanManagerImpl;
import org.hippoecm.hst.content.beans.manager.ObjectConverter;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoDocumentBean;
import org.hippoecm.hst.content.beans.standard.HippoFolderBean;
import org.hippoecm.hst.util.NodeUtils;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.StringCodec;
import org.hippoecm.repository.api.StringCodecFactory;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.standardworkflow.DefaultWorkflow;
import org.hippoecm.repository.standardworkflow.EditableWorkflow;
import org.hippoecm.repository.standardworkflow.FolderWorkflow;
import org.onehippo.repository.documentworkflow.DocumentWorkflow;
import org.slf4j.LoggerFactory;

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

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(WorkflowPersistenceManagerImpl.class);
    
    /**
     * Custom content node binders map, which is used to look up a custom binder for a node type.
     */
    protected Map<String, ContentNodeBinder> contentNodeBinders;
    
    /**
     * Hippo Repository specific predefined folder node type name
     */
    protected String folderNodeTypeName = "hippostd:folder";

    /**
     * The workflow category name to get a folder workflow. We use threepane as this is the same as the CMS uses
     */
    protected String folderNodeWorkflowCategory = "threepane"; 
    
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
     * @deprecated since 2.28.00 (CMS 7.9), use {@link #workflowCallbackHandler} instead
     */
    @SuppressWarnings("rawtypes")
    @Deprecated
    protected WorkflowCallbackHandler deprecatedWorkflowCallbackHandler;

    /**
     * Qualified Workflow callback handler
     */
    @SuppressWarnings("rawtypes")
    protected QualifiedWorkflowCallbackHandler workflowCallbackHandler;

    /**
     * The codec which is used for the node names
     */
    protected StringCodec uriEncoding = new StringCodecFactory.UriEncoding();

    /**
     * The workflow category name to localize the new document
     */
    protected String defaultWorkflowCategory = "core";

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
     * <P>
     * If <CODE>autoCreateFolders</CODE> is true, then folders will be automatically created.
     * </P>
     *
     * @param absPath the absolute node path
     * @param nodeTypeName the node type name of the content object
     * @param name the content node name
     * @param autoCreateFolders the flag to create folders
     * @return the absolute path of the created node
     * @throws ObjectBeanPersistenceException
     */
    public String createAndReturn(final String absPath, final String nodeTypeName, final String name, final boolean autoCreateFolders) throws ObjectBeanPersistenceException {
        try {
            Node parentFolderNode = getExistingFolderNode(absPath);
            
            if (parentFolderNode == null) {
                if (!autoCreateFolders) {
                    throw new ObjectBeanPersistenceException("The folder node is not found on the path: " + absPath);
                } else {
                    parentFolderNode = createMissingFolders(absPath);
                }
            }

            return createNodeByWorkflow(parentFolderNode, nodeTypeName, name);
        } catch (ObjectBeanPersistenceException e) {
            throw e;
        } catch (Exception e) {
            throw new ObjectBeanPersistenceException(e);
        }
    }

    protected Node createMissingFolders(String absPath) throws ObjectBeanPersistenceException {
        try {
            String [] folderNames = StringUtils.split(absPath, "/");

            Node rootNode = session.getRootNode();
            Node curNode = rootNode;
            String folderNodePath;
            
            for (String folderName : folderNames) {
                String folderNodeName = uriEncoding.encode(folderName);

                if (curNode == rootNode) {
                    folderNodePath = "/" + folderNodeName;
                } else {
                    folderNodePath = curNode.getPath() + "/" + folderNodeName;
                }
                
                Node existingFolderNode = getExistingFolderNode(folderNodePath);
                
                if (existingFolderNode == null) {
                    curNode = session.getNode(createNodeByWorkflow(curNode, folderNodeTypeName, folderName));
                } else {
                    curNode = existingFolderNode;
                }

                curNode = NodeUtils.getCanonicalNode(curNode);
                
                if (NodeUtils.isDereferenceable(curNode)) {
                    curNode = NodeUtils.getDeref(curNode);
                }
            }

            return curNode;
        } catch (ObjectBeanPersistenceException e) {
            throw e;
        } catch (Exception e) {
            throw new ObjectBeanPersistenceException(e);
        }
    }
    
    @SuppressWarnings("rawtypes")
    protected String createNodeByWorkflow(Node folderNode, String nodeTypeName, String name)
            throws ObjectBeanPersistenceException {
        try {
            folderNode = getCanonicalNode(folderNode);
            Workflow wf = getWorkflow(folderNodeWorkflowCategory, folderNode);

            if (wf instanceof FolderWorkflow) {
                FolderWorkflow fwf = (FolderWorkflow) wf;

                String category = documentAdditionWorkflowCategory;

                
                if (nodeTypeName.equals(folderNodeTypeName)) {
                    category = folderAdditionWorkflowCategory;
                    
                    // now check if there is some more specific workflow for hippostd:folder
                    if(fwf.hints() != null &&  fwf.hints().get("prototypes") != null ) {
                        Object protypesMap = fwf.hints().get("prototypes");
                        if(protypesMap instanceof Map) {
                            for(Object o : ((Map)protypesMap).entrySet()) {
                                Entry entry = (Entry) o;
                                if(entry.getKey() instanceof String && entry.getValue() instanceof Set) {
                                    if( ((Set)entry.getValue()).contains(folderNodeTypeName)) {
                                        // we found possibly a more specific workflow for folderNodeTypeName. Use the key as category
                                        category =  (String)entry.getKey();
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }

                String nodeName = uriEncoding.encode(name);
                String added = fwf.add(category, nodeTypeName, nodeName);
                if (added == null) {
                    throw new ObjectBeanPersistenceException("Failed to add document/folder for type '" + nodeTypeName
                            + "'. Make sure there is a prototype.");
                }
                Node addedNode = folderNode.getSession().getNode(added);
                if (!nodeName.equals(name)) {
                    DefaultWorkflow defaultWorkflow = (DefaultWorkflow) getWorkflow(defaultWorkflowCategory, addedNode);
                    defaultWorkflow.localizeName(name);
                }

                if (documentAdditionWorkflowCategory.equals(category)) {

                    // added new document : because the document must be in 'preview' availability, we now set this explicitly
                    if (addedNode.isNodeType("hippostd:publishable")) {
                        log.info("Added document '{}' is pusblishable so set status to preview.",
                                addedNode.getPath());
                        addedNode.setProperty("hippostd:state", "unpublished");
                        addedNode.setProperty(HippoNodeType.HIPPO_AVAILABILITY, new String[] {"preview"});
                    } else {
                        log.info("Added document '{}' is not publishable so set status to live & preview directly.",
                                addedNode.getPath());
                        addedNode.setProperty(HippoNodeType.HIPPO_AVAILABILITY, new String[] {"live", "preview"});
                    }

                    if (addedNode.isNodeType("hippostd:publishableSummary")) {
                        addedNode.setProperty("hippostd:stateSummary", "new");
                    }
                    addedNode.getSession().save();
                }
                return added;
            } else {
                throw new ObjectBeanPersistenceException("Can't create folder " + name + " [" + nodeTypeName + "] in the folder " + folderNode.getPath() + ", because there is no FolderWorkflow possible on the folder node: " + wf);
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
     * This will look up a proper custom content node binder from the internal map. ({@link #contentNodeBinders}).
     * If it is not found there, this implementation will check if the content object is an instance of {@link ContentNodeBinder} interface.
     * If so, the content object will be used as a custom binder.
     * </P>
     * <P>
     * If there's no content node binder found, then this implementation will do updating
     * only without any bindings.
     * </P>
     * @param content the object to update
     * @throws ObjectBeanPersistenceException
     */
    public void update(Object content) throws ObjectBeanPersistenceException {
        if (content instanceof HippoBean) {
            ContentNodeBinder binder = null;

            if (contentNodeBinders != null && !contentNodeBinders.isEmpty()) {
                try {
                    HippoBean contentBean = (HippoBean) content;
                    Node contentNode = contentBean.getNode();
                    contentNode = getCanonicalNode(contentNode);
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
     * @param content the object to update
     * @param customContentNodeBinder the custom {@link ContentNodeBinder}
     * @throws ObjectBeanPersistenceException
     */
    @SuppressWarnings("unchecked")
    public void update(Object content, ContentNodeBinder customContentNodeBinder) throws ObjectBeanPersistenceException {
        if (content instanceof HippoBean) {
            String path = null;
            try {
                HippoBean contentBean = (HippoBean) content;
                path = contentBean.getPath();
                Node contentNode = contentBean.getNode();
                contentNode = getCanonicalNode(contentNode);
                Class<? extends Workflow> workflowType = workflowCallbackHandler != null ? workflowCallbackHandler.getWorkflowType() : null;
                WorkflowCallbackHandler currentCallbackHandler = workflowCallbackHandler != null ? workflowCallbackHandler : deprecatedWorkflowCallbackHandler;
                boolean documentWorkflowType = currentCallbackHandler == null || ((workflowType != null && DocumentWorkflow.class.isAssignableFrom(workflowType)));
                Node workflowNode = documentWorkflowType ? getHandleForDocumentWorkflow(contentNode) : contentNode;
                if (workflowNode == null) {
                    // needed fallback if provided contentNode doesn't 'live'
                    // within a hippo:handle Node with a hippostdpubwf:document child node
                    workflowNode = contentNode;
                    documentWorkflowType = false;
                }
                Workflow wf = getWorkflow(documentNodeWorkflowCategory, workflowNode);

                if (wf != null) {
                    // first check we retrieved a callback handler compatible workflow
                    if (workflowType != null && !workflowType.isInstance(wf)) {
                        throw new ObjectBeanPersistenceException("The provided workflow callback workflow type "
                                + workflowType.getName()
                                + " is not compatible with the retrieved workflow of type "
                                + wf.getClass().getName());
                    }
                    Document document;
                    if(customContentNodeBinder != null) {
                        if (wf instanceof EditableWorkflow) {
                            EditableWorkflow ewf = (EditableWorkflow) wf;
                            document = ewf.obtainEditableInstance();
                            String uuid = document.getIdentity();
                            
                            if (uuid != null && !"".equals(uuid)) {
                                contentNode = session.getNodeByIdentifier(uuid);
                            }
                            boolean changed = customContentNodeBinder.bind(content, contentNode);
                            
                            if (changed) {
                                contentNode.getSession().save();
                                // we need to recreate the EditableWorkflow because the node has changed
                                ewf = (EditableWorkflow) getWorkflow(documentNodeWorkflowCategory, workflowNode);
                                document = ewf.commitEditableInstance();
                                if (workflowCallbackHandler != null) {
                                    // recreate the wf because now the is changed
                                    if (documentWorkflowType) {
                                        wf = getWorkflow(documentNodeWorkflowCategory, workflowNode);
                                    }
                                    else {
                                        wf = getWorkflow(documentNodeWorkflowCategory, document);
                                    }
                                    if (wf != null) {
                                        currentCallbackHandler.processWorkflow(wf);
                                    } else {
                                        throw new ObjectBeanPersistenceException("Workflow callback cannot be called because the workflow is null. ");
                                    }
                                }
                            } else {
                                 ewf.disposeEditableInstance();
                            }
                        } else {
                            throw new ObjectBeanPersistenceException("The workflow is not a EditableWorkflow for " + path + ": " + wf);
                        } 
                    } else if (currentCallbackHandler != null) {
                        if (wf != null) {
                            currentCallbackHandler.processWorkflow(wf);
                        } 
                    }
                } else {
                    log.warn("Could not obtain workflow '{}' for '{}'. Make sure that user '{}' has enough workflow rights on the node.", new Object[]{documentNodeWorkflowCategory, path, contentNode.getSession().getUserID()});
                }
            } catch (ObjectBeanPersistenceException e) {
                throw e;
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
     * @param content the object to remove
     * @throws ObjectBeanPersistenceException
     */
    public void remove(Object content) throws ObjectBeanPersistenceException {
        if (!(content instanceof HippoBean)) {
            throw new ObjectBeanPersistenceException("The content object parameter should be an instance of HippoBean.");
        }
        
        try {
            HippoBean beanToRemove = (HippoBean) content;
            
            Node canonicalNodeToRemove = getCanonicalNode(beanToRemove.getNode());
            
            if(beanToRemove instanceof HippoDocumentBean) {
                canonicalNodeToRemove = canonicalNodeToRemove.getParent();
            } else if(beanToRemove instanceof HippoFolderBean){
                // do nothing
            } else {
                throw new ObjectBeanPersistenceException("Don't know how to persist a bean of type '"+beanToRemove.getClass().getName()+"'");
            }
            
            String nodeNameToRemove = canonicalNodeToRemove.getName();
            Node folderNodeToRemoveFrom = canonicalNodeToRemove.getParent();
            Workflow wf = getWorkflow(folderNodeWorkflowCategory, folderNodeToRemoveFrom);
            
            if (wf instanceof FolderWorkflow) {
                FolderWorkflow fwf = (FolderWorkflow) wf;
                fwf.delete(nodeNameToRemove);
                
            } else {
                throw new ObjectBeanPersistenceException("The workflow is not a FolderWorkflow for " + folderNodeToRemoveFrom.getPath() + ": " + wf);
            }
        } catch (Exception e) {
            throw new ObjectBeanPersistenceException(e);
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
     * @throws ObjectBeanPersistenceException
     */
    public void refresh() throws ObjectBeanPersistenceException {
        refresh(false);
    }
    
    /**
     * Invokes {@link javax.jcr.Session#refresh(boolean)}.  
     * @param keepChanges whether to keep changes or not
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
     * @param folderNodeTypeName the name of the folder
     */
    public void setFolderNodeTypeName(String folderNodeTypeName) {
        this.folderNodeTypeName = folderNodeTypeName;
    }

    /**
     * Gets the folder node type name which is used to create folders.
     * @return the <code>folderNodeTypeName</code>
     */
    public String getFolderNodeTypeName() {
        return folderNodeTypeName;
    }
    
    /**
     * Gets the workflow category name used to get a folder workflow.
     * @return he <code>folderNodeWorkflowCategory</code>
     */
    public String getFolderNodeWorkflowCategory() {
        return folderNodeWorkflowCategory;
    }

    /**
     * Sets the workflow category name used to get a folder workflow.
     * @param folderNodeWorkflowCategory sets the folderNodeWorkflowCategory
     */
    public void setFolderNodeWorkflowCategory(String folderNodeWorkflowCategory) {
        this.folderNodeWorkflowCategory = folderNodeWorkflowCategory;
    }

    /**
     * Gets the workflow category name used to get a document workflow.
     * @return returns the documentNodeWorkflowCategory
     */
    public String getDocumentNodeWorkflowCategory() {
        return documentNodeWorkflowCategory;
    }

    /**
     * Sets the workflow category name used to get a document workflow.
     * @param documentNodeWorkflowCategory the documentNodeWorkflowCategory to set
     */
    public void setDocumentNodeWorkflowCategory(String documentNodeWorkflowCategory) {
        this.documentNodeWorkflowCategory = documentNodeWorkflowCategory;
    }

    /**
     * Gets the workflow category name used to add a folder.
     * @return returns folderAdditionWorkflowCategory
     */
    public String getFolderAdditionWorkflowCategory() {
        return folderAdditionWorkflowCategory;
    }

    /**
     * Sets the workflow category name used to add a folder.
     * @param folderAdditionWorkflowCategory the folderAdditionWorkflowCategory to set
     */
    public void setFolderAdditionWorkflowCategory(String folderAdditionWorkflowCategory) {
        this.folderAdditionWorkflowCategory = folderAdditionWorkflowCategory;
    }

    /**
     * Gets the workflow category name used to add a document.
     * @return returns the documentAdditionWorkflowCategory
     */
    public String getDocumentAdditionWorkflowCategory() {
        return documentAdditionWorkflowCategory;
    }

    /**
     * Sets the workflow category name used to add a document.
     * @param documentAdditionWorkflowCategory the documentAdditionWorkflowCategory to set
     */
    public void setDocumentAdditionWorkflowCategory(String documentAdditionWorkflowCategory) {
        this.documentAdditionWorkflowCategory = documentAdditionWorkflowCategory;
    }

    /**
     * @deprecated since 2.28.00 (CMS 7.9), use {@link #setWorkflowCallbackHandler(QualifiedWorkflowCallbackHandler)} instead
     * @param workflowCallbackHandler
     */
    @Deprecated
    public void setWorkflowCallbackHandler(WorkflowCallbackHandler<? extends Workflow> workflowCallbackHandler) {
        if (workflowCallbackHandler instanceof QualifiedWorkflowCallbackHandler) {
            this.workflowCallbackHandler = (QualifiedWorkflowCallbackHandler)workflowCallbackHandler;
        }
        else {
            this.deprecatedWorkflowCallbackHandler = workflowCallbackHandler;
        }
    }

    public void setWorkflowCallbackHandler(QualifiedWorkflowCallbackHandler<? extends Workflow> workflowCallbackHandler) {
        this.workflowCallbackHandler = workflowCallbackHandler;
    }

    public Workflow getWorkflow(String category, Node node) throws RepositoryException {
        Workspace workspace = session.getWorkspace();
        
        ClassLoader workspaceClassloader = workspace.getClass().getClassLoader();
        ClassLoader currentClassloader = Thread.currentThread().getContextClassLoader();
        
        try {
            if (workspaceClassloader != currentClassloader) {
                Thread.currentThread().setContextClassLoader(workspaceClassloader);
            }
            
            WorkflowManager wfm = ((HippoWorkspace) workspace).getWorkflowManager();
            return wfm.getWorkflow(category, node);
        } catch (RepositoryException e) {
            throw e;
        } catch (Exception e) {
            // other exception which are not handled properly in the repository (we cannot do better here then just log them)
            if(log.isDebugEnabled()) {
                log.warn("Exception in workflow", e);
            } else {
                log.warn("Exception in workflow: {}", e.toString());
            }
        } finally { 
            if (workspaceClassloader != currentClassloader) {
                Thread.currentThread().setContextClassLoader(currentClassloader);
            }
        }
        
        return null;
    }
    
    public Workflow getWorkflow(String category, Document document) throws RepositoryException {
        Workspace workspace = session.getWorkspace();
        
        ClassLoader workspaceClassloader = workspace.getClass().getClassLoader();
        ClassLoader currentClassloader = Thread.currentThread().getContextClassLoader();
        
        try {
            if (workspaceClassloader != currentClassloader) {
                Thread.currentThread().setContextClassLoader(workspaceClassloader);
            }
            
            WorkflowManager wfm = ((HippoWorkspace) workspace).getWorkflowManager();
            return wfm.getWorkflow(category, document);
        } catch (RepositoryException e) {
            throw e;
        } catch (Exception e) {
            // other exception which are not handled properly in the repository (we cannot do better here then just log them)
            if(log.isDebugEnabled()) {
                log.warn("Exception in workflow", e);
            } else {
                log.warn("Exception in workflow: {}", e.toString());
            }
            return null;
        } finally {
            if (workspaceClassloader != currentClassloader) {
                Thread.currentThread().setContextClassLoader(currentClassloader);
            }
        }
    }
    
    private Node getCanonicalNode(Node folderNode) throws ObjectBeanPersistenceException {
        folderNode = NodeUtils.getCanonicalNode(folderNode);
        if(folderNode == null) {
            throw new ObjectBeanPersistenceException("Cannot perform workflow on a node that does not have a canonical version");
        }
        return folderNode;
    }

    /**
     * Finds and returns a node for the existing folder by the <CODE>absPath</CODE>.
     * Or it returns null when the folder node is not found by the <CODE>absPath</CODE>.
     * <P>
     * The <CODE>absPath</CODE> can be a virtual path or physical path.
     * This method tries to find a canonical node from the <CODE>absPath</CODE>
     * and a physical node by dereferencing if necessary.
     * </P>
     * <P>
     * Therefore, please note that the node path of the returned node can be different from
     * <CODE>absPath</CODE> by canonicalizing or dereferencing.
     * </P>
     * 
     * @param absPath the absPath to get the existing folder for
     * @return  A node for the existing folder or <code>null</code> when the folder node does not exist
     * @throws RepositoryException
     */
    private Node getExistingFolderNode(String absPath) throws RepositoryException {
        if (!session.nodeExists(absPath)) {
            return null;
        }
        
        Node node = session.getNode(absPath);
        Node candidateNode = null;
        
        if (session.getRootNode().isSame(node)) {
            return session.getRootNode();
        } else {
            Node parentNode = node.getParent();
            for (NodeIterator nodeIt = parentNode.getNodes(node.getName()); nodeIt.hasNext(); ) {
                Node siblingNode = nodeIt.nextNode();
                if (!isDocument(siblingNode)) {
                    candidateNode = siblingNode;
                    break;
                }
            }
        }
        
        if (candidateNode == null) {
            return null;
        }
        
        Node canonicalFolderNode = NodeUtils.getCanonicalNode(candidateNode);
        
        if (NodeUtils.isDereferenceable(canonicalFolderNode)) {
            canonicalFolderNode = NodeUtils.getDeref(canonicalFolderNode);
        }
        
        if (canonicalFolderNode == null) {
            return null;
        }
        
        if (isDocument(canonicalFolderNode)) {
            return null;
        }
        
        return canonicalFolderNode;
    }

    private Node getHandleForDocumentWorkflow(Node node) throws RepositoryException {
        if (NodeUtils.isNodeType(node, "hippo:handle")) {
            NodeIterator nodeIt = node.getNodes(node.getName());
            if (nodeIt.hasNext()) {
                Node child = nodeIt.nextNode();
                if (NodeUtils.isNodeType(child,"hippostdpubwf:document")) {
                    return node;
                }
            }
        }
        else  if (NodeUtils.isNodeType(node, "hippostdpubwf:document")) {
            Node parent = session.getRootNode().isSame(node) ? null : node.getParent();
            if (parent != null && NodeUtils.isNodeType(parent, "hippo:handle")) {
                return parent;
            }
        }
        else {
            Node parent = session.getRootNode().isSame(node) ? null : node.getParent();
            if (parent != null && NodeUtils.isNodeType(parent, "hippo:handle")) {
                return getHandleForDocumentWorkflow(parent);
            }
        }
        return null;
    }
    
    private boolean isDocument(Node node) throws RepositoryException  {
        if (NodeUtils.isNodeType(node, "hippo:handle")) {
            return true;
        } else if (NodeUtils.isNodeType(node, "hippo:document")) {
            if (!session.getRootNode().isSame(node)) {
                Node parentNode = node.getParent();
                
                if (NodeUtils.isNodeType(parentNode, "hippo:handle")) {
                    return true;
                }
            }
        }
        
        return false;
    }
}
