/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.jackrabbit.ocm.hippo;

import java.util.Collections;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.ocm.mapper.impl.annotation.Field;
import org.hippoecm.hst.content.beans.NodeAware;
import org.hippoecm.hst.content.beans.SimpleObjectConverter;
import org.hippoecm.hst.content.beans.SimpleObjectConverterAware;
import org.hippoecm.hst.provider.jcr.JCRValueProvider;
import org.hippoecm.hst.provider.jcr.JCRValueProviderImpl;
import org.hippoecm.hst.util.NOOPELMap;
import org.hippoecm.hst.util.PathUtils;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HippoStdNode implements NodeAware, SimpleObjectConverterAware {

    private static Logger log = LoggerFactory.getLogger(HippoStdFolder.class);
    
    private String equalsComparatorId;
    
    private transient SimpleObjectConverter simpleObjectConverter;
    private JCRValueProvider valueProvider;
    private HippoStdFolder parentFolder;

    public javax.jcr.Node getNode() {
        javax.jcr.Node node = null;
        
        if (this.valueProvider != null) {
            node = this.valueProvider.getJcrNode();
        }
        
        return node;
    }

    public void setNode(javax.jcr.Node node) {
        if (this.valueProvider == null) {
            this.valueProvider = new JCRValueProviderImpl(node);
        }
    }

    public SimpleObjectConverter getSimpleObjectConverter() {
        return this.simpleObjectConverter;
    }

    public void setSimpleObjectConverter(SimpleObjectConverter simpleObjectConverter) {
        this.simpleObjectConverter = simpleObjectConverter;
    }

    @Field(path=true)
    public String getPath() {
       if(this.valueProvider != null) {
           return this.valueProvider.getPath();
       }
       return null;
    }

    public void setPath(String path) {
        // no-op. just for jackrabbit ocm mapping.
    }
    
    public String getName() {
        String name = "";

        if (this.valueProvider != null) {
            name = this.valueProvider.getName();
        }

        return name;
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
    
    public HippoStdNode getHippoStdNode(String relPath){
        if(relPath == null) {
            log.warn("Cannot get HippoStdNode for a relative path that is null.");
            return null;
        }
        if(!relPath.equals(PathUtils.normalizePath(relPath))) {
            log.warn("Relative path does end or start with a slash. Removing leading and trailing slashes");
            relPath = PathUtils.normalizePath(relPath);
        }
        if(this.getNode() == null) {
            log.warn("Node is detached. Cannot get document with relative path '{}'", relPath);
            return null;
        }
        Session session = null;
        try {
            session = getNode().getSession();
        } catch (RepositoryException e) {
            log.warn("Node's session is available. Cannot get document with relative path '{}'", relPath);
            return null;
        }
        String absPath = this.getPath() + "/" + relPath;
        Object o = getSimpleObjectConverter().getObject(session, absPath);
        if(o instanceof HippoStdNode) {
            return (HippoStdNode)o;
        } else {
            log.warn("Cannot return a HippoStdNode for location '{}'. Return null", absPath);
            return null;
        }
        
    }
    

    public HippoStdFolder getParentFolder() {
        if (this.parentFolder == null) {
            if (getNode() != null && getSimpleObjectConverter() != null) {
                try {
                    javax.jcr.Node parent = getNode().getParent();
                    
                    if (parent.isNodeType(HippoNodeType.NT_HANDLE)) {
                        parent = parent.getParent();
                    }
                    
                    this.parentFolder = (HippoStdFolder) getSimpleObjectConverter().getObject(getNode().getSession(), parent.getPath());
                } catch (RepositoryException e) {
                    if (log.isDebugEnabled()) {
                        log.warn("Cannot retrieve parent folder: {}", e.getMessage(), e);
                    } else if (log.isWarnEnabled()) {
                        log.warn("Cannot retrieve parent folder: {}", e.getMessage());
                    }
                }
            }
        }
        
        return this.parentFolder;
    }
    
    /**
     * A convenience method capable of comparing two HippoStdNode instances for you for the underlying jcr node. 
     * 
     * When the nodes being compared have the same canonical node (physical equivalence) this method returns true.
     * @param compare the object to compare to
     * @return <code>true</code> if the object compared has the same canonical node
     */
    public boolean equalCompare(Object compare){
        return (Boolean)new ComparatorMap().get(compare);
    }
    
    /**
     * A convenience method capable of comparing two HippoStdNode instances for you for the underlying jcr node. 
     * 
     * When the nodes being compared have the same canonical node (physical equivalence) the get(Object o) returns true.
     * In expression language, for example jsp, you can use to compare as follows:
     * 
     * <code>${mydocument.equalComparator[otherdocument]}</code>
     * 
     * this only returns true when mydocument and otherdocument have the same canonical node
     * 
     * @return a ComparatorMap in which you can compare HippoStdNode's via the get(Object o)
     */
    public Map<Object,Object> getEqualComparator() {
        return new ComparatorMap();
    }
    
    public class ComparatorMap extends NOOPELMap {
        public Object get(Object compare) {
            if(! (compare instanceof HippoStdNode)) {
                return false;
            }
            
            HippoStdNode compareNode = (HippoStdNode)compare;  
            if(compareNode.equalsComparatorId == null) {
               HippoNode node = (HippoNode)compareNode.getNode();
               compareNode.equalsComparatorId = fetchComparatorId(node);
               if(compareNode.equalsComparatorId == null) {
                   log.warn("Cannot compare detached node or nodes having no physical referenceable node");
                   return false;
               }
            }
            if(HippoStdNode.this.equalsComparatorId == null) {
                HippoNode node = (HippoNode)HippoStdNode.this.getNode();
                HippoStdNode.this.equalsComparatorId = fetchComparatorId(node);
                if(HippoStdNode.this.equalsComparatorId == null) {
                    log.warn("Cannot compare detached node or nodes having no physical referenceable node");
                    return false;
                }
            }
            if(compareNode.equalsComparatorId != null && HippoStdNode.this.equalsComparatorId != null ) {
                return compareNode.equalsComparatorId.equals(HippoStdNode.this.equalsComparatorId);
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
    
}
