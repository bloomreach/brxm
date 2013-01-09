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

import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Session;

import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.site.HstSite;

/**
 * Implementations that know how to rewrite a link for a nodetype containing resources, 
 * like hippogallery:exampleImageSet or hippogallery:exampleAssetSet
 *
 */
public interface ResourceContainer {
  
    /**
    * Implementations should here do their logic, possibly linkrewriting. With the resolved path from this method, a {@link HstLink} object
    * is created
    * @param resourceContainerNode The parent node of the resource node
    * @param resourceNode The resource node itself containing the binary
    * @param mount the {@link Mount} the link is meant for
    * @return the resolved pathInfo for the node, or <code>null</code> when not able to create one
    */
   String resolveToPathInfo(Node resourceContainerNode, Node resourceNode, Mount mount);
    
    /**
     * This is the reverse of {@link #resolvePathInfo(Node, Node, HstSite)}. If this ResourceContainer can resolve
     * the pathInfo to a resource node of type {@link #getNodeType()}, it returns the resourceNode. If it cannot resolve the
     * pathInfo, <code>null</null> is returned
     * @param session
     * @param pathInfo : the path from the url after the context and servlet path. It starts with a slash
     * @param object 
     * @return the resourceNode or <code>null</code> if this resource container does not know how to resolve this pathInfo
     */
    Node resolveToResourceNode(Session session, String pathInfo);
    
    /**
     * 
     * @return the node type this resource container works on
     */
    String getNodeType();
    
    /**
     * 
     * @return the primary item for this resource container 
     */
    String getPrimaryItem();
    
    /**
     * returns the mapping from nodename to url prefix. For example, hippogallery:thumbnail <--> thumbnail
     * @return the mapping from nodename to url prefix
     */
    Map<String, String> getMappings();
}
