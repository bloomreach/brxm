/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.jaxrs.model.content;

import javax.jcr.RepositoryException;
import javax.xml.bind.annotation.XmlRootElement;

import org.hippoecm.hst.content.beans.standard.HippoGalleryImageSetBean;

/**
 * @version $Id$
 */
@XmlRootElement(name = "imageset")
public class HippoGalleryImageSetRepresentation extends NodeRepresentation {
    
    private String fileName;
    private String description;
    private HippoGalleryImageRepresentation thumbnail;
    private HippoGalleryImageRepresentation original;
    
    public HippoGalleryImageSetRepresentation represent(HippoGalleryImageSetBean bean) throws RepositoryException {
        super.represent(bean);
        fileName = bean.getFileName();
        description = bean.getDescription();
        thumbnail = new HippoGalleryImageRepresentation().represent(bean.getThumbnail());
        original = new HippoGalleryImageRepresentation().represent(bean.getOriginal());
        return this;
    }
    
    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public HippoGalleryImageRepresentation getThumbnail(){
        return thumbnail;
    }
    
    public void setThumbnail(HippoGalleryImageRepresentation thumbnail) {
      this.thumbnail = thumbnail;   
    }
    
    public HippoGalleryImageRepresentation getOriginal(){
        return original;
    }
    
    public void setOriginal(HippoGalleryImageRepresentation original) {
      this.original = original;   
    }
    
}
