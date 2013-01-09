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
package org.hippoecm.hst.content.beans.standard;

import javax.jcr.RepositoryException;

import org.hippoecm.hst.content.beans.Node;
import org.hippoecm.hst.content.beans.ObjectBeanManagerException;
import org.hippoecm.hst.content.beans.index.Indexable;
import org.hippoecm.hst.util.NodeUtils;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Indexable(ignore = true)
@Node(jcrType="hippo:mirror")
public class HippoMirror extends HippoFolder implements HippoMirrorBean {

    private static Logger log = LoggerFactory.getLogger(HippoMirror.class);
    
    private BeanWrapper<HippoBean> referencedWrapper;
    private BeanWrapper<HippoBean> derefWrapper;
    
    /**
     * When you want the HippoBean that this mirror represents, you can use this method.
     * 
     * <ul><li>
     * If the mirror is pointing to a hippo:handle, then <b>only</b> a bean is returned if and only if
     * the current node has a childnode with the same name as the hippo:handle. This is to ensure live/preview
     * correctness. 
     * </li><li>
     * If the mirror is <code>not</code> pointing to a hippo:handle, just this <code>HippoMirror</code> is returned. 
     * This HippoMirror then namely represents its referencedBean already as it is a mirror, possibly with the context aware filter (livr/preview)
     * </li>
     * </ul>
     * @return the referenced <code>HippoBean</code> by this mirror or <code>null</code> when the linked bean cannot
     * be created (for example, there is no published and the current context is live)
     */
    public HippoBean getReferencedBean(){
        if(referencedWrapper != null) {
            return referencedWrapper.getBean();
        }
        
        if(this.getNode() == null) {
            log.warn("Can not dereference this HippoMirror because it is detached. Return null");
            referencedWrapper = new BeanWrapper<HippoBean>(null);
            return null;
        }
        javax.jcr.Node deref = NodeUtils.getDeref(this.getNode());
        if(deref == null) {
            // no logging needed, JCRUtilities already does this
            referencedWrapper = new BeanWrapper<HippoBean>(null);
            return null;
        }
        
        try {
            if (deref.isNodeType(HippoNodeType.NT_HANDLE)) {
                /*
                 * the link is to a hippo:handle. Only return the linked bean if, and only if, the same node is also visible
                 * as a child node below the current virtual node. If not, it is filtered and should not be visible, for example
                 * in case that the context is live (published), and the linked bean is only available as unpublished
                 */  
                 if(this.getNode().hasNode(deref.getName())) {
                     javax.jcr.Node linked = this.getNode().getNode(deref.getName());
                     referencedWrapper = new BeanWrapper<HippoBean>((HippoBean) this.objectConverter.getObject(linked));
                     return referencedWrapper.getBean();
                 } else {
                     referencedWrapper = new BeanWrapper<HippoBean>(null);
                     return null;
                 }
            } 
            referencedWrapper = new BeanWrapper<HippoBean>(this);
            return this;
        } catch (RepositoryException e) {
            log.warn("Cannot get a derefenced HippoBean: {}. Return null", e);
        } catch (ObjectBeanManagerException e) {
            log.warn("Cannot get a derefenced HippoBean: {}. Return null", e.toString());
        }
        referencedWrapper = new BeanWrapper<HippoBean>(null);
        return null;
    }
    
    /**
     * When you want the HippoBean that this mirror is pointing to, you can use this method. 
     * 
     * WARNING: A deref breaks out of the virtual context, and might return you a node structure, where you find
     * published and unpublished nodes. Recommended to use is {@link #getReferencedBean()}. 
     * 
     * Note that this might return you a handle containing both live and preview documents. You should not use this
     * method to get a linked item. Use {@link #getReferencedBean()}. 
     * 
     * @return the deferenced <code>HippoBean</code> or <code>null</code> when the bean cannot be dereferenced
     */
    public HippoBean getDeref(){
        if(derefWrapper != null) {
            return derefWrapper.getBean();
        }
        if(this.getNode() == null) {
            log.warn("Can not dereference this HippoMirror because it is detached. Return null");
            derefWrapper = new BeanWrapper<HippoBean>(null);
            return null;
        }
        javax.jcr.Node deref = NodeUtils.getDeref(this.getNode());
        if(deref == null) {
            log.warn("Can not dereference this HippoMirror ('{}') because cannot find the node the mirror is pointing to. Return null", this.getPath());
            derefWrapper = new BeanWrapper<HippoBean>(null);
            return null;
        }
        
        try {
            derefWrapper = new BeanWrapper<HippoBean>((HippoBean) this.objectConverter.getObject(deref));
            return derefWrapper.getBean();
        } catch (ObjectBeanManagerException e) {
            log.warn("Cannot get a derefenced HippoBean: {}. Return null", e.toString());
        }
        derefWrapper = new BeanWrapper<HippoBean>(null);
        return null;
    }

}
