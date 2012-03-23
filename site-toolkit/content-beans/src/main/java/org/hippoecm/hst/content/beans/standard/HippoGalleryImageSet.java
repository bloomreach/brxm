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

import org.hippoecm.hst.content.beans.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Bean mapping class for the 'hippogallery:imageset' document type
 */

@Node(jcrType = "hippogallery:imageset")
public class HippoGalleryImageSet extends HippoDocument implements HippoGalleryImageSetBean {

    private static Logger log = LoggerFactory.getLogger(HippoGalleryImageSet.class);

    private BeanWrapper<HippoGalleryImageBean> thumbnail;
    private BeanWrapper<HippoGalleryImageBean> original; 
    

    @Override
    public String getDescription() {
        return getValueProvider().getString("hippogallery:description");
    }

    @Override
    public String getFileName() {
        return getValueProvider().getString("hippogallery:filename");
    }
    
    /**
     * Get the thumbnail version of the image.
     *
     * @return the thumbnail version of the image
     */
    public HippoGalleryImageBean getThumbnail() {
        if(thumbnail != null) {
            return thumbnail.getBean();
        }
        thumbnail = getHippoGalleryImageBean("hippogallery:thumbnail");
        return thumbnail.getBean();
    }

    /**
     * Get the picture version of the image.
     *
     * @return the picture version of the image
     */
    public HippoGalleryImageBean getOriginal() {
        if(original != null) {
            return original.getBean();
        }
        original = getHippoGalleryImageBean("hippogallery:original");
        return original.getBean();
    }

    private BeanWrapper<HippoGalleryImageBean> getHippoGalleryImageBean(String relPath){
        BeanWrapper<HippoGalleryImageBean> image; 
        HippoBean bean = this.getBean(relPath);
        if(bean instanceof HippoGalleryImageBean) {
            image = new BeanWrapper<HippoGalleryImageBean>((HippoGalleryImageBean)bean);
        } else if(bean == null) {
           log.debug(relPath + " not found for node '{}'", this.getPath());
           image =  new BeanWrapper<HippoGalleryImageBean>((HippoGalleryImageBean)null);
        } else {
            log.warn("Expected resource of type HippoGalleryImageBean but found '{}'. Return null", bean.getClass().getName() );
            image =  new BeanWrapper<HippoGalleryImageBean>((HippoGalleryImageBean)null);
        }
        return image;
    }

    
}
