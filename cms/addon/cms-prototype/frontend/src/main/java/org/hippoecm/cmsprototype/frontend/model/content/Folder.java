/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.cmsprototype.frontend.model.content;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.NodeModelWrapper;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;

public class Folder extends NodeModelWrapper {
    private static final long serialVersionUID = 1L;
    
    protected List<Folder> subFolders;
    protected List<Document> documents;
    
    public Folder(JcrNodeModel nodeModel) {
        super(nodeModel);
    }
    
    public List<Folder> getSubFolders() {
        ensureSubFoldersAreLoaded();
        return subFolders;
    }
    
    public List<Document> getDocuments() {
        ensureDocumentsAreLoaded();
        return documents;
    }
    
    public List<NodeModelWrapper> getSubFoldersAndDocuments() {
        ensureSubFoldersAreLoaded();
        ensureDocumentsAreLoaded();
        List<NodeModelWrapper> list = new ArrayList<NodeModelWrapper>();
        list.addAll(subFolders);
        list.addAll(documents);
        return list;
    }
    
    
    private void ensureDocumentsAreLoaded() {
        if (documents == null) {
            documents = loadDocuments();
        }
    }
    
    private List<Document> loadDocuments() {
        List<Document> docs = new ArrayList<Document>();
        Node node = nodeModel.getNode();
        try {
            NodeIterator jcrChildren = node.getNodes();
            while (jcrChildren.hasNext()) {
                HippoNode jcrChild = (HippoNode) jcrChildren.nextNode();
                if (jcrChild != null ) {
                    if (jcrChild.isNodeType(HippoNodeType.NT_HANDLE)) {
                        docs.add(new Document(new JcrNodeModel(jcrChild)));
                    }
                    
                    // handle facet result nodes
                    else if (jcrChild.isNodeType(HippoNodeType.NT_FACETRESULT)) {
                        NodeIterator fsChildren = jcrChild.getNodes();
                        while (fsChildren.hasNext()) {
                            HippoNode fsChild = (HippoNode) fsChildren.next();
                            if (fsChild != null && fsChild.isNodeType(HippoNodeType.NT_DOCUMENT)) {
                                Node canonicalNode = fsChild.getCanonicalNode();
                                Node parentNode = canonicalNode.getParent();
                                if (parentNode != null && parentNode.isNodeType(HippoNodeType.NT_HANDLE)) {
                                    docs.add(new Document(new JcrNodeModel(parentNode)));
                                }
                            }
                        }
                    }
                }
            }
        }
        catch (RepositoryException e) {
            e.printStackTrace();
        }
        
        // remove duplicates (if any) :-\
        Set<Document> set = new HashSet<Document>();
        set.addAll(docs);
        docs.clear();
        docs.addAll(set);
        
        return docs;
    }
    
    
    
    private void ensureSubFoldersAreLoaded() {
        if (subFolders == null) {
            subFolders = loadSubFolders();
        }
    }
    
    private List<Folder> loadSubFolders() {
        List<Folder> folders = new ArrayList<Folder>();
        Node node = nodeModel.getNode();
        try {
            NodeIterator jcrChildren = node.getNodes();
            while (jcrChildren.hasNext()) {
                Node jcrChild = jcrChildren.nextNode();
                if (jcrChild != null ) {
                    if (!(jcrChild.isNodeType(HippoNodeType.NT_HANDLE) 
                            || jcrChild.isNodeType(HippoNodeType.NT_DOCUMENT)
                            || jcrChild.isNodeType(HippoNodeType.NT_FACETRESULT))) {
                        JcrNodeModel newNodeModel = new JcrNodeModel(jcrChild);
                        Folder subFolder = new Folder(newNodeModel);
                        folders.add(subFolder);
                    }
                }
            }
        }
        catch (RepositoryException e) {
            
        }
        
        return folders;
    }

}
