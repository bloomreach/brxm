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
import java.util.List;

import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.hippoecm.hst.content.beans.Node;
import org.hippoecm.hst.content.beans.ObjectBeanManagerException;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Node(jcrType="hippostd:folder")
public class HippoFolder extends HippoItem implements HippoFolderBean{
    private static Logger log = LoggerFactory.getLogger(HippoFolder.class);
    private List<HippoFolderBean> hippoFolders;
    private List<HippoDocumentBean> hippoDocuments;
    
    public List<HippoFolderBean> getFolders(){
         return this.getFolders(false);
    }
    
    public List<HippoFolderBean> getFolders(boolean sorted){
        if(this.hippoFolders != null) {
            return this.hippoFolders;
        }
        if(this.node == null) {
            log.warn("Cannot get documents because node is null");
            return null;
        }
        try {
            this.hippoFolders = new ArrayList<HippoFolderBean>();
            NodeIterator nodes = this.node.getNodes();
            while(nodes.hasNext()) {
                javax.jcr.Node child = nodes.nextNode();
                if(child == null) {continue;}
                HippoFolder hippoFolder = getHippoFolder(child);
                if(hippoFolder != null) {
                    this.hippoFolders.add(hippoFolder);
                }
            }
            if(sorted) {
                Collections.sort(this.hippoFolders);
            }
            return this.hippoFolders;
        } catch (RepositoryException e) {
            log.warn("Repository Exception : {}", e);
            return null;
        }
    }
    
    public int getDocumentSize(){
        return getDocuments().size();
    }
    
    public List<HippoDocumentBean> getDocuments() {
        return getDocuments(false);
    }
    
    public List<HippoDocumentBean> getDocuments(int from, int to) {
        return getDocuments(from,to,false);
    }
    
    public List<HippoDocumentBean> getDocuments(int from, int to, boolean sorted) {
        List<HippoDocumentBean> documents = getDocuments(sorted);
        if(from < 0) {from = 0;}
        if(from > documents.size()) {return new ArrayList<HippoDocumentBean>();}
        if(to > documents.size()){
        	to = documents.size();
        }
        return documents.subList(from, to);
    }
    
    public <T> List<T> getDocuments(Class<T> clazz) {
        List<HippoDocumentBean> documents = getDocuments();
        List<T> documentOfClass = new ArrayList<T>();
        for(HippoDocumentBean bean : documents) {
            if(clazz.isAssignableFrom(bean.getClass())) {
                documentOfClass.add((T)bean);
            }
        }
        return documentOfClass;
    }
    
    public List<HippoDocumentBean> getDocuments(boolean sorted) {
        if(this.hippoDocuments != null) {
            return this.hippoDocuments;
        }
        if(this.node == null) {
            log.warn("Cannot get documents because node is null");
            return null;
        }
        try {
            this.hippoDocuments = new ArrayList<HippoDocumentBean>();
            NodeIterator nodes = this.node.getNodes();
            while(nodes.hasNext()) {
                javax.jcr.Node child = nodes.nextNode();
                if(child == null) {continue;}
                HippoDocument hippoDocument = getHippoDocument(child);
                if(hippoDocument != null) {
                    this.hippoDocuments.add(hippoDocument);
                }
            }
            if(sorted) {
                Collections.sort(this.hippoDocuments);
            }
            return this.hippoDocuments;
        } catch (RepositoryException e) {
            log.warn("Repository Exception : {}", e);
            return null;
        }
    }
    
    
    private HippoFolder getHippoFolder(javax.jcr.Node child) {
        try {
            // folders inherit from HippoNodeType.NT_DOCUMENT
            if(child.isNodeType(HippoNodeType.NT_DOCUMENT)) {
                Object o  = objectConverter.getObject(child);
                if(o instanceof HippoFolder) {
                    return (HippoFolder)o;
                } else {
                    log.warn("Cannot return HippoFolder for. Return null '{}'", child.getPath());
                }
            }
        } catch (RepositoryException e) {
            log.error("Cannot return HippoFolder. Return null : {} " , e);
        } catch (ObjectBeanManagerException e) {
            log.warn("Cannot return HippoFolder. Return null : {} " , e);
        }
        return null;
    }
    
    private HippoDocument getHippoDocument(javax.jcr.Node child) {
        try {
            if(child.isNodeType(HippoNodeType.NT_HANDLE)) {
                if(child.hasNode(child.getName())) {
                    Object o  = objectConverter.getObject(child.getNode(child.getName()));
                    if(o instanceof HippoDocument) {
                        return (HippoDocument)o;
                    } else {
                        log.warn("Cannot return HippoDocument for. Return null '{}'", child.getPath());
                    }
                } 
                return null;
            } else if(child.getParent().isNodeType(HippoNodeType.NT_HANDLE)) {
                Object o  = (HippoDocument)objectConverter.getObject(child);
                if(o instanceof HippoDocument) {
                    return (HippoDocument)o;
                } else {
                    log.warn("Cannot return HippoDocument for. Return null '{}'", child.getPath());
                }
            }
        } catch (RepositoryException e) {
            log.error("Cannot return HippoDocument. Return null : {} " , e);
        } catch (ObjectBeanManagerException e) {
            log.warn("Cannot return HippoDocument. Return null : {} " , e);
        }
        return null;
    }

    
}
