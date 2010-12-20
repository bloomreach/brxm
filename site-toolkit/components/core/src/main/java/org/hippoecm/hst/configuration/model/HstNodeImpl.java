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
package org.hippoecm.hst.configuration.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.provider.ValueProvider;
import org.hippoecm.hst.provider.jcr.JCRValueProvider;
import org.hippoecm.hst.provider.jcr.JCRValueProviderImpl;
import org.slf4j.LoggerFactory;

/**
 * A HstRepositoryNode is a node that during initialization fetches everything it needs, after which, it detaches its backing
 * content provider. A HstRepositoryNode is suitable for (event) caching.
 */

public class HstNodeImpl implements HstNode {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(HstNodeImpl.class);

    private HstNode parent;
    
    /**
     * the provider containing the node data
     */
    private JCRValueProvider provider;
    
    /**
     * We use a LinkedHashMap because insertion order does matter. This map can be used and modified concurrently
     */
    private Map<String, HstNode> children = Collections.synchronizedMap(new LinkedHashMap<String, HstNode>()); 
    
    /**
     * The primary node type name
     */
    private String nodeTypeName;
    
    public HstNodeImpl(Node jcrNode, HstNode parent, boolean loadChilds) throws HstNodeException {
        this.parent = parent;
        try {
            provider = new JCRValueProviderImpl(jcrNode, false);
            nodeTypeName = jcrNode.getPrimaryNodeType().getName();
            if(loadChilds) {
                loadChilds(jcrNode, parent);
            }
        } catch (RepositoryException e) {
           throw new HstNodeException(e);
        }
        // detach the backing jcr node now we are done.                     
        provider.detach();
    }

    protected void loadChilds(Node jcrNode, HstNode parent) throws RepositoryException {
        NodeIterator nodes = jcrNode.getNodes();
        while (nodes.hasNext()) {
            Node child = nodes.nextNode();
            if (child == null) {
                throw new HstNodeException("Configuration changed while loading. Reload");
            }
            HstNode childRepositoryNode = null;
            try {
                childRepositoryNode = createNew(child, this, true);
            } catch (HstNodeException e) {
                log.warn("Failed to load configuration node for '{}'. {}", child.getPath(), e.toString());
            }
            if (childRepositoryNode != null) {
                HstNodeImpl existing = (HstNodeImpl) children.get(childRepositoryNode.getValueProvider().getName());
                if (existing != null) {
                    log.warn("Ignoring node configuration at '{}' for '{}' because it is duplicate. This is not allowed",
                                provider.getPath(), childRepositoryNode.getValueProvider().getPath());
                } else {
                    // does not exist yet
                    children.put(childRepositoryNode.getValueProvider().getName(), childRepositoryNode);
                }
            }
        }
    }
    
    protected HstNode createNew(Node jcrNode,  HstNode parent, boolean loadChilds)  throws HstNodeException {
        return new HstNodeImpl(jcrNode, parent, loadChilds);
    }
    
    /* (non-Javadoc)
     * @see org.hippoecm.hst.configuration.model.HstNode#getValueProvider()
     */
    public ValueProvider getValueProvider(){
        return this.provider;
    }
    

    public Map<String, HstNode> getChildren() {
        return children;
    }
    
    /* (non-Javadoc)
     * @see org.hippoecm.hst.configuration.model.HstNode#getNode(java.lang.String)
     */
    public HstNode getNode(String relPath) throws IllegalArgumentException{
        if(relPath == null || "".equals(relPath) || relPath.startsWith("/")) {
            throw new IllegalArgumentException("Not a valid relPath '"+relPath+"'");
        }
        relPath = StringUtils.stripEnd(relPath, "/");
        String[] args = relPath.split("/");
        HstNode child = children.get(args[0]);
        if(args.length > 1 && child != null) {
            relPath = relPath.substring(relPath.indexOf("/"));
            return child.getNode(relPath);
        }
        
        if(child == null) {
            log.debug("Node at relPath '{}' cannot be found for ConfigNode '{}'", relPath, provider.getPath());
        }
        return child;
    }
    
    public void setNode(String name, HstNode hstNode)  {
        children.put(name, hstNode);
    }

    /* (non-Javadoc)
     * @see org.hippoecm.hst.configuration.model.HstNode#getNodes()
     */
    public List<HstNode> getNodes()  {
        return new ArrayList<HstNode>(children.values());
    }
  
    /* (non-Javadoc)
     * @see org.hippoecm.hst.configuration.model.HstNode#getNodes(java.lang.String)
     */
    public List<HstNode> getNodes(String configNodeTypeName)  {
        if(configNodeTypeName == null) {
            throw new IllegalArgumentException("configNodeTypeName is not allowed to be null");
        }
        List<HstNode> childrenOfType = new ArrayList<HstNode>();
        for(HstNode child : children.values()) {
            if(configNodeTypeName.equals(child.getNodeTypeName())){
                childrenOfType.add(child);
            }
        }
        return childrenOfType;
    }

    /* (non-Javadoc)
     * @see org.hippoecm.hst.configuration.model.HstNode#getNodeTypeName()
     */
    public String getNodeTypeName() {
        return nodeTypeName;
    }

    /* (non-Javadoc)
     * @see org.hippoecm.hst.configuration.model.HstNode#getParent()
     */
    public HstNode getParent()  {
        return parent;
    }
 

}
