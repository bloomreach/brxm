/*
 * Copyright 2018-2019 Hippo B.V. (http://www.onehippo.com)
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
package {{beansPackage}};

import java.util.Calendar;
import java.util.List;

import org.hippoecm.hst.content.beans.Node;
import org.hippoecm.hst.content.beans.standard.HippoGalleryImageSetBean;
import org.hippoecm.hst.content.beans.standard.HippoHtml;

@Node(jcrType="{{namespace}}:contentblocksdocument")
public class ContentBlocksDocument extends BaseDocument {

    public String getTitle() {
        return getProperty("{{namespace}}:title");
    }
    
    public String getSummary() {
        return getProperty("{{namespace}}:summary");
    }
    
    public Calendar getDate() {
        return getProperty("{{namespace}}:date");
    }

    public HippoHtml getHtml(){
        return getHippoHtml("{{namespace}}:body");
    }

    public List<?> getVideos() {
        return getChildBeans("{{namespace}}:videoblock");
    }

    public List<?> getImages() {
        return getChildBeans("{{namespace}}:imageblock");
    }

    public List<?> getTexts() {
        return getChildBeans("{{namespace}}:textblock");
    }

    /**
     * Get the imageset of the newspage
     *
     * @return the imageset of the newspage
     */
    public HippoGalleryImageSetBean getImage() {
        return getLinkedBean("{{namespace}}:image", HippoGalleryImageSetBean.class);
    }


}
