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

import java.util.Calendar;

import org.hippoecm.hst.content.beans.Node;
import org.hippoecm.hst.content.beans.standard.HippoBean;

@Node(jcrType="demosite:newspage")
public class NewsPage extends TextPage{

    private ImageBean imageBean;
    private boolean imagesLoaded = false;
    
    @Override
    public Calendar getDate() {
        
        return getProperty("demosite:date");
    }

    /**
     * Get the image of the newspage
     *
     * @return the image of the newspage
     */
    public ImageBean getImage() {
        if(imagesLoaded) {
            return this.imageBean;
        }
        imagesLoaded = true;
        HippoBean bean = getBean("demosite:image");
        if(!(bean instanceof ImageLinkBean)) {
            return null;
        }
        ImageLinkBean imageLinkBean = (ImageLinkBean)bean;
        if(imageLinkBean == null) {
            return null;
        }
        this.imageBean = (ImageBean) imageLinkBean.getReferencedBean();
        return imageBean;
    }
 
}
