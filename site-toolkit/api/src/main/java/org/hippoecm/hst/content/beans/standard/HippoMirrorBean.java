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

public interface HippoMirrorBean extends HippoBean {

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
    HippoBean getReferencedBean();

    /**
     * @deprecated since 2.28.00 use {@link #getReferencedBean()} instead
     */
    @Deprecated
    HippoBean getDeref();
}
