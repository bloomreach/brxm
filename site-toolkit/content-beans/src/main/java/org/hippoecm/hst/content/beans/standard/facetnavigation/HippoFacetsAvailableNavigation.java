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
package org.hippoecm.hst.content.beans.standard.facetnavigation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hippoecm.hst.content.beans.Node;
import org.hippoecm.hst.content.beans.standard.HippoBean;

@Node(jcrType="hippofacnav:facetsavailablenavigation")
public class HippoFacetsAvailableNavigation extends AbstractHippoFacetChildNavigation {

    /**
     * the list of <code>HippoFacetsAvailableNavigation</code>'s ancestors + this HippoFacetsAvailableNavigation. Also see {@link #getAncestors()}
     * @return the list of <code>HippoFacetsAvailableNavigation</code>'s ancestors + this HippoFacetsAvailableNavigation
     */
    public List<HippoFacetsAvailableNavigation> getAncestorsAndSelf() {
        List<HippoFacetsAvailableNavigation> ancestorListAndSelf = getAncestors();
        ancestorListAndSelf.add(this);
        return ancestorListAndSelf;
    }
    /**
     * Returns the list of <code>HippoFacetsAvailableNavigation</code>'s ancestors, where the closest ancestors are last in the list
     * @return the list of <code>HippoFacetsAvailableNavigation</code>'s ancestors or an empty list if no ancestors of this type.  
     */
    public List<HippoFacetsAvailableNavigation> getAncestors() {
        HippoFacetsAvailableNavigation grandFatherBean = getGrandFatherBean(this);
        List<HippoFacetsAvailableNavigation> ancestorList = new ArrayList<HippoFacetsAvailableNavigation>();
        if(grandFatherBean != null) {
            ancestorList.add(grandFatherBean);
            while( (grandFatherBean = getGrandFatherBean(grandFatherBean)) != null ){
                ancestorList.add(grandFatherBean);
            }
        }
        Collections.reverse(ancestorList);
        return ancestorList;
    }
    
    private HippoFacetsAvailableNavigation getGrandFatherBean(HippoFacetsAvailableNavigation current){
        HippoBean parentBean = current.getParentBean();
        if(parentBean != null) {
            HippoBean grandFatherBean = parentBean.getParentBean();
            if(grandFatherBean != null && grandFatherBean instanceof HippoFacetsAvailableNavigation) {
                return (HippoFacetsAvailableNavigation)grandFatherBean;
            }
        }
        return null;
    }
}
