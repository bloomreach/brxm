/*
 *  Copyright 2008-2019 Hippo B.V. (http://www.onehippo.com)
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

import javax.jcr.ItemNotFoundException;
import javax.jcr.RepositoryException;

import org.hippoecm.hst.content.beans.Node;
import org.hippoecm.hst.content.beans.ObjectBeanManagerException;
import org.hippoecm.hst.content.beans.index.Indexable;
import org.hippoecm.repository.api.HippoNodeType;
import org.onehippo.cms7.essentials.dashboard.annotations.HippoEssentialsGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Indexable(ignore = true)
@Node(jcrType="hippo:mirror")
@HippoEssentialsGenerated(allowModifications = false)
public class HippoMirror extends HippoFolder implements HippoMirrorBean {

    private static Logger log = LoggerFactory.getLogger(HippoMirror.class);

    /**
     * <p>
     *      When you want the HippoBean that this mirror represents, you can use this method.
     * </p>
     * <p>
     *      If the mirror is pointing to a <code>hippo:handle</code>, then <b>only</b> a {@link HippoBean} is returned
     *      if a child node (document) with the same name is present.
     *      A {@link HippoBean} for the document (child) is then returned. If no such child,
     *      <code>null</code> is returned.
     * </p>
     * <p>
     *     If the mirror point to a node that is not of type <code>hippo:handle</code>, a {@link HippoBean} for that
     *     node is returned.
     * </p>
     * @return the referenced <code>HippoBean</code> by this mirror or <code>null</code> when missing
     */
    public HippoBean getReferencedBean() {

        if (node == null) {
            log.info("Can not dereference this HippoMirror because it is detached. Return null");
            return null;
        }
        try {
            if (!node.hasProperty(HippoNodeType.HIPPO_DOCBASE)) {
                return null;
            }

            javax.jcr.Node deref = node.getSession().getNodeByIdentifier(node.getProperty(HippoNodeType.HIPPO_DOCBASE).getString());
            if (deref == null) {
                return null;
            }

            if (deref.isNodeType(HippoNodeType.NT_HANDLE)) {
                /*
                 * the link is to a hippo:handle. Only return the linked bean if a child node (document) with the same name is
                 * present, and return a bean for that one if there is. Otherwise return null
                 */
                if (deref.hasNode(deref.getName())) {
                    javax.jcr.Node linked = deref.getNode(deref.getName());
                    return (HippoBean) objectConverter.getObject(linked);
                } else {
                    return null;
                }
            }
            return (HippoBean) objectConverter.getObject(deref);
        } catch (ItemNotFoundException | ObjectBeanManagerException e) {
            log.info("Cannot get a dereferenced HippoBean, return null", e);
        } catch (RepositoryException e) {
            log.warn("Cannot get a dereferenced HippoBean, return null", e);
        }
        return null;
    }

}
