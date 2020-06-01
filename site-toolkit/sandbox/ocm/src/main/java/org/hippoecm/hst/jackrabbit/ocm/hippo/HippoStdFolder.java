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
package org.hippoecm.hst.jackrabbit.ocm.hippo;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.ocm.mapper.impl.annotation.Node;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Node(jcrType="hippostd:folder", discriminator=false)
public class HippoStdFolder extends HippoStdNode {
    
    private static Logger log = LoggerFactory.getLogger(HippoStdFolder.class);
    
    private List<HippoStdFolder> childFolders;
    private List<HippoStdDocument> childDocuments;
    
   
    public List<HippoStdFolder> getFolders() {
        if (this.childFolders == null) {
            if (getNode() == null || getSimpleObjectConverter() == null) {
                this.childFolders = Collections.emptyList();
            } else {
                this.childFolders = new LinkedList<HippoStdFolder>();
    
                try {
                    javax.jcr.Node child = null;
                    
                    for (NodeIterator it = getNode().getNodes(); it.hasNext(); ) {
                        child = it.nextNode();
                        
                        if (child == null) {
                            continue;
                        } 
                        
                        if (!child.isNodeType(HippoNodeType.NT_HANDLE)) {
                            HippoStdFolder childCol = (HippoStdFolder) getSimpleObjectConverter().getObject(getNode().getSession(), child.getPath());
                            this.childFolders.add(childCol);
                        }
                    }            
                } catch (RepositoryException e) {
                    if (log.isDebugEnabled()) {
                        log.warn("Cannot retrieve child folders: {}", e.getMessage(), e);
                    } else if (log.isWarnEnabled()) {
                        log.warn("Cannot retrieve child folders: {}", e.getMessage());
                    }
                }
                
                // Now detach the session because the session is probably from the pool.
                //setSession(null);
            }
        }
        
        return this.childFolders;
    }
    
    public List<HippoStdDocument> getDocuments(int from, int to) {
        List<HippoStdDocument> docs =  getDocuments();
        List<HippoStdDocument> subList = new LinkedList<HippoStdDocument>();
        while(from < to) {
            if(docs.size() <= from) {
                return subList;
            }
            subList.add(docs.get(from));
            from++;
        }
        return subList;
    }
    
    public int getDocumentSize() {
        return getDocuments().size();
    }
    
    
    public List<HippoStdDocument> getDocuments() {
        if (this.childDocuments == null) {
            if (getNode() == null || getSimpleObjectConverter() == null) {
                this.childDocuments = Collections.emptyList();
            } else {
                this.childDocuments = new LinkedList<HippoStdDocument>();
                
                try {
                    javax.jcr.Node child = null;
                    
                    for (NodeIterator it = getNode().getNodes(); it.hasNext(); ) {
                        child = it.nextNode();
                        
                        if (child == null) {
                            continue;
                        } 
                        
                        if (child.isNodeType(HippoNodeType.NT_HANDLE)) {
                            javax.jcr.Node docNode = child.getNode(child.getName());
                            HippoStdDocument childDoc = (HippoStdDocument) getSimpleObjectConverter().getObject(getNode().getSession(), docNode.getPath());
                            this.childDocuments.add(childDoc);
                        } else if(child.getParent().isNodeType(HippoNodeType.NT_HANDLE)){
                            HippoStdDocument childDoc = (HippoStdDocument) getSimpleObjectConverter().getObject(getNode().getSession(), child.getPath());
                            this.childDocuments.add(childDoc);
                        }
                    }            
                } catch (RepositoryException e) {
                    if (log.isDebugEnabled()) {
                        log.warn("Cannot retrieve child documents: {}", e.getMessage(), e);
                    } else if (log.isWarnEnabled()) {
                        log.warn("Cannot retrieve child documents: {}", e.getMessage());
                    }
                }

                // Now detach the session because the session is probably from the pool.
                //setSession(null);
            }
        }
        
        return this.childDocuments;
    }
    
    
}
