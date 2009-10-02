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
 * Bean mapping class for the 'hippogallery:exampleImageSet' document type
 *
 */
@Node(jcrType = "hippogallery:exampleImageSet")
public class HippoImage extends HippoItem implements HippoImageBean {

    private static Logger log = LoggerFactory.getLogger(HippoImage.class);

    private BeanWrapper<HippoResourceBean> thumbnail;
    private BeanWrapper<HippoResourceBean> picture; 
    
    /**
     * Get the thumbnail version of the image.
     *
     * @return the thumbnail version of the image
     */
    public HippoResourceBean getThumbnail() {
        if(thumbnail != null) {
            return thumbnail.getBean();
        }
        thumbnail = getResourceBean("hippogallery:thumbnail");
        return thumbnail.getBean();
    }

    /**
     * Get the picture version of the image.
     *
     * @return the picture version of the image
     */
    public HippoResourceBean getPicture() {
        if(picture != null) {
            return picture.getBean();
        }
        picture = getResourceBean("hippogallery:picture");
        return picture.getBean();
    }

    private BeanWrapper<HippoResourceBean> getResourceBean(String relPath){
        BeanWrapper<HippoResourceBean> resource; 
        HippoBean bean = this.getBean(relPath);
        if(bean instanceof HippoResourceBean) {
            resource = new BeanWrapper<HippoResourceBean>((HippoResourceBean)bean);
        } else if(bean == null) {
           log.debug(relPath + " not found for node '{}'", this.getPath());
           resource =  new BeanWrapper<HippoResourceBean>((HippoResourceBean)null);
        } else {
            log.warn("Expected resource of type HippoResourceBean but found '{}'. Return null", bean.getClass().getName() );
            resource =  new BeanWrapper<HippoResourceBean>((HippoResourceBean)null);
        }
        return resource;
    }
    
}
