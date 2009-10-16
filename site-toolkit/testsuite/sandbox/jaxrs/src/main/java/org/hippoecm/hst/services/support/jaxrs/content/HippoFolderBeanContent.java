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

import java.util.ArrayList;

import javax.jcr.RepositoryException;
import javax.xml.bind.annotation.XmlRootElement;

import org.hippoecm.hst.content.beans.standard.HippoDocumentBean;
import org.hippoecm.hst.content.beans.standard.HippoFolderBean;

@XmlRootElement(name = "folder")
public class HippoFolderBeanContent extends HippoBeanContent {
    
    private ItemContent [] folder;
    private ItemContent [] document;
    
    public HippoFolderBeanContent() {
        super();
    }
    
    public HippoFolderBeanContent(HippoFolderBean bean) throws RepositoryException {
        super(bean);
        
        ArrayList<ItemContent> folderItemContents = new ArrayList<ItemContent>();
        for (HippoFolderBean fldrBean : bean.getFolders()) {
            if (fldrBean != null) {
                folderItemContents.add(new ItemContent(fldrBean.getNode()));
            }
        }
        folder = new ItemContent[folderItemContents.size()];
        folder = folderItemContents.toArray(folder);
        
        ArrayList<ItemContent> documentItemContents = new ArrayList<ItemContent>();
        for (HippoDocumentBean docBean : bean.getDocuments()) {
            if (docBean != null) {
                documentItemContents.add(new ItemContent(docBean.getNode()));
            }
        }
        document = new ItemContent[documentItemContents.size()];
        document = documentItemContents.toArray(document);
    }
    
    public ItemContent [] getFolder() {
        return folder;
    }
    
    public void setFolder(ItemContent [] folder) {
        this.folder = folder;
    }
    
    public ItemContent [] getDocument() {
        return document;
    }
    
    public void setDocument(ItemContent [] document) {
        this.document = document;
    }
    
}
