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

import java.util.ArrayList;
import java.util.List;

import org.hippoecm.hst.content.beans.Node;
import org.hippoecm.hst.content.beans.index.IndexField;
import org.hippoecm.hst.content.beans.standard.HippoBean;

@Node(jcrType = "demosite:wikidocument")
public class WikiBean extends BaseBean {
    private String title;
    private String[] categories;

    @Override
    @IndexField
    public String getTitle() {
        return title == null ? (String) getProperty("demosite:title") : title;
    }

    @Override
    public void setTitle(String title) {
        this.title = title;
    }

    public PlaceTimeBean getPlacetime() {
        return getBean("demosite:placetime", PlaceTimeBean.class);
    }

    @IndexField
    public String[] getCategories() {
        return categories == null ? (String[]) getProperty("demosite:categories") : categories;
    }

    public void setCategories(String[] cat) {
        categories = cat;
    }

    public List<BlockBean> getBlocks() {
        return getChildBeans(BlockBean.class);
    }

    public List<HippoBean> getRelatedDocs() {
        RelatedDocsBean rd = getBean("relateddocs:docs", RelatedDocsBean.class);
        if (rd != null) {
            return rd.getRelatedDocs("demosite:wikidocument");
        } else {
            return new ArrayList<HippoBean>();
        }
    }
}
