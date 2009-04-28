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
package org.hippoecm.hst.content.beans.standard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.hst.content.beans.ObjectBeanManagerException;
import org.hippoecm.hst.content.beans.manager.ObjectConverter;
import org.hippoecm.hst.provider.jcr.JCRValueProvider;
import org.hippoecm.hst.provider.jcr.JCRValueProviderImpl;
import org.hippoecm.hst.util.NOOPELMap;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HippoItem implements HippoBean{

    private static Logger log = LoggerFactory.getLogger(HippoItem.class);
    
    protected String canonicalId;
    protected transient Node node;
    protected String path;
    protected JCRValueProvider valueProvider;
    protected transient ObjectConverter objectConverter;
    protected boolean detached = false;
    

    public void setObjectConverter(ObjectConverter objectConverter) {
       this.objectConverter = objectConverter;
    }
    
    public ObjectConverter getObjectConverter(){
        return this.objectConverter;
    }

    public void setNode(Node node) {
        this.node = node;
        this.valueProvider = new JCRValueProviderImpl(node);
    }
    
    public Node getNode() {
        return node;
    }
    
    public JCRValueProvider getValueProvider(){
        return this.valueProvider;
    }
    
    public String getName() {
        return valueProvider.getName();
    }
    
    public String getPath(){
       if(this.path == null) {
           this.path = valueProvider.getPath();
       }
       return this.path;
    }
    
    public Map<String, Object> getProperties() {
        return valueProvider.getProperties();
    }
       
    public <T> T getProperty(String name) {
        return (T) getProperties().get(name);
    }
    
    public Map<String, Object> getProperty() {
        Map<String, Object> properties = null;
        if (this.valueProvider == null) {
            properties = Collections.emptyMap();
        } else {
            properties = this.valueProvider.getProperties();
        }
        return properties;
    }
    
    public <T> T getBean(String relPath) {
        try {
            Object o = this.objectConverter.getObject(node, relPath);
            if(o == null) {
                log.debug("Cannot get bean.");
                return null;
            }
            return (T)o;
         }
        catch (ObjectBeanManagerException e) {
            log.warn("Cannot get Object at relPath '{}' for '{}'", relPath, this.getPath());
        } 
        return null;
    }
    
    public <T> List<T> getChildBeansByName(String childNodeName) {
       List<T> childBeans = new ArrayList<T>();
       NodeIterator nodes;
       try {
           nodes = node.getNodes();
           while(nodes.hasNext()) {
               Node child = nodes.nextNode();
               if(child == null) {continue;}
               if(child.getName().equals(childNodeName)) {
                   try {
                       Object bean = this.objectConverter.getObject(child);
                       childBeans.add((T)bean);
                   } catch (ObjectBeanManagerException e) {
                      log.warn("Skipping bean: {}", e);
                   }
               }
           }
       } catch (RepositoryException e) {
           log.error("RepositoryException: Cannot get ChildBeans for jcrPrimaryNodeType: {}", e);
           return new ArrayList<T>();
       }
       return childBeans;
    }
    
    public <T> List<T> getChildBeans(String jcrPrimaryNodeType) {
         Class annotatedClass = this.objectConverter.getAnnotatedClassFor(jcrPrimaryNodeType);
         if(annotatedClass == null) {
             log.warn("Cannot get ChildBeans for jcrPrimaryNodeType '{}' because there is no annotated class for this node type. Return null");
             return new ArrayList<T>();
         }
         if(this.node == null) {
             log.warn("Cannot get ChildBeans for jcrPrimaryNodeType '{}' because the jcr node is detached. ");
             return new ArrayList<T>();
         }
         
         List<T> childBeans = new ArrayList<T>();
         NodeIterator nodes;
        try {
            nodes = node.getNodes();
            while(nodes.hasNext()) {
                Node child = nodes.nextNode();
                if(child == null) {continue;}
                if(child.getPrimaryNodeType().getName().equals(jcrPrimaryNodeType)) {
                    try {
                        Object bean = this.objectConverter.getObject(child);
                        if(bean != null && annotatedClass.isAssignableFrom(bean.getClass())) {
                            childBeans.add((T)bean);
                        }
                    } catch (ObjectBeanManagerException e) {
                       log.warn("Skipping bean: {}", e);
                    }
                }
            }
        } catch (RepositoryException e) {
            log.error("RepositoryException: Cannot get ChildBeans for jcrPrimaryNodeType: {}", e);
            return new ArrayList<T>();
        }
        
        return childBeans;
    }
    
    public HippoBean getParentBean(){
        try {
            Node parentNode = valueProvider.getParentJcrNode();
            if(parentNode == null) {
                log.warn("Cannot return parent bean for detached bean. Return null");
                return null;
            } 
            if(parentNode.isNodeType(HippoNodeType.NT_HANDLE)) {
                parentNode = parentNode.getParent();
            }
            Object o = this.objectConverter.getObject(parentNode);
            if(o instanceof HippoBean) {
                return (HippoBean)o;
            } else {
                log.warn("Bean is not an instance of HippoBean. Return null : ", o);
            }
        } catch (ObjectBeanManagerException e) {
            log.warn("Failed to get parent object for '{}'", this.getPath());
        }  catch (RepositoryException e) {
            log.warn("Failed to get parent object for '{}'", this.getPath());
        } 
        return null;
    }
    
    public boolean equalCompare(Object compare){
        return (Boolean)new ComparatorMap().get(compare);
    }
    
    public Map<Object,Object> getEqualComparator() {
        return new ComparatorMap();
    }
    
    public class ComparatorMap extends NOOPELMap {
        public Object get(Object compare) {
            if(! (compare instanceof HippoItem)) {
                return false;
            }
            
            HippoItem compareNode = (HippoItem)compare;  
            if(compareNode.canonicalId == null) {
               HippoNode node = (HippoNode)compareNode.getNode();
               if(node == null) {
                   return false;
               }
               compareNode.canonicalId = fetchComparatorId(node);
               if(compareNode.canonicalId == null) {
                   return false;
               }
            }
            if(HippoItem.this.canonicalId == null) {
                HippoNode node = (HippoNode)HippoItem.this.getNode();
                if(node == null) {
                    return false;
                }
                HippoItem.this.canonicalId = fetchComparatorId(node);
                if(HippoItem.this.canonicalId == null) {
                    return false;
                }
            }
            if(compareNode.canonicalId != null && HippoItem.this.canonicalId != null ) {
                return compareNode.canonicalId.equals(HippoItem.this.canonicalId);
            }
            return false;
        }

        private String fetchComparatorId(HippoNode node) {
            if(node == null) {
                return null;
            }
            try {
                if (node.hasProperty(HippoNodeType.HIPPO_UUID)) {
                   return  node.getProperty(HippoNodeType.HIPPO_UUID).getString();
                } else if (node.isNodeType("mix:referenceable")) {
                   return node.getUUID();
                }
            } catch (RepositoryException e) {
                log.warn("RepositoryException while comparing HippoStdNodes. Return false");
            }
            return null;
        }       
    }
    
    /**
     *  The standard HippoItem has a natural ordering based on node name.
     *  if you need ordering on a different basis, override this method
     */
    
    public int compareTo(HippoBean hippoBean) {
        // if hippoFolder == null, a NPE will be thrown which is fine because the arg is not allowed to be null.
        if(this.getName() == null) {
            // should not be possible
            return -1;
        }
        if(hippoBean.getName() == null) {
         // should not be possible
            return 1;
        }
        if(this.getName().equals(hippoBean.getName())) {
            if(this.equals(hippoBean)) {
                return 0;
            }
            // do not return 0, because then this.equals(hippoFolder) should also be true
            return 1;
        }
        return this.getName().compareTo(hippoBean.getName());
        
    }

    /**
     * Detach the jcr Node. Already loaded properties and variables are still available. 
     */
    public void detach(){
        if(this.path == null) {
            this.path = valueProvider.getPath();
        }
        this.valueProvider.detach();
        this.node = null;
        this.detached = true;
    }
    
  /**
   * Try to attach the jcr Node again with this session. 
   * @param session
   */
    public void attach(Session session) {
       try {
        if(session.itemExists(this.path)) {
            Item item = session.getItem(this.path); 
            if(item instanceof Node) {
                 this.valueProvider = new JCRValueProviderImpl((Node)item);
            } else {
                log.warn("Cannot attach an item that is not a jcr property: '{}'", this.path);
            }
        }
    } catch (RepositoryException e) {
        log.error("Repository exception while trying to attach jcr node: {}", e);
      }
    }
    
}
