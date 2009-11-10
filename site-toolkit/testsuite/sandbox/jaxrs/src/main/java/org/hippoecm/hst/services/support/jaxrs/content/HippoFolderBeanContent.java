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
package org.hippoecm.hst.services.support.jaxrs.content;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;

import org.hippoecm.hst.content.beans.standard.HippoDocumentBean;
import org.hippoecm.hst.content.beans.standard.HippoFolderBean;
import org.hippoecm.repository.api.HippoNodeType;

@XmlRootElement(name = "folder")
public class HippoFolderBeanContent extends HippoBeanContent {
    
    private Collection<HippoFolderBeanContent> childFolderBeanContents;
    private Collection<HippoDocumentBeanContent> childDocumentBeanContents;
    private Collection<NodeContent> childNodeContents;
    
    public HippoFolderBeanContent() {
        super();
    }
    
    public HippoFolderBeanContent(String name) {
        super(name);
    }
    
    public HippoFolderBeanContent(String name, String path) {
        super(name, path);
    }
    
    public HippoFolderBeanContent(HippoFolderBean bean) throws RepositoryException {
        super(bean);
        
        Set<String> childNodePathSet = new HashSet<String>();
        
        childFolderBeanContents = new ArrayList<HippoFolderBeanContent>();
        
        for (HippoFolderBean fldrBean : bean.getFolders()) {
            if (fldrBean != null) {
                childFolderBeanContents.add(new HippoFolderBeanContent(fldrBean.getName(), fldrBean.getPath()));
                childNodePathSet.add(fldrBean.getPath());
            }
        }
        
        childDocumentBeanContents = new ArrayList<HippoDocumentBeanContent>();
        
        for (HippoDocumentBean docBean : bean.getDocuments()) {
            if (docBean != null) {
                childDocumentBeanContents.add(new HippoDocumentBeanContent(docBean.getName(), docBean.getPath()));
                childNodePathSet.add(docBean.getPath());
            }
        }

        childNodeContents = new ArrayList<NodeContent>();
        
        for (NodeIterator nodeIt = bean.getNode().getNodes(); nodeIt.hasNext(); ) {
            Node childNode = nodeIt.nextNode();
            
            if (childNode != null && !childNode.isNodeType(HippoNodeType.NT_HANDLE) && !childNodePathSet.contains(childNode.getPath())) {
                childNodeContents.add(new NodeContent(childNode.getName(), childNode.getPath()));
            }
        }
    }
    
    @XmlElements(@XmlElement(name="folder"))
    public Collection<HippoFolderBeanContent> getChildFolderBeanContents() {
        return childFolderBeanContents;
    }
    
    public void setChildFolderBeanContents(Collection<HippoFolderBeanContent> childFolderBeanContents) {
        this.childFolderBeanContents = childFolderBeanContents;
    }
    
    @XmlElements(@XmlElement(name="document"))
    public Collection<HippoDocumentBeanContent> getChildDocumentBeanContents() {
        return childDocumentBeanContents;
    }
    
    public void setChildDocumentBeanContents(Collection<HippoDocumentBeanContent> childDocumentBeanContents) {
        this.childDocumentBeanContents = childDocumentBeanContents;
    }
    
    @Override
    public void buildChildUris(String urlBase, String siteContentPath, String encoding) throws UnsupportedEncodingException {
        Collection<PropertyContent> propertyContents = getPropertyContents();
        
        if (propertyContents != null) {
            for (PropertyContent propertyContent : propertyContents) {
                propertyContent.buildUri(urlBase, siteContentPath, encoding);
            }
        }
        
        if (childFolderBeanContents != null) {
            for (HippoFolderBeanContent folderContent : childFolderBeanContents) {
                folderContent.buildUri(urlBase, siteContentPath, encoding);
            }
        }
        
        if (childDocumentBeanContents != null) {
            for (HippoDocumentBeanContent documentContent : childDocumentBeanContents) {
                documentContent.buildUri(urlBase, siteContentPath, encoding);
            }
        }
    }
    
    @Override
    public Collection<NodeContent> getChildNodeContents() {
        return childNodeContents;
    }
    
}
