/*
 *  Copyright 2014-2019 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.intranet.beans;

import java.util.Calendar;

import org.hippoecm.hst.content.beans.Node;
import org.hippoecm.hst.content.beans.standard.HippoGalleryImageSetBean;
import org.hippoecm.hst.content.beans.standard.HippoHtml;

@Node(jcrType="intranet:newsdocument")
public class NewsDocument extends BaseDocument{

    public String getTitle() {
        return getSingleProperty("intranet:title");
    }
    
    public String getSummary() {
        return getSingleProperty("intranet:summary");
    }
    
    public Calendar getDate() {
        return getSingleProperty("intranet:date");
    }

    public HippoHtml getHtml(){
        return getHippoHtml("intranet:body");    
    }

    /**
     * Get the imageset of the newspage
     *
     * @return the imageset of the newspage
     */
    public HippoGalleryImageSetBean getImage() {
        return getLinkedBean("intranet:image", HippoGalleryImageSetBean.class);
    }


}
