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
package org.hippoecm.hst.content.beans.standard;

import org.hippoecm.hst.content.beans.Node;
import org.hippoecm.hst.content.beans.ObjectBeanManagerException;
import org.hippoecm.hst.provider.jcr.JCRUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Node(jcrType="hippo:facetselect")
public class HippoFacetSelect extends HippoFolder{

    private static Logger log = LoggerFactory.getLogger(HippoFacetSelect.class);
    
    /**
     * When you want the HippoBean that this facetSelect is pointing to, you can use this method
     * @return the deferenced <code>HippoBean</code> or <code>null</code> when the bean cannot be dereferenced
     */
    public HippoBean getDeref(){
        if(this.getNode() == null) {
            log.warn("Can not dereference this HippoFacetSelect because it is detached. Return null");
            return null;
        }
        javax.jcr.Node deref = JCRUtilities.getDeref(this.getNode());
        if(deref == null) {
            log.warn("Can not dereference this HippoFacetSelect because cannot find the node the facetselect is pointing to. Return null");
            return null;
        }
        try {
            return (HippoBean) this.objectConverter.getObject(deref);
        } catch (ObjectBeanManagerException e) {
           log.warn("Cannot get a derefenced HippoBean: {}. Return null", e.getMessage());
        }
        return null;
    }
}
