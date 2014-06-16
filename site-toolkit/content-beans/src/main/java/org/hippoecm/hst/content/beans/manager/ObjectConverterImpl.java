/*
 *  Copyright 2008-2014 Hippo B.V. (http://www.onehippo.com)
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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.content.beans.NodeAware;
import org.hippoecm.hst.content.beans.ObjectBeanManagerException;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.service.ServiceFactory;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.util.JcrUtils;
import org.slf4j.LoggerFactory;

public class ObjectConverterImpl implements ObjectConverter {
    
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(ObjectConverterImpl.class);

    protected Map<String, Class<? extends HippoBean>> jcrPrimaryNodeTypeBeanPairs;
    protected Map<Class<? extends HippoBean>, String> jcrBeanPrimaryNodeTypePairs;
    protected String [] fallBackJcrNodeTypes;
    
    public ObjectConverterImpl(Map<String, Class<? extends HippoBean>> jcrPrimaryNodeTypeBeanPairs, String [] fallBackJcrNodeTypes) {
        this.jcrPrimaryNodeTypeBeanPairs = jcrPrimaryNodeTypeBeanPairs;
        this.jcrBeanPrimaryNodeTypePairs = new HashMap<Class<? extends HippoBean>, String>();
        
        for(Entry<String, Class<? extends HippoBean>> entry: jcrPrimaryNodeTypeBeanPairs.entrySet()) {
            jcrBeanPrimaryNodeTypePairs.put(entry.getValue(), entry.getKey());
        }
        
        if (fallBackJcrNodeTypes != null) {
            this.fallBackJcrNodeTypes = new String[fallBackJcrNodeTypes.length];
            System.arraycopy(fallBackJcrNodeTypes, 0, this.fallBackJcrNodeTypes, 0, fallBackJcrNodeTypes.length);
        }
    }

    public Object getObject(Session session, String path) throws ObjectBeanManagerException {
        if(StringUtils.isEmpty(path) || !path.startsWith("/")) {
            log.warn("Illegal argument for '{}' : not an absolute path", path);
            return null;
        }
        String relPath = path.substring(1);
        try {
            return getObject(session.getRootNode(), relPath);
        } catch (RepositoryException re) {
            throw new ObjectBeanManagerException("Impossible to get the object at " + path, re);
        }
           
    }

    public Object getObject(Node node, String relPath) throws ObjectBeanManagerException {
        if(StringUtils.isEmpty(relPath) || relPath.startsWith("/")) {
            log.info("'{}' is not a valid relative path. Return null.", relPath);
            return null;
        }
        if(node == null) {
            log.info("Node is null. Cannot get document with relative path '{}'", relPath);
            return null;
        }
        String nodePath = null;
        try {
            nodePath  = node.getPath();
            final Node relNode = JcrUtils.getNodeIfExists(node, relPath);
            if (relNode == null) {
                log.info("Cannot get object for node '{}' with relPath '{}'", nodePath , relPath);
                return null;
            }
            if (relNode.isNodeType(HippoNodeType.NT_HANDLE)) {
                // if its a handle, we want the child node. If the child node is not present,
                // this node can be ignored
                final Node document = JcrUtils.getNodeIfExists(relNode, relNode.getName());
                if (document == null) {
                    log.info("Cannot get object for node '{}' with relPath '{}'", nodePath, relPath);
                    return null;
                } else {
                    return getObject(document);
                }
            } else {
                return getObject(relNode);
            }   
            
        } catch (RepositoryException e) {
            if (log.isDebugEnabled()) {
                log.warn("Cannot get object for node '"+nodePath+"' with relPath '"+relPath+"'" , e);
            } else {
                log.warn("Cannot get object for node '{}' with relPath '{}'",nodePath , relPath);
            }
            return null;
        }
    }
    
    public Object getObject(String uuid, Session session) throws ObjectBeanManagerException {
        checkUUID(uuid);
        try {
            Node node = session.getNodeByIdentifier(uuid);
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
        String jcrPrimaryNodeType;
        String path;
        try {

            if (node.isSame(node.getSession().getRootNode()) && getAnnotatedClassFor("rep:root") == null) {
                log.debug("Root node is not mapped to be resolved to a bean.");
                return null;
            }

            if(node.isNodeType(HippoNodeType.NT_HANDLE) ) {
                if(node.hasNode(node.getName())) {
                    return getObject(node.getNode(node.getName()));
                } else {
                    return null;
                }
            }
            jcrPrimaryNodeType = node.getPrimaryNodeType().getName();
            Class<? extends HippoBean> proxyInterfacesOrDelegateeClass = this.jcrPrimaryNodeTypeBeanPairs.get(jcrPrimaryNodeType);
          
            if (proxyInterfacesOrDelegateeClass == null) {
                if (jcrPrimaryNodeType.equals("hippotranslation:translations")) {
                    log.warn("Encountered node of type 'hippotranslation:translations' : This nodetype is completely deprecated and should be " +
                            "removed from all content including from prototypes.");
                    return null;
                }
                // no exact match, try a fallback type
                for (String fallBackJcrPrimaryNodeType : this.fallBackJcrNodeTypes) {
                    
                    if(!node.isNodeType(fallBackJcrPrimaryNodeType)) {
                        continue;
                    }
                    // take the first fallback type
                    proxyInterfacesOrDelegateeClass = this.jcrPrimaryNodeTypeBeanPairs.get(fallBackJcrPrimaryNodeType);
                    if(proxyInterfacesOrDelegateeClass != null) {
                    	log.debug("No bean found for {}, using fallback class  {} instead", jcrPrimaryNodeType, proxyInterfacesOrDelegateeClass);
                        break;
                    }
                }
            }
            
            if (proxyInterfacesOrDelegateeClass != null) {
                Object object = ServiceFactory.create(node, proxyInterfacesOrDelegateeClass);
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
        log.info("No Descriptor found for node '{}'. Cannot return a Bean for '{}'.", path , jcrPrimaryNodeType);
        return null;
    }

    public String getPrimaryObjectType(Node node) throws ObjectBeanManagerException {
        String jcrPrimaryNodeType;
        String path;
        try {

            if(node.isNodeType(HippoNodeType.NT_HANDLE) ) {
                if(node.hasNode(node.getName())) {
                    return getPrimaryObjectType(node.getNode(node.getName()));
                } else {
                    return null;
                }
            }
            jcrPrimaryNodeType = node.getPrimaryNodeType().getName();
            boolean isObjectType = jcrPrimaryNodeTypeBeanPairs.containsKey(jcrPrimaryNodeType);
          
            if (!isObjectType) {
                if (jcrPrimaryNodeType.equals("hippotranslation:translations")) {
                    log.info("Encountered node of type 'hippotranslation:translations' : This nodetype is completely deprecated and should be " +
                            "removed from all content including from prototypes.");
                    return null;
                }
                // no exact match, try a fallback type
                for (String fallBackJcrPrimaryNodeType : this.fallBackJcrNodeTypes) {
                    
                    if(!node.isNodeType(fallBackJcrPrimaryNodeType)) {
                        continue;
                    }
                    // take the first fallback type
                    isObjectType = jcrPrimaryNodeTypeBeanPairs.containsKey(fallBackJcrPrimaryNodeType);
                    if(isObjectType) {
                    	log.debug("No primary node type found for {}, using fallback type {} instead", jcrPrimaryNodeType, fallBackJcrPrimaryNodeType);
                    	jcrPrimaryNodeType = fallBackJcrPrimaryNodeType;
                        break;
                    }
                }
            }
            
            if (isObjectType) {
            	return jcrPrimaryNodeType;
            }
            path = node.getPath();
        } catch (RepositoryException e) {
            throw new ObjectBeanManagerException("Impossible to get the node from the repository", e);
        } catch (Exception e) {
            throw new ObjectBeanManagerException("Impossible to determine node type for node", e);
        }
        log.info("No Descriptor found for node '{}'. Cannot return a matching node type for '{}'.", path , jcrPrimaryNodeType);
        return null;
    }
    
    private void checkUUID(String uuid) throws ObjectBeanManagerException{
        try {
            UUID.fromString(uuid);
        } catch (IllegalArgumentException e){
            throw new ObjectBeanManagerException("Uuid is not parseable to a valid uuid: '"+uuid+"'");
        }
    }

    public Class<? extends HippoBean> getAnnotatedClassFor(String jcrPrimaryNodeType) {
        return this.jcrPrimaryNodeTypeBeanPairs.get(jcrPrimaryNodeType);
    }
    
    public String getPrimaryNodeTypeNameFor(Class<? extends HippoBean> hippoBean) {
        return jcrBeanPrimaryNodeTypePairs.get(hippoBean);
    }
}
