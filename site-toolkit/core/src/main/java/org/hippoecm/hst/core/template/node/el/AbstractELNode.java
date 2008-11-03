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
package org.hippoecm.hst.core.template.node.el;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;

import org.hippoecm.hst.core.template.ContextBase;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.ISO9075Helper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractELNode implements ELNode {

    private final Logger log = LoggerFactory.getLogger(AbstractELNode.class);
    protected Node jcrNode;

    public AbstractELNode(Node node) {
        this.jcrNode = node;
        if(node == null) {
            log.warn("jcrnode is null");
        }
    }

    public AbstractELNode(ContextBase contextBase, String relativePath) throws RepositoryException {
        this.jcrNode = contextBase.getRelativeNode(relativePath);
        if(jcrNode == null) {
            if(contextBase.getContextRootNode() != null) {
                log.warn("cannot get jcrNode at '" + contextBase.getContextRootNode().getPath() + "/" +relativePath);
            } else {
                log.warn("cannot get jcrNode because context root node is null");
            }
        }
    }

    public Node getJcrNode() {
        return jcrNode;
    }
    
    public ELNode getParent() {
    	try {
			return new AbstractELNode(this.jcrNode.getParent()){
			};
        } catch (ItemNotFoundException e) {
            log.error("ItemNotFoundException while getting parent node: " + e.getMessage() +". Return null");           
        } catch (AccessDeniedException e) {
            log.error("AccessDeniedException while getting parent node: " + e.getMessage() +". Return null");        
        } catch (RepositoryException e) {
            log.error("RepositoryException while getting parent node: " + e.getMessage() +". Return null");        
        }
		return null;
    }
    
    public String getNodetype(){
        try {
            return this.jcrNode.getPrimaryNodeType().getName();
        } catch (RepositoryException e) {
            log.error("RepositoryException " + e.getMessage());
        }
        return null;
    }
    public String getUuid(){
    	try {
      	  if(jcrNode.hasProperty(HippoNodeType.HIPPO_UUID)){
    		  return jcrNode.getProperty(HippoNodeType.HIPPO_UUID).getValue().getString();
    	  }else {
        	  if(jcrNode.isNodeType("mix:referenceable")){
        		  return jcrNode.getUUID();
              }
        	  return null;
          }			
		} catch (RepositoryException e) {
			log.error("RepositoryException " + e.getMessage());
		}
		return null;
    }

    public Map getHasNode() {
        if (jcrNode == null) {
            log.error("jcrNode is null. Return empty map");
            return Collections.EMPTY_MAP;
        }
        return new ELPseudoMap() {
            @Override
            public Object get(Object nodeName) {
                try {
                    return jcrNode.hasNode((String) nodeName);
                } catch (RepositoryException e) {
                    log.error("RepositoryException " + e.getMessage());
                    return false;
                }

            }
        };
    }

    
    
    public Map getNode(){
        if (jcrNode == null) {
            log.error("jcrNode is null. Return empty map");
            return Collections.EMPTY_MAP;
        }
        return new ELPseudoMap() {
            @Override
            public Object get(Object nodeName) {
                String name = (String) nodeName;
                 try {
                     if (!jcrNode.hasNode(name)) {
                         log.debug("Node '{}' not found. Return empty string", name);
                         return null;
                     } else{
                         return  new AbstractELNode(jcrNode.getNode(name)){
                         };
                     }
                 } catch (PathNotFoundException e) {
                     log.debug("PathNotFoundException: {}", e.getMessage());
                 } catch (RepositoryException e) {
                     log.error("RepositoryException: {}", e.getMessage());
                     log.debug("RepositoryException:", e);
                 }
                 return null;
            }
        };
    }
    
    public Map getNodes(){
        if (jcrNode == null) {
            log.error("jcrNode is null. Return empty map");
            return Collections.EMPTY_MAP;
        }
        return new ELPseudoMap() {
            @Override
            public Object get(Object nodeName) {
                String name = (String) nodeName;
                 try {
                     List<ELNode> wrappedNodes = new ArrayList<ELNode>();
                     for(NodeIterator it = jcrNode.getNodes(name); it.hasNext();) {
                         Node n = it.nextNode();
                         if(n!=null) {
                             wrappedNodes.add(new AbstractELNode(n){});
                         }
                     }
                     return wrappedNodes;
                 } catch (RepositoryException e) {
                     log.error("RepositoryException: {}", e.getMessage());
                     log.debug("RepositoryException:", e);
                 }
                 return null;
            }
            
            @Override 
            public Set<ELNode> entrySet() {
                Set<ELNode> s = new HashSet<ELNode>();
                try {
                    for(NodeIterator it = jcrNode.getNodes(); it.hasNext();) {
                        Node n = it.nextNode();
                        if(n!=null) {
                            s.add(new AbstractELNode(n){});
                        }
                    }
                } catch (RepositoryException e) {
                    log.error("RepositoryException: {}", e.getMessage());
                    log.debug("RepositoryException:", e);
                }
                return  s;
            }
        };
    }
    
    public Map getNodesoftype(){
        if (jcrNode == null) {
            log.error("jcrNode is null. Return empty map");
            return Collections.EMPTY_MAP;
        }
        return new ELPseudoMap() {
            @Override
            public Object get(Object nodeName) {
                String nodetype = (String) nodeName;
                 try {
                     List<ELNode> wrappedNodes = new ArrayList<ELNode>();
                     for(NodeIterator it = jcrNode.getNodes(); it.hasNext();) {
                         Node n = it.nextNode();
                         if(n!=null && n.isNodeType(nodetype)) {
                             wrappedNodes.add(new AbstractELNode(n){});
                         }
                     }
                     return wrappedNodes;
                 } catch (RepositoryException e) {
                     log.error("RepositoryException: {}", e.getMessage());
                     log.debug("RepositoryException:", e);
                 }
                 return null;
            }
        };
    }
    

    
    public Map getHasProperty() {
        if (jcrNode == null) {
            log.error("jcrNode is null. Return empty map");
            return Collections.EMPTY_MAP;
        }
        return new ELPseudoMap() {
            @Override
            public Object get(Object propertyName) {
                try {
                    return jcrNode.hasProperty((String) propertyName);
                } catch (RepositoryException e) {
                    log.error("RepositoryException " + e.getMessage());
                    return false;
                }

            }
        };
    }

    public Map getProperty() {
        if (jcrNode == null) {
            log.error("jcrNode is null. Return empty map");
            return Collections.EMPTY_MAP;
        }
        return new ELPseudoMap() {
            @Override
            public Object get(Object propertyName) {
                try {
                    Value val = jcrNode.getProperty((String) propertyName).getValue();
                    switch (val.getType()) {
                    case PropertyType.BINARY:
                        break;
                    case PropertyType.BOOLEAN:
                        return val.getBoolean();
                    case PropertyType.DATE:
                        return val.getDate();
                    case PropertyType.DOUBLE:
                        return val.getDouble();
                    case PropertyType.LONG:
                        return val.getLong();
                    case PropertyType.REFERENCE:
                        // TODO return path of referenced node?
                        break;
                    case PropertyType.PATH:
                        // TODO return what?
                        break;
                    case PropertyType.STRING:
                        return val.getString();
                    case PropertyType.NAME:
                        // TODO what to return
                        break;
                    default:
                        log.error("Illegal type for Value");
                        return "";
                    }
                } catch (ValueFormatException e) {
                    log.debug("Property is multivalued: not applicable for AbstractELNode. Return null");
                } catch (PathNotFoundException e) {
                    log.debug("Property '{}' not found. Return null.", propertyName);
                } catch (RepositoryException e) {
                    log.warn("RepositoryException while getting property: '{}", e.getMessage());
                    log.debug("RepositoryException:", e);
                }
                return null;
            }
        };
    }

    public String getDecodedName() {
        try {
            return ISO9075Helper.decodeLocalName(jcrNode.getName());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return "";
        }
    }

    public String getName() {
        try {
            return jcrNode.getName();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return "";
        }
    }

    public String getPath() {
        try {
            return jcrNode.getPath();
        } catch (RepositoryException e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    public String getRelpath() {
        log.warn("relpath not supported for AbstractELNode");
        try {
            return jcrNode.getPath();
        } catch (RepositoryException e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }
}
