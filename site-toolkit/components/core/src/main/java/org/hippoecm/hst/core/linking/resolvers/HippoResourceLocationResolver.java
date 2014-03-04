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
package org.hippoecm.hst.core.linking.resolvers;

import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.core.linking.HstLink;
import org.hippoecm.hst.core.linking.HstLinkImpl;
import org.hippoecm.hst.core.linking.LocationMapTree;
import org.hippoecm.hst.core.linking.ResourceContainer;
import org.hippoecm.hst.core.linking.ResourceLocationResolver;
import org.hippoecm.hst.util.PathUtils;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HippoResourceLocationResolver implements ResourceLocationResolver {
    
    private static final Logger log = LoggerFactory.getLogger(HippoResourceLocationResolver.class);
    private String[] binaryLocations;
    private String binariesPrefix;
    private List<ResourceContainer> resourceContainers;
    
    private final static String NODE_TYPE = HippoNodeType.NT_RESOURCE;

    /**
     * @deprecated unused since 7.9.0 : Use {@link #resolve(javax.jcr.Node, org.hippoecm.hst.configuration.hosting.Mount,
     *             LocationMapTree)} instead
     */
    @Deprecated
    public void setLocationMapTree(LocationMapTree locationMapTree) {
        // we do not need a locationMapTree one for binary data
    }
    
    public void setBinariesPrefix(String binariesPrefix){
        this.binariesPrefix = binariesPrefix;
    }
    
    public void setBinaryLocations(String[] binaryLocations) {
        if (binaryLocations == null) {
            this.binaryLocations = null;
        } else {
            this.binaryLocations = new String[binaryLocations.length];
            System.arraycopy(binaryLocations, 0, this.binaryLocations, 0, binaryLocations.length);
        }
    }
    
    public void setResourceContainers(List<ResourceContainer> resourceContainers) {
        this.resourceContainers = resourceContainers;
    }
    
    public List<ResourceContainer> getResourceContainers(){
        return this.resourceContainers;
    }
    
    public String getNodeType() {
        return NODE_TYPE;
    }

    /**
     * @deprecated unused since 7.9.0 : Use {@link #resolve(javax.jcr.Node, org.hippoecm.hst.configuration.hosting.Mount,
     *             LocationMapTree)} instead
     */
    @Deprecated
    public HstLink resolve(final Node node, final Mount mount) {
        log.warn("This method is deprecated. Use resolve(Node, Mount, LocationMapTree) instead");
        return resolve(node, mount, null);
    }

    public HstLink resolve(Node node, final Mount mount, final LocationMapTree tree) {
        try {
            Node canonicalNode = null;
            if(node instanceof HippoNode) {
               canonicalNode = ((HippoNode)node).getCanonicalNode();  
            }
            if(canonicalNode != null && isBinaryLocation(canonicalNode.getPath())) {
                node = canonicalNode;
            } else {
                /* This is a Hippo Resource *in* a document outside /content/assets or /content/images. This means, that
                 * you have a context aware resource: The live view is only allowed to view it when it is published for example. Therefor
                 * we do not fallback to the canonical node, but return the link in context.
                 */
            }
            
            Node resourceContainerNode = node.getParent();
            for(ResourceContainer container : resourceContainers) {
                if(resourceContainerNode.isNodeType(container.getNodeType())) {
                    String pathInfo = container.resolveToPathInfo(resourceContainerNode, node, mount);
                    if(pathInfo != null) {
                        return new HstLinkImpl(getBinariesPrefix() + pathInfo, mount, true);
                    }
                    log.debug("resourceContainer for '{}' unable to create a HstLink for path '{}'. Try next", container.getNodeType(), node.getPath());
                }
            }
            log.debug("No resource container found for '{}'. Fallback to default link for binary which is '{}'/_nodepath_", resourceContainerNode.getPrimaryNodeType().getName(), getBinariesPrefix());
            // fallback
            return defaultResourceLink(node, mount);
            
        } catch (RepositoryException e) {
            log.warn("RepositoryException during creating HstLink for resource. Return null");
            return null;
        }
    }

    
    private HstLink defaultResourceLink(Node node, Mount mount) throws RepositoryException {
        String pathInfo = getBinariesPrefix()+node.getPath();
        boolean containerResource = true;
        return new HstLinkImpl(pathInfo, mount, containerResource);
    }

    public boolean isBinaryLocation(String path) {
        if(binaryLocations == null || path == null) {
            return false;
        }
        for(String prefix : this.binaryLocations) {
            if(path.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
    
    public String getBinariesPrefix() {
        return this.binariesPrefix == null ? "" : this.binariesPrefix;
    }

    

}
