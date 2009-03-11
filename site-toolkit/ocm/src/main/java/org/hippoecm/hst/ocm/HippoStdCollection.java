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

@Node(jcrType="hippostd:folder")
public class HippoStdCollection extends HippoStdNode {
    
    public List<HippoStdCollection> getCollections() {
        List<HippoStdCollection> cols = null;
        
        if (getNode() == null || getSimpleObjectConverter() == null) {
            cols = Collections.emptyList();
        } else {
            cols = new LinkedList<HippoStdCollection>();

            try {
                javax.jcr.Node child = null;
                
                for (NodeIterator it = getNode().getNodes(); it.hasNext(); ) {
                    child = it.nextNode();
                    
                    if (child == null) {
                        continue;
                    } 
                    
                    if (!child.isNodeType(HippoNodeType.NT_HANDLE)) {
                        HippoStdCollection childCol = (HippoStdCollection) getSimpleObjectConverter().getObject(getNode().getSession(), child.getPath());
                        cols.add(childCol);
                    }
                }            
            } catch (RepositoryException e) {
                e.printStackTrace();
            }
        }
        
        return cols;
    }
    
    public List<HippoStdDocument> getDocuments() {
        List<HippoStdDocument> docs = null;

        if (getNode() == null || getSimpleObjectConverter() == null) {
            docs = Collections.emptyList();
        } else {
            docs = new LinkedList<HippoStdDocument>();
            
            try {
                javax.jcr.Node child = null;
                
                for (NodeIterator it = getNode().getNodes(); it.hasNext(); ) {
                    child = it.nextNode();
                    
                    if (child == null) {
                        continue;
                    } 
                    
                    if (child.isNodeType(HippoNodeType.NT_HANDLE)) {
                        javax.jcr.Node docNode = child.getNode(child.getName());
                        HippoStdDocument childDoc = (HippoStdDocument) getSimpleObjectConverter().getObject(docNode.getSession(), docNode.getPath());
                        docs.add(childDoc);
                    } else if(child.getParent().isNodeType(HippoNodeType.NT_HANDLE)){
                        HippoStdDocument childDoc = (HippoStdDocument) getSimpleObjectConverter().getObject(child.getSession(), child.getPath());
                        docs.add(childDoc);
                    }
                }            
            } catch (RepositoryException e) {
                e.printStackTrace();
            }
        }
        
        return docs;
    }
    
}
