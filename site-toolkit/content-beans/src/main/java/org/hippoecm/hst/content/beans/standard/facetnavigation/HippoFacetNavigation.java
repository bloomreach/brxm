/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.content.beans.standard.facetnavigation;

import java.util.ArrayList;
import java.util.List;

import org.hippoecm.hst.content.beans.Node;
import org.hippoecm.hst.content.beans.index.Indexable;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoDocumentBean;
import org.hippoecm.hst.content.beans.standard.HippoFacetChildNavigationBean;
import org.hippoecm.hst.content.beans.standard.HippoFacetNavigationBean;
import org.hippoecm.hst.content.beans.standard.HippoFolder;
import org.hippoecm.hst.content.beans.standard.HippoFolderBean;
import org.hippoecm.hst.content.beans.standard.HippoResultSetBean;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Indexable(ignore = true)
@Node(jcrType="hippofacnav:facetnavigation")
public class HippoFacetNavigation extends HippoFolder implements HippoFacetNavigationBean {
    
    private static Logger log = LoggerFactory.getLogger(HippoFacetNavigation.class);
    
    
    /**
     * HippoFacetNavigation does not return the HippoResultSetBean as a folder, as this is normally a 
     * folder that is not wanted to be displayed, and can be accessed through {@link #getResultSet()} already
     */
    @Override
    public List<HippoFolderBean> getFolders(boolean sorted){
        List<HippoFolderBean> folders = super.getFolders(sorted);
        
        List<HippoFolderBean> remove = new ArrayList<HippoFolderBean>();
        for(HippoFolderBean folder : folders) {
            if(folder instanceof HippoResultSetBean) {
                remove.add(folder);
            }
        }
        this.hippoFolders.removeAll(remove);
        return this.hippoFolders;
    }
    
    public Long getChildCountsCombined() {
        List<HippoFolderBean> folders = getFolders(false);
        Long combinedCount = 0L;
        for(HippoFolderBean folder : folders) {
            combinedCount += folder.getValueProvider().getLong(HippoNodeType.HIPPO_COUNT);
        }
        return combinedCount;
    }
    
    /**
     * because there are only documents below the resultset, we can return empty list here directly
     */
    @Override
    public List<HippoDocumentBean> getDocuments(boolean sorted) {
        return new ArrayList<HippoDocumentBean>();
    }

    @Override
    public boolean isLeaf() {
        // nodes.hasNodes is too expensive for faceted navigation. Only HippoFacetSubNavigation beans can be a leaf node.
        // hence, skip the test here and for the HippoFacetsAvailableNavigation
        return false;
    }
    
    public Long getCount() {
        return this.getProperty(HippoNodeType.HIPPO_COUNT);
    }

    public HippoResultSetBean getResultSet() {
        return this.getBean(HippoNodeType.HIPPO_RESULTSET);
    }

    public HippoFacetNavigationBean getRootFacetNavigationBean() {
        HippoBean bean = this;
        while(bean != null) {
            if(bean instanceof HippoFacetNavigationBean && !(bean instanceof HippoFacetChildNavigationBean)) {
                // found the HippoFacetNavigation bean
                return (HippoFacetNavigationBean)bean;
            }
            bean = bean.getParentBean();
        }
        log.info("Unable to return the HippoFacetNavigationBean for the current HippoFacetChildNavigationBean at '{}'. Return null", this.getPath());
        return null;
    }
}
