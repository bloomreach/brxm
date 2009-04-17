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

import java.util.Collections;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.hst.content.beans.NodeAware;
import org.hippoecm.hst.content.beans.ObjectBeanManagerException;
import org.hippoecm.hst.content.beans.manager.ObjectConverter;
import org.hippoecm.hst.content.beans.manager.ObjectConverterAware;
import org.hippoecm.hst.provider.jcr.JCRValueProvider;
import org.hippoecm.hst.provider.jcr.JCRValueProviderImpl;
import org.hippoecm.hst.util.NOOPELMap;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HippoItem implements NodeAware, ObjectConverterAware, Comparable<HippoItem>{

    private static Logger log = LoggerFactory.getLogger(HippoItem.class);
    
    protected String canonicalId;
    protected transient Node node;
    protected JCRValueProvider valueProvider;
    protected transient ObjectConverter objectConverter;
    

    public void setObjectConverter(ObjectConverter objectConverter) {
       this.objectConverter = objectConverter;
    }

    public void setNode(Node node) {
        this.node = node;
        this.valueProvider = new JCRValueProviderImpl(node);
    }
    
    public Node getNode() {
        return node;
    }
    
    public String getName() {
        return valueProvider.getName();
    }
    
    public String getPath(){
       return valueProvider.getPath();
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
    
    public Object getObject(String relPath) {
        try {
            return this.objectConverter.getObject(node, relPath);
        } catch (ObjectBeanManagerException e) {
            log.warn("Cannot get Object at relPath '{}' for '{}'", relPath, this.getPath());
            return null;
        } 
    }
    
    public Object getParentObject(){
        try {
            Node parentNode = valueProvider.getParentJcrNode();
            return this.objectConverter.getObject(parentNode);
        } catch (ObjectBeanManagerException e) {
            log.warn("Failed to get parent object for '{}'", this.getPath());
            return null;
        } 
    }
    
    /**
     * A convenience method capable of comparing two HippoItem instances for you for the underlying jcr node. 
     * 
     * When the nodes being compared have the same canonical node (physical equivalence) this method returns true.
     * @param compare the object to compare to
     * @return <code>true</code> if the object compared has the same canonical node
     */
    public boolean equalCompare(Object compare){
        return (Boolean)new ComparatorMap().get(compare);
    }
    
    /**
     * A convenience method capable of comparing two HippoItem instances for you for the underlying jcr node. 
     * 
     * When the nodes being compared have the same canonical node (physical equivalence) the get(Object o) returns true.
     * In expression language, for example jsp, you can use to compare as follows:
     * 
     * <code>${mydocument.equalComparator[otherdocument]}</code>
     * 
     * this only returns true when mydocument and otherdocument have the same canonical node
     * 
     * @return a ComparatorMap in which you can compare HippoItem's via the get(Object o)
     */
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
    
    public int compareTo(HippoItem hippoItem) {
        // if hippoFolder == null, a NPE will be thrown which is fine because the arg is not allowed to be null.
        if(this.getName() == null) {
            // should not be possible
            return -1;
        }
        if(hippoItem.getName() == null) {
         // should not be possible
            return 1;
        }
        if(this.getName().equals(hippoItem.getName())) {
            if(this.equals(hippoItem)) {
                return 0;
            }
            // do not return 0, because then this.equals(hippoFolder) should also be true
            return 1;
        }
        return this.getName().compareTo(hippoItem.getName());
        
    }

}
