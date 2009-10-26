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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.jcr.RepositoryException;

import org.hippoecm.hst.content.beans.Node;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.util.KeyValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Node(jcrType="hipposys:facetsubnavigation")
public class HippoFacetSubNavigation extends AbstractHippoFacetChildNavigation {
    
    private static Logger log = LoggerFactory.getLogger(HippoFacetSubNavigation.class);
    
    
    public KeyValue<String, String> getFacetValueCombi(){
        try {
            final String key = this.getNode().getParent().getName();
            final String value = this.getNode().getName();
            
            return new KeyValue<String, String>() {
                public String getKey() {
                    return key;
                }
                public String getValue() {
                    return value;
                }  
            };
        } catch (RepositoryException e) {
            log.error("Node must have a parent here. ", e);
        }
        return null;
    }
    
    /**
     * Returns the list of <code>HippoFacetSubNavigation</code>'s ancestors, where the closest ancestors are last in the list
     * @return the list of <code>HippoFacetSubNavigation</code>'s ancestors or an empty list if no ancestors of this type.  
     */
    public List<HippoFacetSubNavigation> getAncestors() {
        HippoFacetSubNavigation grandFatherBean = getGrandFatherBean(this);
        List<HippoFacetSubNavigation> ancestorList = new ArrayList<HippoFacetSubNavigation>();
        if(grandFatherBean != null) {
            ancestorList.add(grandFatherBean);
            while( (grandFatherBean = getGrandFatherBean(grandFatherBean)) != null ){
                ancestorList.add(grandFatherBean);
            }
        }
        Collections.reverse(ancestorList);
        return ancestorList;
    }
    
    private HippoFacetSubNavigation getGrandFatherBean(HippoFacetSubNavigation current){
        HippoBean parentBean = current.getParentBean();
        if(parentBean != null) {
            HippoBean grandFatherBean = parentBean.getParentBean();
            if(grandFatherBean != null && grandFatherBean instanceof HippoFacetSubNavigation) {
                return (HippoFacetSubNavigation)grandFatherBean;
            }
        }
        return null;
    }
    
}
