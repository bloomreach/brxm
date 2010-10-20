/*
 *  Copyright 2010 Hippo.
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
package org.hippoecm.hst.demo.jaxrs.model;

import javax.jcr.RepositoryException;
import javax.xml.bind.annotation.XmlRootElement;

import org.hippoecm.hst.content.beans.standard.HippoImageBean;
import org.hippoecm.hst.jaxrs.model.content.NodeRepresentation;

/**
 * @version $Id$
 */
@XmlRootElement(name = "imageSet")
public class ImageSetRepresentation extends NodeRepresentation {
    
    HippoResourceRepresentation thumbnail;
    HippoResourceRepresentation picture;
    
    public ImageSetRepresentation represent(HippoImageBean bean) throws RepositoryException {
        super.represent(bean);
        thumbnail = new HippoResourceRepresentation().represent(bean.getThumbnail());
        picture = new HippoResourceRepresentation().represent(bean.getPicture());
        return this;
    }

    public HippoResourceRepresentation getThumbnail(){
        return thumbnail;
    }
    
    public void setThumbnail(HippoResourceRepresentation thumbnail) {
      this.thumbnail = thumbnail;   
    }
    
    public HippoResourceRepresentation getPicture(){
        return picture;
    }
    
    public void setPicture(HippoResourceRepresentation picture) {
      this.picture = picture;   
    }
    
}
