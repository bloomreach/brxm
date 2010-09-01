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
package org.hippoecm.hst.content.beans.standard.facetnavigation;

import org.hippoecm.hst.content.beans.Node;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoFacetChildNavigationBean;
import org.hippoecm.hst.content.beans.standard.HippoFacetNavigationBean;
import org.hippoecm.hst.content.beans.standard.HippoFolder;
import org.hippoecm.hst.content.beans.standard.HippoResultSetBean;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Node(jcrType="hippofacnav:facetnavigation")
public class HippoFacetNavigation extends HippoFolder implements HippoFacetNavigationBean {
    
    private static Logger log = LoggerFactory.getLogger(HippoFacetNavigation.class);
    
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
        log.warn("Unable to return the HippoFacetNavigationBean for the current HippoFacetChildNavigationBean at '{}'. Return null", this.getPath());
        return null;
    }
}
