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
package org.hippoecm.hst.ocm;

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
public class HippoStdCollection extends HippoStdNode implements SessionAware {
    
    private static Logger log = LoggerFactory.getLogger(HippoStdCollection.class);
    
    private List<HippoStdCollection> childCollections;
    private List<HippoStdDocument> childDocuments;
    
   
    public List<HippoStdCollection> getCollections() {
        if (this.childCollections == null) {
            if (this.getSession() == null || getNode() == null || getSimpleObjectConverter() == null) {
                this.childCollections = Collections.emptyList();
            } else {
                this.childCollections = new LinkedList<HippoStdCollection>();
    
                try {
                    javax.jcr.Node child = null;
                    
                    for (NodeIterator it = getNode().getNodes(); it.hasNext(); ) {
                        child = it.nextNode();
                        
                        if (child == null) {
                            continue;
                        } 
                        
                        if (!child.isNodeType(HippoNodeType.NT_HANDLE)) {
                            HippoStdCollection childCol = (HippoStdCollection) getSimpleObjectConverter().getObject(this.getSession(), child.getPath());
                            this.childCollections.add(childCol);
                        }
                    }            
                } catch (RepositoryException e) {
                    if (log.isDebugEnabled()) {
                        log.warn("Cannot retrieve child collections: {}", e.getMessage(), e);
                    } else if (log.isWarnEnabled()) {
                        log.warn("Cannot retrieve child collections: {}", e.getMessage());
                    }
                }
                
                // Now detach the session because the session is probably from the pool.
                //setSession(null);
            }
        }
        
        return this.childCollections;
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
            if (this.getSession() == null || getNode() == null || getSimpleObjectConverter() == null) {
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
                            HippoStdDocument childDoc = (HippoStdDocument) getSimpleObjectConverter().getObject(this.getSession(), docNode.getPath());
                            this.childDocuments.add(childDoc);
                        } else if(child.getParent().isNodeType(HippoNodeType.NT_HANDLE)){
                            HippoStdDocument childDoc = (HippoStdDocument) getSimpleObjectConverter().getObject(this.getSession(), child.getPath());
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
