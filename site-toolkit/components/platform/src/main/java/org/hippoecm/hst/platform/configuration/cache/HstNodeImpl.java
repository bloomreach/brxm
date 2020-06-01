/*
 *  Copyright 2010-2020 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.platform.configuration.cache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang3.StringUtils;
import org.hippoecm.hst.core.util.PropertyParser;
import org.hippoecm.hst.platform.configuration.model.ConfigurationNodesLoadingException;
import org.hippoecm.hst.configuration.model.HstNode;
import org.hippoecm.hst.core.internal.StringPool;
import org.hippoecm.hst.platform.configuration.model.ModelLoadingException;
import org.hippoecm.hst.provider.ValueProvider;
import org.hippoecm.hst.provider.jcr.JCRValueProvider;
import org.hippoecm.hst.provider.jcr.JCRValueProviderImpl;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.NodeIterable;
import org.slf4j.LoggerFactory;

import static org.hippoecm.hst.configuration.HstNodeTypes.NODENAME_HST_DOMAINS;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODENAME_HST_UPSTREAM;

/**
 * A {@link HstNodeImpl} is a node that during initialization fetches everything it needs, after which, it detaches its backing
 * content provider. A {@link HstNodeImpl} is suitable for (event) caching.
 */

public class HstNodeImpl implements HstNode {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(HstNodeImpl.class);

    private HstNode parent;
    
    /**
     * the provider containing the node data
     */
    private JCRValueProvider provider;
    
    /**
     * We use a LinkedHashMap because insertion order does matter. 
     */
    private LinkedHashMap<String, HstNode> children = null;
    
    /**
     * The primary node type name
     */
    private String nodeTypeName;
    
    /**
     * When true, this JCRValueProvider is out of date and needs to be reloaded
     */
    private boolean stale = false;
    private boolean staleChildren = false;

    public HstNodeImpl(Node jcrNode, HstNode parent) throws RepositoryException {
        this.parent = parent;
        provider = new JCRValueProviderImpl(jcrNode, false, true, false);
        nodeTypeName = StringPool.get(jcrNode.getPrimaryNodeType().getName());
        loadChildren(jcrNode);
        // detach the backing jcr node now we are done.       
        stale = false;
        provider.detach();
    }

    protected void loadChildren(Node jcrNode) throws RepositoryException {
        NodeIterator nodes = jcrNode.getNodes();
        long iteratorSizeBeforeLoop = nodes.getSize();
        while (nodes.hasNext()) {
            Node child = nodes.nextNode();
            if (skipNode(child)) {
                continue;
            }
            HstNode childRepositoryNode = new HstNodeImpl(child, this);
            if(children == null) {
                children = new LinkedHashMap<>((int)iteratorSizeBeforeLoop * 4 / 3);
            }
            HstNodeImpl existing = (HstNodeImpl) children.get(childRepositoryNode.getName());
            if (existing != null) {
                log.warn("Ignoring node configuration at '{}' for '{}' because it is duplicate. This is not allowed",
                            provider.getPath(), childRepositoryNode.getValueProvider().getPath());
            } else {
                // does not exist yet
                children.put(childRepositoryNode.getName(), childRepositoryNode);
            }

        }
        long iteratorSizeAfterLoop = nodes.getSize();
        if (iteratorSizeBeforeLoop != iteratorSizeAfterLoop) {
            throw new ConfigurationNodesLoadingException("During building the in memory HST model, the hst configuration jcr nodes have changed.");
        }
    }

    private boolean skipNode(final Node child) throws RepositoryException {
        if (parent == null) {
            // root node, skip 'hst:domains' below it since not part of the hst model
            if (child.getName().equals(NODENAME_HST_DOMAINS)) {
                log.debug("Skipping '{}' since not part of the HST in memory model but federated security domains",
                        child.getPath());
                return true;
            }
        }
        if (NODENAME_HST_UPSTREAM.equals(child.getName())) {
            log.debug("Skip hst:upstream node (and descendants)");
            return true;
        }
        return false;
    }

    @Override
    public String getName() {
        return provider.getName();
    }

    @Override
    public String getSubstitutedName() {
        final PropertyParser pp = new PropertyParser(System.getProperties());
        final Object substitutedName =  pp.resolveProperty("substitutedName", provider.getName());
        if (substitutedName == null) {
            throw new ModelLoadingException(String.format("Could not replace placeholder '%s' because no such system " +
                    "property.", provider.getName()), true);
        }
        if (substitutedName.equals(provider.getName())) {
            // just return as is, might be a node name equal to for example 127.0.0.1 (and has not been substituted)
            return String.valueOf(substitutedName);
        }

        String name = String.valueOf(substitutedName);
        if (name.contains(".")) {
            throw new ModelLoadingException(String.format("Invalid substituted name '%s' for '%s'", name, provider.getName()));
        }
        return name;
    }
    
    /* (non-Javadoc)
     * @see org.hippoecm.hst.configuration.model.HstNode#getValueProvider()
     */
    @Override
    public ValueProvider getValueProvider(){
        return this.provider;
    }
    

    public Map<String, HstNode> getChildren() {
        if(children == null) {
            return Collections.emptyMap();
        }
        return children;
    }
    
    /* (non-Javadoc)
     * @see org.hippoecm.hst.configuration.model.HstNode#getNode(java.lang.String)
     */
    @Override
    public HstNode getNode(String relPath) throws IllegalArgumentException{
        if(relPath == null || "".equals(relPath) || relPath.startsWith("/")) {
            throw new IllegalArgumentException("Not a valid relPath '"+relPath+"'");
        }
        if(children == null) {
            return null;
        }
        relPath = StringUtils.stripEnd(relPath, "/");
        String[] args = relPath.split("/");
        HstNode child = children.get(args[0]);
        int i = 1;
        while(i < args.length && child != null) {
            // instead of recursively invoking #getNode with the next rel path use
            // code below as this is more efficient as it needs no string parsing
            if (((HstNodeImpl)child).children == null) {
                child = null;
                break;
            }
            child = ((HstNodeImpl)child).children.get(args[i]);
            i++;
        }
        
        if(child == null) {
            log.debug("Node at relPath '{}' cannot be found for ConfigNode '{}'", relPath, provider.getPath());
        }
        return child;
    }

    @Override
    public void addNode(String name, HstNode hstNode)  {
        if(children == null) {
            children = new LinkedHashMap<>();
        }
        // if the child already exists, it is just replaced
        children.put(name, hstNode);
    }

    @Override
    public void removeNode(String name)  {
        if(children == null) {
            return;
        }
        HstNode removed = children.remove(name);
        if (removed == null) {
            log.debug("Could not remove child '{}' from {} because does not exist.", name, this);
        }
    }

    /* (non-Javadoc)
     * @see org.hippoecm.hst.configuration.model.HstNode#getNodes()
     */
    @Override
    public List<HstNode> getNodes()  {
        if(children == null) {
            return Collections.emptyList();
        }
        return new ArrayList<>(children.values());
    }
  
    /* (non-Javadoc)
     * @see org.hippoecm.hst.configuration.model.HstNode#getNodes(java.lang.String)
     */
    @Override
    public List<HstNode> getNodes(String configNodeTypeName)  {
        if(configNodeTypeName == null) {
            throw new IllegalArgumentException("configNodeTypeName is not allowed to be null");
        }
        if(children == null) {
            return Collections.emptyList();
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
    @Override
    public String getNodeTypeName() {
        return nodeTypeName;
    }

    /* (non-Javadoc)
     * @see org.hippoecm.hst.configuration.model.HstNode#getParent()
     */
    @Override
    public HstNode getParent()  {
        return parent;
    }

    @Override
    public void markStaleByPropertyEvent() {
        stale = true;
    }

    @Override
    public void markStaleByNodeEvent() {
        stale = true;
        staleChildren = true;
    }

    @Override
    public boolean isStale() {
        return stale;
    }

    public void update(final Session session) throws RepositoryException {
        if (!stale) {
            if (children == null) {
                return;
            }
            updateChildren(session);
            return;
        }

        log.debug("Reload provider for : "  + getValueProvider().getPath());
        Node jcrNode = JcrUtils.getNodeIfExists(getValueProvider().getPath(), session);
        if (jcrNode == null) {
            if (parent != null) {
                log.debug("Removing path '{}' from HstNode tree.", getValueProvider().getPath());
                parent.removeNode(getName());
            }
            return;
        }
        setJCRValueProvider(new JCRValueProviderImpl(jcrNode, false, true, false));
        // node type might have changed, just reset
        nodeTypeName = StringPool.get(jcrNode.getPrimaryNodeType().getName());
        if (staleChildren) {
            log.debug("Children reload for '{}'", getValueProvider().getPath());
            childrenReload(jcrNode);
        } else {
            if (children != null) {
                updateChildren(session);
            }
        }

        stale = false;
        staleChildren = false;
    }

    private void updateChildren(final Session session) throws RepositoryException {
        // because when calling child.update(session) can result in child.getParent().remove(child.getName())
        // we can not iterate directly through the children map
        final HstNode[] arr = children.values().toArray(new HstNode[children.size()]);
        for (HstNode child : arr) {
            child.update(session);
        }
    }

    private void childrenReload(final Node jcrNode) throws RepositoryException {
        LinkedHashMap<String, HstNode> newChildren = new LinkedHashMap<>();
        for (Node jcrChildNode : new NodeIterable(jcrNode.getNodes())) {
            if (skipNode(jcrChildNode)) {
                continue;
            }
            String childName = jcrChildNode.getName();
            final HstNode existing = getChild(childName);
            if (existing == null) {
                newChildren.put(childName, new HstNodeImpl(jcrChildNode, this));
            } else {
                newChildren.put(childName, existing);
                // one of its descendants might still be stale, hence continue updating the child node
                existing.update(jcrNode.getSession());
            }
        }
        if (newChildren.isEmpty()) {
            children = null;
        } else {
            children = newChildren;
        }
    }

    /**
     * private method that is an efficient variant of getNode(String relPath) as this method does never assume
     * relative path and never needs to parse the argument
     */
    private HstNode getChild(String name) {
        if(children == null) {
            return null;
        }
        return children.get(name);
    }

    @Override
    public void setJCRValueProvider(JCRValueProvider valueProvider) {
        this.provider = valueProvider;
        provider.detach();
        stale = false;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "[path=" + getValueProvider().getPath()
                + ", nodeTypeName=" + nodeTypeName +"]";
    }
    
}
