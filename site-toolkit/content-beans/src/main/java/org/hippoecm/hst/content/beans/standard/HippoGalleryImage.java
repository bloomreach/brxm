/*
 * Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *  http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.content.beans.standard;

import org.hippoecm.hst.content.beans.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bean mapping class for the 'hippogallery:image' document type
 */

@Node(jcrType = "hippogallery:image")
public class HippoGalleryImage extends HippoResource implements HippoGalleryImageBean{
    
    private static Logger log = LoggerFactory.getLogger(HippoGalleryImage.class);
    
    public int getHeight() {
        if(!getValueProvider().hasProperty("hippogallery:height")) {
            log.debug("no height property available for image '{}'. Return -1", getValueProvider().getPath());
            return -1;
        }
        return this.getValueProvider().getLong("hippogallery:height").intValue();
    }

    public int getWidth() {
        if(!getValueProvider().hasProperty("hippogallery:width")) {
            log.debug("no width property available for image '{}'. Return -1", getValueProvider().getPath());
            return -1;
        }
        return this.getValueProvider().getLong("hippogallery:width").intValue();
    }

}
