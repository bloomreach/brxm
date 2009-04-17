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
package org.hippoecm.hst.content.beans.standard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.hippoecm.hst.content.beans.Node;
import org.hippoecm.hst.content.beans.ObjectBeanManagerException;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Node(jcrType="hippostd:folder")
public class HippoFolder extends HippoItem{
    private static Logger log = LoggerFactory.getLogger(HippoFolder.class);
    
    
    public List<HippoFolder> getFolders(){
        return this.getFolders(null);
    }
    public List<HippoFolder> getFolders(Comparator<HippoFolder> comparator){
        if(this.node == null) {
            log.warn("Cannot get documents because node is null");
            return null;
        }
        try {
            List<HippoFolder> hippoFolders = new ArrayList<HippoFolder>();
            NodeIterator nodes = this.node.getNodes();
            while(nodes.hasNext()) {
                javax.jcr.Node child = nodes.nextNode();
                if(child == null) {continue;}
                HippoFolder hippoFolder = getHippoFolder(child);
                if(hippoFolder != null) {
                    hippoFolders.add(hippoFolder);
                }
            }
            if(comparator != null) {
                Collections.sort(hippoFolders, comparator);
            }
            return hippoFolders;
        } catch (RepositoryException e) {
            log.warn("Repository Exception : {}", e);
            return null;
        }
    }
    
    public List<HippoDocument> getDocuments(){
        return this.getDocuments(null);
        
    }
    public List<HippoDocument> getDocuments(Comparator<HippoDocument> comparator) {
        if(this.node == null) {
            log.warn("Cannot get documents because node is null");
            return null;
        }
        try {
            List<HippoDocument> hippoDocuments = new ArrayList<HippoDocument>();
            NodeIterator nodes = this.node.getNodes();
            while(nodes.hasNext()) {
                javax.jcr.Node child = nodes.nextNode();
                if(child == null) {continue;}
                HippoDocument hippoDocument = getHippoDocument(child);
                if(hippoDocument != null) {
                    hippoDocuments.add(hippoDocument);
                }
            }
            if(comparator != null) {
                Collections.sort(hippoDocuments, comparator);
            }
            return hippoDocuments;
        } catch (RepositoryException e) {
            log.warn("Repository Exception : {}", e);
            return null;
        }
    }
    
    
    private HippoFolder getHippoFolder(javax.jcr.Node child) {
        try {
            // folders inherit from HippoNodeType.NT_DOCUMENT
            if(child.isNodeType(HippoNodeType.NT_DOCUMENT)) {
                return (HippoFolder)objectConverter.getObject(child);
            }
        } catch (RepositoryException e) {
            // TODO
            e.printStackTrace();
        } catch (ObjectBeanManagerException e) {
            // TODO
            e.printStackTrace();
        }
        return null;
    }
    
    private HippoDocument getHippoDocument(javax.jcr.Node child) {
        try {
            if(child.isNodeType(HippoNodeType.NT_HANDLE)) {
                if(child.hasNode(child.getName())) {
                    return (HippoDocument)objectConverter.getObject(child.getNode(child.getName()));
                } 
                return null;
            } else if(child.getParent().isNodeType(HippoNodeType.NT_HANDLE)) {
                return (HippoDocument)objectConverter.getObject(child);
            }
        } catch (RepositoryException e) {
            // TODO
            e.printStackTrace();
        } catch (ObjectBeanManagerException e) {
            // TODO
            e.printStackTrace();
        }
        return null;
    }
}
