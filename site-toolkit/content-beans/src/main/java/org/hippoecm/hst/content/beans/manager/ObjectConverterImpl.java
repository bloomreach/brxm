/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.hst.content.beans.manager;

import java.util.Map;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.uuid.UUID;
import org.hippoecm.hst.content.beans.NodeAware;
import org.hippoecm.hst.content.beans.ObjectBeanManagerException;
import org.hippoecm.hst.service.ServiceFactory;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.LoggerFactory;

public class ObjectConverterImpl implements ObjectConverter {
    
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(ObjectConverterImpl.class);
    
    protected Map<String, Class> jcrPrimaryNodeTypeClassPairs;
    protected String [] fallBackJcrNodeTypes;
    
    public ObjectConverterImpl(Map<String, Class> jcrPrimaryNodeTypeClassPairs, String [] fallBackJcrNodeTypes) {
        this.jcrPrimaryNodeTypeClassPairs = jcrPrimaryNodeTypeClassPairs;
        this.fallBackJcrNodeTypes = fallBackJcrNodeTypes;
    }

    public Object getObject(Session session, String path) throws ObjectBeanManagerException {
        Object object = null;
        
        try {
            if(path == null || !path.startsWith("/")) {
                log.warn("Illegal argument for '{}' : not an absolute path", path);
                return null;
            }
            if (!session.itemExists(path)) {
                return null;
            }
            
            Node node = (Node) session.getItem(path);

            if (node.isNodeType(HippoNodeType.NT_HANDLE)) {
                // if its a handle, we want the child node. If the child node is not present,
                // this node can be ignored
                if(node.hasNode(node.getName())) {
                    return getObject(node.getNode(node.getName()));
                } else {
                    return null;
                }
            } else {
                object = getObject(node);
                
            }
        } catch (PathNotFoundException pnfe) {
            // HINT should never get here
            throw new ObjectBeanManagerException("Impossible to get the object at " + path, pnfe);
        } catch (RepositoryException re) {
            throw new ObjectBeanManagerException("Impossible to get the object at " + path, re);
        }
        
        return object;
    }

    public Object getObject(Node node, String relPath) throws ObjectBeanManagerException {
        if(relPath == null || relPath.startsWith("/")) {
            log.warn("'{}' is not a valid relative path. Return null.", relPath);
            return null;
        }
        if(node == null) {
            log.warn("Node is null. Cannot get document with relative path '{}'", relPath);
            return null;
        }
        Session session = null;
        try {
            session = node.getSession();
            String absPath = node.getPath() + "/" + relPath;
            return this.getObject(session, absPath);
        } catch (RepositoryException e) {
            log.warn("Node's session is available. Cannot get document with relative path '{}'", relPath);
            return null;
        }
    }
    
    public Object getObject(String uuid, Session session) throws ObjectBeanManagerException {
        checkUUID(uuid);
        try {
            Node node = session.getNodeByUUID(uuid);
            return this.getObject(node);
        } catch (ItemNotFoundException e) {
            log.warn("ItemNotFoundException for uuid '{}'. Return null.", uuid);
        } catch (RepositoryException e) {
            log.error("RepositoryException for uuid '{}' : {}. Return null.",uuid, e);
        }
        return null;
    }

    public Object getObject(String uuid, Node node) throws ObjectBeanManagerException {
        try {
            return this.getObject(uuid, node.getSession());
        } catch (RepositoryException e) {
            log.error("RepositoryException {}. Return null.", e);
        }
        return null;
    }
    
    public Object getObject(Node node) throws ObjectBeanManagerException {
        Object object = null;
        String jcrPrimaryNodeType;
        String path;
        try {
            jcrPrimaryNodeType = node.getPrimaryNodeType().getName();
            Class proxyInterfacesOrDelegateeClass = this.jcrPrimaryNodeTypeClassPairs.get(jcrPrimaryNodeType);
          
            if (proxyInterfacesOrDelegateeClass == null) {
                // no exact match, try a fallback type
                for (String fallBackJcrPrimaryNodeType : this.fallBackJcrNodeTypes) {
                    
                    if(!node.isNodeType(fallBackJcrPrimaryNodeType)) {
                        continue;
                    }
                    // take the first fallback type
                    proxyInterfacesOrDelegateeClass = this.jcrPrimaryNodeTypeClassPairs.get(fallBackJcrPrimaryNodeType);
                    if(proxyInterfacesOrDelegateeClass != null) {
                    	log.debug("No bean found for {}, using fallback class  {} instead", jcrPrimaryNodeType, proxyInterfacesOrDelegateeClass);
                        break;
                    }
                }
            }
            
            if (proxyInterfacesOrDelegateeClass != null) {
                object = ServiceFactory.create(node, proxyInterfacesOrDelegateeClass);
                if (object instanceof NodeAware) {
                    ((NodeAware) object).setNode(node);
                }
                if (object instanceof ObjectConverterAware) {
                    ((ObjectConverterAware) object).setObjectConverter(this);
                }
                return object;
            }
            path = node.getPath();
        } catch (RepositoryException e) {
            throw new ObjectBeanManagerException("Impossible to get the object from the repository", e);
        } catch (Exception e) {
            throw new ObjectBeanManagerException("Impossible to convert the node", e);
        }
        log.warn("No Descriptor found for node type '{}'. Cannot return a Bean for '{}'.", path , jcrPrimaryNodeType);
        return null;
    }

    public Class getAnnotatedClassFor(String jcrPrimaryNodeType) {
        return this.jcrPrimaryNodeTypeClassPairs.get(jcrPrimaryNodeType);
    }
    
    private void checkUUID(String uuid) throws ObjectBeanManagerException{
        try {
            UUID uuidObj = UUID.fromString(uuid);
        } catch (IllegalArgumentException e){
            throw new ObjectBeanManagerException("Uuid is not parseable to a valid uuid: '"+uuid+"'");
        }
    }
}
