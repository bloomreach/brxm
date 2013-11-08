/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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

package ${beansPackage};

import java.util.Calendar;

import org.hippoecm.hst.content.beans.Node;
import org.hippoecm.hst.content.beans.standard.HippoGalleryImageSetBean;
import org.hippoecm.hst.content.beans.standard.HippoHtml;
import org.onehippo.cms7.essentials.dashboard.annotations.HippoEssentialsGenerated;

@HippoEssentialsGenerated(internalName = "${namespace}:newsdocument")
@Node(jcrType = "${namespace}:newsdocument")
public class NewsDocument extends BaseDocument {
    @HippoEssentialsGenerated(internalName = "${namespace}:title")
    public String getTitle() {
        return getProperty("${namespace}:title");
    }

    @HippoEssentialsGenerated(internalName = "${namespace}:summary")
    public String getSummary() {
        return getProperty("${namespace}:summary");
    }

    @HippoEssentialsGenerated(internalName = "${namespace}:date")
    public Calendar getDate() {
        return getProperty("${namespace}:date");
    }

    @HippoEssentialsGenerated(internalName = "${namespace}:body")
    public HippoHtml getHtml() {
        return getHippoHtml("${namespace}:body");
    }

    /**
     * Get the imageset of the newspage
     *
     * @return the imageset of the newspage
     */
    @HippoEssentialsGenerated(internalName = "${namespace}:image")
    public HippoGalleryImageSetBean getImage() {
        return getLinkedBean("${namespace}:image",
                HippoGalleryImageSetBean.class);
    }
}
