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
package org.hippoecm.hst.demo.beans;

import org.hippoecm.hst.content.beans.Node;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoItem;
import org.hippoecm.hst.content.beans.standard.HippoResource;


/**
 * Bean mapping class for the 'hippogallery:exampleImageSet' document type
 *
 * @author Roberto van der Linden
 * @author Jeroen Reijn
 */
@Node(jcrType = "hippogallery:exampleImageSet")
public class ImageBean extends HippoItem implements HippoBean {

    /**
     * Get the thumbnail version of the image.
     *
     * @return the thumbnail version of the image
     */
    public HippoResource getThumbnail() {
        return getBean("hippogallery:thumbnail");
    }

    /**
     * Get the picture version of the image.
     *
     * @return the picture version of the image
     */
    public HippoResource getPicture() {
        return getBean("hippogallery:picture");
    }

}
