/*
 * Copyright 2010-2022 Bloomreach
 */
package org.hippoecm.hst.core.linking;

import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Session;

import org.hippoecm.hst.configuration.hosting.Mount;

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
     * This is the reverse of {@link #resolveToPathInfo(Node, Node, Mount)}. If this ResourceContainer can resolve
     * the pathInfo to a resource node of type {@link #getNodeType()}, it returns the resourceNode. If it cannot resolve the
     * pathInfo, <code>null</null> is returned
     * @param session
     * @param pathInfo : the path from the url after the context and servlet path. It starts with a slash
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
     * returns the mapping from nodename to url prefix. For example, hippogallery:thumbnail &lt;--&gt; thumbnail
     * @return the mapping from nodename to url prefix
     */
    Map<String, String> getMappings();
}
