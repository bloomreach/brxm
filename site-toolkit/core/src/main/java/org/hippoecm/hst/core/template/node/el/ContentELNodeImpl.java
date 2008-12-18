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

import java.util.Collections;
import java.util.Map;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;

import org.hippoecm.hst.core.filters.base.HstRequestContext;
import org.hippoecm.hst.core.mapping.URLMapping;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContentELNodeImpl extends AbstractELNode implements ContentELNode {

    private final Logger log = LoggerFactory.getLogger(ContentELNodeImpl.class);

    private boolean rewriteContents = true;

    private HstRequestContext hstRequestContext;
    /*
     * If you want a custom source rewriter, use this constructor
     */
    
    public ContentELNodeImpl(Node node, HstRequestContext hstRequestContext) {
        super(node);
        this.hstRequestContext = hstRequestContext;
    }


    public ELNode newInstance(Node jcrNode, ELNode elNode){
        if(elNode instanceof ContentELNodeImpl) {
            return new ContentELNodeImpl(jcrNode, ((ContentELNodeImpl)elNode).hstRequestContext );
        } else {
            log.warn("Cannot create instance that is not of type ContentELNodeImpl");
            return null;
        }
    }
    
 
    @Override
    public Map getProperty() {
        if (jcrNode == null) {
            log.error("jcrNode is null. Return empty map");
            return Collections.EMPTY_MAP;
        }
        return new ELPseudoMap() {
            @Override
            public Object get(Object propertyName) {
                String prop = (String) propertyName;
                try {
                    if (!jcrNode.hasProperty(prop)) {
                        log.debug("Property '{}' not found. Return empty string", prop);
                        return null;
                    }
                    if (jcrNode.getProperty(prop).getDefinition().isMultiple()) {
                        log.warn("The property is a multivalued property. Use .... if you want the collection."
                                + " All properties will now be returned appended into a single String");
                        StringBuffer sb = new StringBuffer("");
                        for (Value val : jcrNode.getProperty(prop).getValues()) {
                            sb.append(value2Object(jcrNode, prop, val));
                            sb.append(" ");
                        }
                        return sb;
                    } else {
                        return value2Object(jcrNode, prop, jcrNode.getProperty(prop).getValue());
                    }

                } catch (PathNotFoundException e) {
                    log.warn("PathNotFoundException: {}", e.getMessage());
                } catch (RepositoryException e) {
                    log.error("RepositoryException: {}", e.getMessage());
                    log.debug("RepositoryException:", e);
                }
                return null;
            }
        };
    }

    public Object value2Object(Node node, String prop, Value val) {
        try {
            switch (val.getType()) {
            case PropertyType.BINARY:
                break;
            case PropertyType.BOOLEAN:
                return val.getBoolean();
            case PropertyType.DATE:
                // TODO : format date
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
                /*
                 * Default String values are parsed for src and href attributes because these need
                 * translation
                 */
                if (hstRequestContext.getContentRewriter() == null) {
                    log.warn("sourceRewriter is null. No linkrewriting or srcrewriting will be done");
                    return val.getString();
                } else {
                    log.debug("parsing string property for source rewriting for property: {}", prop);
                    // only rewrite sources if isRewriteSources(). Default true
                    if(isRewriteSources()) {
                        return hstRequestContext.getContentRewriter().replace(node, val.getString());
                    } else {
                        return val.getString();
                    }
                }

            case PropertyType.NAME:
                // TODO what to return
                break;
            default:
                log.error("Unable to parse type: {}", PropertyType.nameFromValue(val.getType()));
                return "";
            }
        } catch (ValueFormatException e) {
            log.error("ValueFormatException while trying to parse type '{}' : {}", PropertyType.nameFromValue(val.getType()), e.getMessage());
        } catch (RepositoryException e) {
            log.error("RepositoryException: {}", e.getMessage());
            log.debug("RepositoryException:", e);
        }
        return "";
    }

    public Map getResourceUrl() {
        if (jcrNode == null) {
            log.warn("jcrNode is null. Return empty map");
            return Collections.EMPTY_MAP;
        }
        return new ELPseudoMap() {

            @Override
            public Object get(Object resource) {
                String resourceName = (String) resource;
                try {
                    if (jcrNode.hasNode(resourceName)) {
                        Node resourceNode = jcrNode.getNode(resourceName);
                        if (resourceNode.isNodeType(HippoNodeType.NT_RESOURCE)) {
                            // first find the canonical resource
                            if (!(resourceNode instanceof HippoNode)) {
                                throw new RepositoryException("Resource node is not of type HippoNode");
                            }
                            HippoNode hippoNode = (HippoNode) resourceNode;
                            int levelsUp = 0;
                            String[] origPaths = hippoNode.getPath().split("/");
                            String postfix = "";
                            while (hippoNode.getCanonicalNode() == null) {
                                // TODO this might go wrong for same name siblings
                                hippoNode = (HippoNode) hippoNode.getParent();
                                levelsUp++;
                                if (postfix.length() > 0) {
                                    postfix = "/" + postfix;
                                }
                                postfix = origPaths[origPaths.length - levelsUp] + postfix;
                            }
                            hippoNode = (HippoNode)hippoNode.getCanonicalNode();
                            Node canonical;
                            if (levelsUp > 0) {
                                canonical = hippoNode.getNode(postfix);
                            } else {
                                canonical = hippoNode.getCanonicalNode();
                            }
                            return hstRequestContext.getUrlMapping().rewriteLocation(canonical, hstRequestContext);
                        }
                    } else {
                        log.warn(resourceName + "not of type hippo:resource. Returning null");
                        return null;
                    }
                } catch (RepositoryException e) {
                    log.error("RepositoryException while looking for resource '{}': {}", resourceName, e.getMessage());
                    log.debug("RepositoryException:", e);
                }
                return null;
            }
        };
    }

    public Map getHasResourceUrl() {
        if (jcrNode == null) {
            log.warn("jcrNode is null. Return empty map");
            return Collections.EMPTY_MAP;
        }
        return new ELPseudoMap() {

            @Override
            public Object get(Object resource) {
                String resourceName = (String) resource;
                try {
                    return jcrNode.hasNode(resourceName);
                } catch (RepositoryException e) {
                    log.error("RepositoryException: {}", e.getMessage());
                    log.debug("RepositoryException:", e);
                    return false;
                }

            }
        };
    }
    
    public Map getDeref() {
        return new ELPseudoMap() {
            @Override
            public Object get(Object property) {
                String propertyName = (String) property;
                try {
                    if(jcrNode.hasProperty(propertyName) && jcrNode.getProperty(propertyName).getType()==PropertyType.STRING ) {
                        String uuid = jcrNode.getProperty(propertyName).getString();
                        try {
                            Node deref = jcrNode.getSession().getNodeByUUID(uuid);
                            return newInstance(deref, ContentELNodeImpl.this);
                        } catch (ItemNotFoundException e) {
                            log.warn("Node with uuid '"+uuid+"' cannot be found. Cannot deref property");
                            return null;
                        }  
                    } else {
                       log.warn("jcr node '" + jcrNode.getPath() + "' does not have property '"+propertyName+"'");
                       return null;
                    }
                } catch (RepositoryException e) {
                    log.warn("RepositoryException: {}", e.getMessage());
                    log.debug("RepositoryException:", e);
                    return null;
                }

            }
        };
    }
    
    // use getDeref instead as that is a general purpose method
    @Deprecated 
    public ELNode getFacetlink(){
       try {
       	 if(jcrNode != null && jcrNode.hasProperty("hippo:docbase")){
       	  Node facetedNode = jcrNode.getSession().getNodeByUUID(jcrNode.getProperty("hippo:docbase").getValue().getString());
       	  String facetedNodeName = facetedNode.getName();
       	  log.debug("facetedNodeName: " + facetedNodeName);
       	  	if(facetedNodeName!=null && !facetedNodeName.equals("") && facetedNode.hasNode(facetedNodeName)){           			  
       	  		Node childFacetNode = facetedNode.getNode(facetedNode.getName());
       	  		return newInstance(childFacetNode,this);
       	  	}
       	  	else {
       	  		return null;
       	  	}
          }
          else{
             return null;
          }           			  
        } catch (PathNotFoundException e) {
          log.debug("PathNotFoundException: {}", e.getMessage());
        } catch (RepositoryException e) {
          log.error("RepositoryException: {}", e.getMessage());
          log.debug("RepositoryException:", e);
        }
        return null;
    }
    
    @Override
    public String getRelpath() {
        if(hstRequestContext.getContentRewriter() == null || hstRequestContext.getUrlMapping() == null) {
            log.warn("cannot get relpath because no urlMapping");
            return null;
        }
        if(jcrNode == null) {
        	log.warn("cannot get relpath because wrapped jcrNode is null");
            return null;
        }
        URLMapping urlMapping = hstRequestContext.getUrlMapping();
        
        String repositoryPath   = urlMapping.getRepositoryMapping().getContentPath();
        
        if(repositoryPath == null || "".equals(repositoryPath)) {
            log.warn("no prefix path. Cannot get relpath");
            return null;
        }
        try {
            String path = jcrNode.getPath();
            if(path.startsWith(repositoryPath)) {
                String relPath = path.substring(repositoryPath.length());
                if(relPath.startsWith("/")) {
                    relPath = relPath.substring(1);
                }
                return relPath;
            }
            else {
                log.warn("cannot get relpath because jcr path does not start with context prefix path");
            }
            return null;
        } catch (RepositoryException e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    public boolean isRewriteSources() {
        return rewriteContents;
    }

}
