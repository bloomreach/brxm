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
package org.hippoecm.hst.core.linking;

import javax.jcr.Node;

import org.hippoecm.hst.configuration.hosting.Mount;

public interface LocationResolver {

    /**
     * Implementations should here do their logic, possibly linkrewriting. With the resolved path from this method, a {@link HstLink} object
     * is created
     * @param node
     * @param mount the {@link Mount} where the HstLink should be created for 
     * @return the resolved HstLink for the node, or <code>null</code> when not able to create one
     * @deprecated unused since 7.9.0 : Use {@link #resolve(javax.jcr.Node,
     * org.hippoecm.hst.configuration.hosting.Mount, LocationMapTree)} instead
     */
    @Deprecated
    HstLink resolve(Node node, Mount mount);

    /**
     * Implementations should here do their logic, possibly linkrewriting. With the resolved path from this method, a {@link HstLink} object
     * is created. Do not store any of the arguments as instance variables as they should not be referenced from a LocationResolver
     * @param node
     * @param mount the {@link Mount} where the HstLink should be created for
     * @param locationMapTree inversed HstSiteMapItem's object
     * @return the resolved HstLink for the node, or <code>null</code> when not able to create one
     */
    HstLink resolve(Node node, Mount mount, LocationMapTree locationMapTree);
    
    /**
     * Since of 7.9.0, this method won't be invoked any more!
     * Of course, your LocationResolver might not need the locationMapTree at all. Then, just keep this method empty
     * @param locationMapTree
     * @deprecated unused since 7.9.0 : Use {@link #resolve(javax.jcr.Node,
     * org.hippoecm.hst.configuration.hosting.Mount, LocationMapTree)} instead.
     */
    @Deprecated
    void setLocationMapTree(LocationMapTree locationMapTree);
    
    /**
     * 
     * @return the node type this resolver works on
     */
    String getNodeType();
}
