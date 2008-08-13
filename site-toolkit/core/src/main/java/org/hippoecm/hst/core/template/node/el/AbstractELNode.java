package org.hippoecm.hst.core.template.node.el;

import java.util.Collections;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;

import org.hippoecm.hst.core.template.ContextBase;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.ISO9075Helper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractELNode implements ELNode{
    
      private Logger log = LoggerFactory.getLogger(AbstractELNode.class);
      protected Node jcrNode; 
      
      public AbstractELNode(Node node) {
          this.jcrNode = node;
      }
    
      public AbstractELNode(ContextBase contextBase, String relativePath) throws RepositoryException  {
          this.jcrNode = contextBase.getRelativeNode(relativePath);
    }

    public Node getJcrNode() {
          return jcrNode;
      }
      
    public Map getProperty() {
        if(jcrNode == null) {
            log.error("jcrNode is null. Return empty map");
            return Collections.EMPTY_MAP;
        }
        return new ELPseudoMap() {
            public Object get(Object propertyName) {
                try { 
                    return jcrNode.getProperty((String) propertyName).getValue().getString();
                } catch (ValueFormatException e) {                  
                    e.printStackTrace();
                } catch (IllegalStateException e) {                 
                    e.printStackTrace();
                } catch (PathNotFoundException e) {             
                    e.printStackTrace();
                } catch (RepositoryException e) {               
                    e.printStackTrace();
                }
                return "";
            }
        };
     }
    
    public Map getResourceUrl() {
        if(jcrNode == null) {
            log.error("jcrNode is null. Return empty map");
            return Collections.EMPTY_MAP;
        }
        return new ELPseudoMap() {
            private String DEFAULT_RESOURCE = "";
            @Override
            public Object get(Object resource) {
                String resourceName = (String)resource;
                try {
                    if(jcrNode.hasNode(resourceName)){
                        Node resourceNode = jcrNode.getNode(resourceName);
                        if(resourceNode.isNodeType(HippoNodeType.NT_RESOURCE)){
                           if(resourceNode instanceof HippoNode && ((HippoNode)resourceNode).getCanonicalNode() != null) {
                               Node canonical = ((HippoNode)resourceNode).getCanonicalNode();
                               log.info("resource location = " + "/binaries"+canonical.getPath());
                               return "/binaries"+canonical.getPath();
                           } else {
                               log.info("resource location = " + "/binaries"+resourceNode.getPath());
                               return "/binaries"+resourceNode.getPath();
                           }
                        } else {
                            log.error(resourceName + "not of type hippo:resource. Returning default value");
                            return DEFAULT_RESOURCE;
                        }
                    }
                } catch (RepositoryException e) {
                    log.error("RepositoryException while looking for resource " + resourceName + "  :" + e.getMessage());
                }
                return DEFAULT_RESOURCE;
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
              return "";
          }
      }
}
