/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
import org.hippoecm.hst.content.beans.index.IndexField;
import org.hippoecm.hst.content.beans.standard.HippoAssetBean;
import org.hippoecm.hst.content.beans.standard.HippoGalleryImageSetBean;

@Node(jcrType="demosite:newsdocument")
public class NewsBean extends TextBean{

    private HippoGalleryImageSetBean imageBean;
    private boolean imagesLoaded;

    private HippoAssetBean videoBean;
    private boolean videoLoaded;

    @Override
    @IndexField
    public Calendar getDate() {
        return getProperty("demosite:date");
    }

    /**
     * Get the image of the newspage
     *
     * @return the image of the newspage
     */
    public HippoGalleryImageSetBean getImage() {
        if(imagesLoaded) {
            return this.imageBean;
        }
        imagesLoaded = true;
        // although the testsuite has imageset types demosite:qaimageset, the HST should fallback to built in HippoGalleryImageSetBean
        // because we do not have in the testsuite an explicit bean annotated with @Node(jcrType = "demosite:qaimageset")
        // and thus it should fallback to @Node(jcrType = "hippogallery:imageset")
        this.imageBean = this.getLinkedBean("demosite:image", HippoGalleryImageSetBean.class);
        return imageBean;
    }

    /**
     * Get the video of the newspage
     *
     * @return the video of the newspage
     */
    public HippoAssetBean getVideo() {
        if(videoLoaded) {
            return this.videoBean;
        }
        videoLoaded = true;
        this.videoBean = getLinkedBean("demosite:video", HippoAssetBean.class);
        return videoBean;
    }
}
