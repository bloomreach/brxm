/*
 *  Copyright 2010 Hippo.
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

import org.hippoecm.hst.configuration.HstSite;
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
    
    public void setLocationMapTree(LocationMapTree locationMapTree) {
       // we do not need a locationMapTree one for binary data 
    }
    
    public void setBinariesPrefix(String binariesPrefix){
        this.binariesPrefix = PathUtils.normalizePath(binariesPrefix);
    }
    
    public void setBinaryLocations(String[] binaryLocations) {
        this.binaryLocations = binaryLocations;
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
    
    public HstLink resolve(Node node, HstSite hstSite) {
        
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
                    String pathInfo = container.resolveToPathInfo(resourceContainerNode, node, hstSite);
                    if(pathInfo != null) {
                        return new HstLinkImpl(getBinariesPrefix() + pathInfo, hstSite, true);
                    }
                    log.debug("resourceContainer for '{}' unable to create a HstLink for path '{}'. Try next", container.getNodeType(), node.getPath());
                }
            }
            log.debug("No resource container found for '{}'. Fallback to default link for binary which is '{}'/_nodepath_", resourceContainerNode.getPrimaryNodeType().getName(), getBinariesPrefix());
            // fallback
            return defaultResourceLink(node, hstSite);
            
        } catch (RepositoryException e) {
            log.warn("RepositoryException during creating HstLink for resource. Return null");
            return null;
        }
    }

    
    private HstLink defaultResourceLink(Node node, HstSite hstSite) throws RepositoryException {
        String pathInfo = getBinariesPrefix()+node.getPath();
        boolean containerResource = true;
        return new HstLinkImpl(pathInfo, hstSite, containerResource);
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
