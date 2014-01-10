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

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract implementation of default (simple) resource containers (like "hippogallery:exampleAssetSet" and "hippogallery:exampleImageSet"). 
 * 
 * Method {@link #getNodeType()} is not yet implemented, but must be done by the concrete implementations. When you have
 * a resource container that has its complete custom methods for resolving from and to pathInfo of the resource, then, those classes
 * should implement their own {@link #resolveToPathInfo(Node, Node, Mount)} and {@link #resolveToResourceNode(Session, String)}
 */
public abstract class AbstractResourceContainer implements ResourceContainer {

    private static final Logger log = LoggerFactory.getLogger(AbstractResourceContainer.class);
    private Map<String, String> mappings = new HashMap<String, String>();
    private String primaryItem; 
    
    public void setMappings(Map<String, String> mappings){
        this.mappings = mappings;
    }
    
    
    public Map<String, String> getMappings(){
       return this.mappings;
    }
    
    public void setPrimaryItem(String primaryItem){
        this.primaryItem = primaryItem;
    }
    
    public String getPrimaryItem() {
        return this.primaryItem;
    }
    
    public String resolveToPathInfo(Node resourceContainerNode, Node resourceNode, Mount mount) {
        try {
            if(primaryItem == null && (this.mappings == null || this.mappings.isEmpty() )) {
                return resourceNode.getPath();
            }
            
            if(resourceNode.getName().equals(primaryItem)) {
                Node parentContainer = resourceContainerNode.getParent();
                if(!parentContainer.isNodeType(HippoNodeType.NT_HANDLE)) {
                    // parent is not a handle, so we cannot 'shorten the path': return the resourceContainerNode path
                   return resourceContainerNode.getPath();
                }
                return parentContainer.getPath();
            }
            String mapTo = mappings.get(resourceNode.getName());
            if(mapTo != null) {
                // We now map the nodepath to a pretty url:
                Node parentContainer = resourceContainerNode.getParent();
                if(!parentContainer.isNodeType(HippoNodeType.NT_HANDLE)) {
                 // parent is not a handle, so we cannot 'shorten the path': return the resourceContainerNode path
                    resourceContainerNode.getPath();
                }
                String path = parentContainer.getPath();
                if(mapTo != null) {
                    if(!"".equals(mapTo)) {
                        path = "/" + mapTo + path;
                    }
                }
                return path;
            } 
            log.info("'{}' is not mapped in mappings. Return null", resourceNode.getName());
            return null;
        } catch (RepositoryException e) {
            log.warn("RepositoryException: Return null", e);
        }
        return null;
    }
    
    public Node resolveToResourceNode(Session session, String pathInfo) {
        String actualPath = pathInfo;
        String[] elems = actualPath.substring(1).split("/");
        String mapTo = null;
        for(Entry<String, String> mapping : mappings.entrySet()) {
            if(mapping.getValue().equals(elems[0])) {
               mapTo = mapping.getKey();
               break;
            }
        }
        if(mapTo != null) {
            actualPath = actualPath.substring(1).substring(elems[0].length());
        }
        
       try {
           Item item = session.getItem(actualPath);
           if(!item.isNode()) {
               log.debug("path '{}' does not point to a node", actualPath);
               return null;
           }
           Node node = (Node)item;
           if(node.isNodeType(HippoNodeType.NT_RESOURCE)) {
               // the path directly map to a resource node: return this one
               log.debug("Resource Node found at '{}'. Return resource", actualPath);
               return node;
           }
           if(node.isNodeType(HippoNodeType.NT_HANDLE)) {
               try {
                   node = node.getNode(node.getName());
               } catch(PathNotFoundException e) {
                   log.info("Cannot return binary for a handle with no hippo document. Return null");
                   return null;
               }
           }
           
           if (node.isNodeType(getNodeType())) {
               if(mapTo != null) {
                   if(node.hasNode(mapTo)) {
                       Node resourceNode = node.getNode(mapTo);
                       if(resourceNode.isNodeType(HippoNodeType.NT_RESOURCE)) {
                           return resourceNode;
                       }
                       log.debug("Expected resource node of type '{}' but found node of type '{}'. Try to return the primary item from this resource container.", HippoNodeType.NT_RESOURCE, resourceNode.getPrimaryNodeType().getName());
                   }
               }
               if(node.hasNode(primaryItem)) {
                   Node resourceNode = node.getNode(primaryItem);
                   if(resourceNode.isNodeType(HippoNodeType.NT_RESOURCE)) {
                       return resourceNode;
                   }
                   log.debug("Expected resource node of type '{}' but found node of type '{}'. Try to return the primary jcr item (primarry item as in cnd).", HippoNodeType.NT_RESOURCE, resourceNode.getPrimaryNodeType().getName());
                  
               }
               Item primItem = node.getPrimaryItem();
               if (primItem.isNode()) {
                   Node resourceNode = (Node)primItem;
                   if(resourceNode.isNodeType(HippoNodeType.NT_RESOURCE)) {
                       return resourceNode;
                   } else {
                       log.debug("Expected resource node of type '{}' but found node of type '{}'. Return null.", HippoNodeType.NT_RESOURCE, resourceNode.getPrimaryNodeType().getName());
                   }  
               } else {
                   log.debug("Primary jcr item was a property where we expected a node of type '{}'. Return null", HippoNodeType.NT_RESOURCE);
               }
               return null;
           } else {
               log.debug("'{}' is not a resource container that is applicable for node of type '{}' at path :" + node.getPath() + ". Return null", this.getClass().getName(), node.getPrimaryNodeType().getName());
           }
       } catch (PathNotFoundException e) {
           log.debug("Cannot find resource node for path '{}' beloning to pathInfo '{}'", actualPath, pathInfo);
       } catch (RepositoryException e) {
           log.warn("RepositoryException: '{}'", e.getMessage());
       }
        
        return null;
    }

  
}
