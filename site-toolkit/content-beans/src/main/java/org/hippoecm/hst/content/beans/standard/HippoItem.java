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
package org.hippoecm.hst.content.beans.standard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.hst.content.beans.ObjectBeanManagerException;
import org.hippoecm.hst.content.beans.index.IndexField;
import org.hippoecm.hst.content.beans.manager.ObjectConverter;
import org.hippoecm.hst.content.beans.standard.HippoAvailableTranslationsBean.NoopTranslationsBean;
import org.hippoecm.hst.provider.jcr.JCRValueProvider;
import org.hippoecm.hst.provider.jcr.JCRValueProviderImpl;
import org.hippoecm.hst.util.NOOPELMap;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HippoItem implements HippoBean {
 
    private static Logger log = LoggerFactory.getLogger(HippoItem.class);

    protected transient Node node;
    protected String comparePath;
    protected String path;
    protected String name;
    protected String localizedName;
    protected JCRValueProvider valueProvider;
    protected transient ObjectConverter objectConverter;
    protected boolean detached = false;

    private String canonicalUUID;

    private boolean availableTranslationsInitialized;
    @SuppressWarnings("rawtypes")
    private HippoAvailableTranslationsBean availableTranslations;

    public void setObjectConverter(ObjectConverter objectConverter) {
        this.objectConverter = objectConverter;
    }

    public ObjectConverter getObjectConverter() {
        return this.objectConverter;
    }

    public void setNode(Node node) {
        this.node = node;
        this.valueProvider = new JCRValueProviderImpl(node);
    }

    public Node getNode() {
        return this.node;
    }

    public JCRValueProvider getValueProvider() {
        return this.valueProvider;
    }

    @Override
    public String getIdentifier() {
        return getCanonicalUUID();
    }
    
    public void setIdentifier(String identifier) {
        this.canonicalUUID = identifier;
    }

    @IndexField(ignoreInCompound = true)
    public String getName() {
        if (name != null) {
            return name;
        }
        if (valueProvider != null) {
            name = valueProvider.getName();
        }
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @IndexField(ignoreInCompound = true)
    public String getLocalizedName() {
        if (localizedName != null) {
            return localizedName;
        }
        if (valueProvider != null) {
            localizedName = valueProvider.getLocalizedName();
        }
        return localizedName;
    }

    public void setLocalizedName(String localizedName) {
        this.localizedName = localizedName;
    }

    public String getPath() {
        if (path != null) {
            return path;
        }

        if (valueProvider != null) {
            path = valueProvider.getPath();
        }
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    @IndexField(ignoreInCompound = true)
    public String getComparePath() {
        if (comparePath != null) {
            return comparePath;
        }
        HippoNode node = (HippoNode) getNode();
        if (node == null) {
            throw new IllegalStateException("Cannot get comparePath if jcr node is null");
        }
        Node canonical = null;
        try {
            canonical = node.getCanonicalNode();
            if (canonical == null) {
                // virtual only
                comparePath = node.getPath();
            } else {
                comparePath = canonical.getPath();
            }
        } catch (RepositoryException e) {
            throw new IllegalStateException("Cannot get comparePath", e);
        }
        return comparePath;
    }
    
    public void setComparePath(String comparePath) {
        this.comparePath = comparePath;
    }

    @Override
    public String getCanonicalUUID() {
        if (canonicalUUID != null) {
            return canonicalUUID;
        }
        if (getValueProvider() == null) {
            log.warn("Cannot get canonicalUUID for '{}' because no value provider. Return null", getPath());
            return null;
        }
        return getValueProvider().getIdentifier();
    }

    @Override
    public String getCanonicalPath() {
        return getValueProvider().getCanonicalPath();
    }

    /**
     * values in the map can be of type String, String[], Boolean, Boolean[], Long, Long[], Double, Double[] or Calendar, Calendar[]
     * <br/>
     * {@inheritDoc}
     */
    public Map<String, Object> getProperties() {
        Map<String, Object> properties = null;
        if (this.valueProvider == null) {
            properties = Collections.emptyMap();
        } else {
            properties = this.valueProvider.getProperties();
        }
        return properties;
    }

    @SuppressWarnings("unchecked")
    public <T> T getProperty(String name) {
        return (T) getProperties().get(name);
    }


    @Override
    public <T> T getProperty(String name, T defaultValue) {
        
        @SuppressWarnings("unchecked")
        T val = (T) getProperties().get(name);
        if(val == null) {
            return defaultValue;
        }
        return val;
    }

    
    /**
     * @see {@link #getProperties()}
     * @see org.hippoecm.hst.content.beans.standard.HippoBean#getProperty()
     */
    public Map<String, Object> getProperty() {
        return this.getProperties();
    }

    @SuppressWarnings("unchecked")
    public <T> T getBean(String relPath) {
        try {
            Object o = this.objectConverter.getObject(node, relPath);
            if (o == null) {
                log.debug("Cannot get bean for relpath {} for current bean {}.", relPath, this.getPath());
                return null;
            }
            return (T) o;
        } catch (ObjectBeanManagerException e) {
            log.info("Cannot get Object at relPath '{}' for '{}'", relPath, this.getPath());
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public <T extends HippoBean> T getBean(String relPath, Class<T> beanMappingClass) {
        try {
            Object o = this.objectConverter.getObject(node, relPath);
            if (o == null) {
                log.debug("Cannot get bean for relpath {} for current bean {}.", relPath, this.getPath());
                return null;
            }
            if (!beanMappingClass.isAssignableFrom(o.getClass())) {
                log.debug("Expected bean of type '{}' but found of type '{}'. Return null.",
                        beanMappingClass.getName(), o.getClass().getName());
                return null;
            }
            return (T) o;
        } catch (ObjectBeanManagerException e) {
            log.info("Cannot get Object at relPath '{}' for '{}'", relPath, this.getPath());
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public <T extends HippoBean> List<T> getChildBeans(Class<T> beanMappingClass) {
        List<T> childBeans = new ArrayList<T>();
        NodeIterator nodes;
        try {
            nodes = node.getNodes();
            while (nodes.hasNext()) {
                Node child = nodes.nextNode();
                if (child == null) {
                    continue;
                }
               try {
                    Object bean = this.objectConverter.getObject(child);
                    if (bean != null) {
                        if (beanMappingClass != null) {
                            if(beanMappingClass.isAssignableFrom(bean.getClass())) {
                                childBeans.add((T) bean); 
                            } else {
                                log.debug("Skipping bean of type '{}' because not of beanMappingClass '{}'", bean.getClass().getName(), beanMappingClass.getName());
                            }
                        } else {
                            childBeans.add((T) bean); 
                        }
                    }
                } catch (ObjectBeanManagerException e) {
                    log.info("Skipping bean: {}", e);
                }
            }
        } catch (RepositoryException e) {
            log.error("RepositoryException: Error while trying to create childBeans:", e);
            return new ArrayList<T>();
        }
        return childBeans;
    }


    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <T> List<T> getChildBeansByName(String childNodeName) {
        return getChildBeansByName(childNodeName, (Class)null);
    }
    
    @SuppressWarnings("unchecked")
    public <T extends HippoBean> List<T> getChildBeansByName(String childNodeName, Class<T> beanMappingClass) {
        List<T> childBeans = new ArrayList<T>();
        NodeIterator nodes;
        try {
            nodes = node.getNodes(childNodeName);
            while (nodes.hasNext()) {
                Node child = nodes.nextNode();
                if (child == null) {
                    continue;
                }
                try {
                    Object bean = this.objectConverter.getObject(child);
                    if (bean != null) {
                        if (beanMappingClass != null) {
                            if(beanMappingClass.isAssignableFrom(bean.getClass())) {
                                childBeans.add((T) bean);
                            } else {
                                log.debug("Skipping bean of type '{}' because not of beanMappingClass '{}'", bean.getClass().getName(), beanMappingClass.getName());
                            }
                        } else {
                            childBeans.add((T) bean);
                        }
                    }
                } catch (ObjectBeanManagerException e) {
                    log.info("Skipping bean: {}", e);
                }
            }
        } catch (RepositoryException e) {
            log.error("RepositoryException: Error while trying to create childBeans:", e);
            return new ArrayList<T>();
        }
        return childBeans;
    }

    

    @SuppressWarnings("unchecked")
    public <T> List<T> getChildBeans(String jcrPrimaryNodeType) {
        @SuppressWarnings("rawtypes")
        Class annotatedClass = this.objectConverter.getAnnotatedClassFor(jcrPrimaryNodeType);
        if (annotatedClass == null) {
            log.info("Cannot get ChildBeans for jcrPrimaryNodeType '{}' because there is no annotated class for this node type. Return null",
                      jcrPrimaryNodeType);
            return new ArrayList<T>();
        }
        if (this.node == null) {
            log.info("Cannot get ChildBeans for jcrPrimaryNodeType '{}' because the jcr node is detached. ",
                    jcrPrimaryNodeType);
            return new ArrayList<T>();
        }

        List<T> childBeans = new ArrayList<T>();
        NodeIterator nodes;
        try {
            nodes = node.getNodes();
            while (nodes.hasNext()) {
                Node child = nodes.nextNode();
                if (child == null) {
                    continue;
                }

                try {
                    String nodeObjectType = objectConverter.getPrimaryObjectType(child);
                    if (nodeObjectType != null && nodeObjectType.equals(jcrPrimaryNodeType)) {
                        T bean = (T)this.objectConverter.getObject(child);
                        if (bean != null) { // && annotatedClass.isAssignableFrom(bean.getClass())) {
                            childBeans.add(bean);
                        }
                    }
                } catch (ObjectBeanManagerException e) {
                    log.info("Skipping bean: {}", e);
                } 

            }
        } catch (RepositoryException e) {
            log.error("RepositoryException: Cannot get ChildBeans for jcrPrimaryNodeType: '" + jcrPrimaryNodeType
                    + "' ", e);
            return new ArrayList<T>();
        }

        return childBeans;
    }
  
    @SuppressWarnings("unchecked")
    public <T extends HippoBean> T getLinkedBean(String relPath, Class<T> beanMappingClass) {
        HippoMirrorBean mirror = getBean(relPath, HippoMirrorBean.class);
        if (mirror == null) {
            return null;
        }
        HippoBean bean = mirror.getReferencedBean();
        if (bean == null) {
            return null;
        }
        if (!beanMappingClass.isAssignableFrom(bean.getClass())) {
            log.debug("Expected bean of type '{}' but found of type '{}'. Return null.", beanMappingClass.getName(),
                    bean.getClass().getName());
            return null;
        }
        return (T) bean;
    }

    @SuppressWarnings("unchecked")
    public <T extends HippoBean> List<T> getLinkedBeans(String relPath, Class<T> beanMappingClass) {
        List<T> childBeans = new ArrayList<T>();
        String relFromNodePath = null;
        String nodeName = relPath;
        if(relPath.contains("/")) {
            relFromNodePath = relPath.substring(0, relPath.lastIndexOf("/"));
            nodeName = relPath.substring(relPath.lastIndexOf("/")+1);
        }
        Node relNode = node;
        if(relFromNodePath != null) {
            try {
                relNode = relNode.getNode(relFromNodePath);
            } catch (PathNotFoundException e) {
                log.debug("did not find relPath '{}' at '{}'. Return empty list", relPath, this.getPath());
                return childBeans;
            } catch (RepositoryException e) {
                log.warn("RepositoryException" , e);
                return childBeans;
            }
        }
        
        try {
            NodeIterator nodes = relNode.getNodes(nodeName);
            while (nodes.hasNext()) {
                Node child = nodes.nextNode();
                if (child == null) {
                    continue;
                }
                Object bean;
                try {
                    bean = this.objectConverter.getObject(child);
                    if(bean instanceof HippoMirrorBean) {
                        HippoBean linked = ((HippoMirrorBean)bean).getReferencedBean();
                        if (linked != null && beanMappingClass.isAssignableFrom(linked.getClass())) {
                            childBeans.add((T) linked);
                        }
                    }
                } catch (ObjectBeanManagerException e) {
                    if(log.isDebugEnabled()) {
                        log.info("Could not map jcr node to bean: '{}'", e);
                    } else {
                        log.info("Could not map jcr node to bean: '{}'", e.getMessage());
                    }
                }
                
            }
        } catch (PathNotFoundException e) {
            log.debug("did not find relPath '{}' at '{}'. Return empty list", relPath, this.getPath());
        } catch (RepositoryException e) {
            log.warn("RepositoryException" , e);
        }
        return childBeans;
    }

    public HippoBean getParentBean() {
        try {
            Node parentNode = valueProvider.getParentJcrNode();
            if (parentNode == null) {
                log.warn("Cannot get parent bean for detached bean");
                return null;
            }
            if (parentNode.isNodeType(HippoNodeType.NT_HANDLE)) {
                parentNode = parentNode.getParent();
            }
            Object o = this.objectConverter.getObject(parentNode);
            if (o == null) {
                log.debug("Failed to get parent bean for node '{}'", parentNode.getPath());
                return null;
            }
            if (o instanceof HippoBean) {
                return (HippoBean) o;
            } else {
                log.info("Bean is not an instance of HippoBean but '{}'. Return null : ", o.getClass().getName());
            }
        } catch (ObjectBeanManagerException e) {
            log.warn("Failed to get parent object for '{}'", this.getPath());
        } catch (RepositoryException e) {
            log.warn("Failed to get parent object for '{}'", this.getPath());
        }
        return null;
    }

    @Override
    public <T extends HippoBean> T getCanonicalBean() {
        if(this.getNode() == null) {
            log.info("Cannot get canonical bean for detached bean. Return just the current bean instance");
            return (T) this;
        }
        try {
            HippoNode hn = (HippoNode)this.getNode();
            if (!hn.isVirtual()) {
                // already canonical
                return (T)this;
            }
            Node canonical = hn.getCanonicalNode();
            if(canonical == null) {
                log.debug("Cannot get canonical for a node that is virtual only: '{}'. Return null", this.getPath());
                return null;
            }
            Object o = this.objectConverter.getObject(canonical);
            if (o instanceof HippoBean) {
                log.debug("Getting canonical bean succeeded: translated from '{}' --> '{}'", this.getPath(), ((HippoBean) o).getPath());
                return (T) o;
            } else {
                log.info("Bean is not an instance of HippoBean. Return null : ", o);
            }
        } catch (ObjectBeanManagerException e) {
            log.warn("Exception while trying to fetch canonical bean. Return null : {}", e.toString());
        } catch (RepositoryException e) {
            log.warn("Exception while trying to fetch canonical bean. Return null : {}", e.toString());
        }
        return null;
    }

    public boolean isAncestor(HippoBean compare) {
        if (this.getPath() == null || compare.getPath() == null) {
            log.info("Cannot compare the HippoBeans as one as a path that is null. Return false.");
            return false;
        }
        if (compare.getPath().startsWith(this.getPath()+"/")) {
           return true;
        }
        return false;
    }

    public boolean isLeaf() {
        if (this.getNode() == null) {
            return true;
        }
        try {
            return !(this.node.hasNodes());
        } catch (RepositoryException e) {
            log.error("Repository exception : ", e);
            return true;
        }
    }

    public boolean isDescendant(HippoBean compare) {
        if (this.getPath() == null || compare.getPath() == null) {
            log.info("Cannot compare the HippoBeans as one as a path that is null. Return false.");
            return false;
        }
        if (this.getPath().startsWith(compare.getPath() + "/")) {
            return true;
        }
        return false;
    }

    public boolean isSelf(HippoBean compare) {
        if (this.getPath() == null || compare.getPath() == null) {
            log.info("Cannot compare the HippoBeans as one as a path that is null. Return false.");
            return false;
        }
        if (this.getPath().equals(compare.getPath())) {
            return true;
        }
        return false;
    }

    public boolean isHippoDocumentBean() {
        return this instanceof HippoDocumentBean;
    }

    public boolean isHippoFolderBean() {
        return this instanceof HippoFolderBean;
    }

    @SuppressWarnings("unchecked")
    public <T extends HippoBean> HippoAvailableTranslationsBean<T> getAvailableTranslations() {
        if(!availableTranslationsInitialized) {
            availableTranslationsInitialized = true;
            availableTranslations = new AvailableTranslations<T>(getNode(), getObjectConverter());
        }
        return availableTranslations;
    }
    
    
    public boolean equalCompare(Object compare) {
        return (Boolean) new ComparatorMap().get(compare);
    }

    public Map<Object, Object> getEqualComparator() {
        return new ComparatorMap();
    }

    public class ComparatorMap extends NOOPELMap {
        public Object get(Object compare) {
            if (!(compare instanceof HippoItem)) {
                return false;
            }
            HippoItem compareItem = (HippoItem) compare;
            try {
                return compareItem.getComparePath().equals(HippoItem.this.getComparePath());
            } catch (IllegalStateException e) {
                log.error("Could not compare items correctly : ", e);
                return false;
            }

        }

    }

    /**
     *  The standard HippoItem has a natural ordering based on node name.
     *  if you need ordering on a different basis, override this method
     */

    public int compareTo(HippoBean hippoBean) {
        if (hippoBean == null) {
            throw new NullPointerException("HippoBean to compareTo is not allowed to be null");
        }
        if (this.getName() == null) {
            throw new IllegalStateException("Cannot compare when getName is null");
        }
        if (hippoBean.getName() == null) {
            throw new IllegalStateException("Cannot compare when getName is null");
        }
        int val = this.getName().compareTo(hippoBean.getName());
        if(val != 0) {
            return val;
        }

        if (this.getPath() == null && hippoBean.getPath() == null) {
            return 0;
        }
        
        // return the compareTo of the backing path : this to be in sync with the equals
        if (this.getPath() == null) {
            return 1;
        }
        if (hippoBean.getPath() == null) {
            return -1;
        }
        return this.getPath().compareTo(hippoBean.getPath());
    }


    /**
     * equality is based on the absolute path of the backing jcr node.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (this.getPath() == null) {
            return false;
        }
        if(obj instanceof HippoBean) {
            return this.getPath().equals(((HippoBean)obj).getPath());
        }
        return false;
    }

    /**
     * Detach the jcr Node. Already loaded properties and variables are still available. 
     */
    public void detach() {
        this.valueProvider.detach();
        this.node = null;
        this.detached = true;
    }

    /**
     * Try to attach the jcr Node again with this session.
     * @param session
     */
    public void attach(Session session) {
        if(getPath() == null) {
            log.error("Unable to attach HippoBean again since getPath() is null");
            return;
        }
        try {
            if (session.itemExists(getPath())) {
                Item item = session.getItem(getPath());
                if (item instanceof Node) {
                    this.valueProvider = new JCRValueProviderImpl((Node) item);
                } else {
                    log.info("Cannot attach an item that is not a jcr property: '{}'", getPath());
                }
            }
        } catch (RepositoryException e) {
            log.error("Repository exception while trying to attach jcr node: {}", e);
        }
    }

    /**
     * hashcode is based on the absolute path of the backing jcr node.
     */
    @Override
    public int hashCode() {
        return this.getPath() == null ? super.hashCode() : this.getPath().hashCode();
    }


}
