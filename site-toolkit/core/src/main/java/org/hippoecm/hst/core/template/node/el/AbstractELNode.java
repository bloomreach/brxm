package org.hippoecm.hst.core.template.node.el;

import java.util.Collections;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;

import org.hippoecm.hst.core.template.ContextBase;
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
}
