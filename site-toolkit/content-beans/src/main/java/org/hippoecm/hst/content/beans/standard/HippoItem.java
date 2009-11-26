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
import javax.jcr.PathNotFoundException;
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

public class HippoItem implements HippoBean {

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

    public String getName() {
        return valueProvider.getName();
    }

    public String getPath() {
        if (this.path == null && valueProvider != null) {
            this.path = valueProvider.getPath();
        }
        return this.path;
    }

    public String getCanonicalUUID() {
        if (this.canonicalId != null) {
            return canonicalId;
        }
        if (this.node == null) {
            log.warn("Cannot get uuid for detached node '{}'", this.getPath());
            return null;
        }
        try {
            if (this.node.hasProperty(HippoNodeType.HIPPO_UUID)) {
                this.canonicalId = this.node.getProperty(HippoNodeType.HIPPO_UUID).getString();
            } else if (this.node.isNodeType("mix:referenceable")) {
                this.canonicalId = this.node.getUUID();
            }
        } catch (RepositoryException e) {
            log.warn("RepositoryException while trying to get canonical uuid for '" + this.getPath() + "'", e);
        }
        return this.canonicalId;

    }

    /**
     * values in the map can be of type String, String[], Boolean, Boolean[], Long, Long[], Double, Double[] or Calendar, Calendar[]
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

    public <T> T getProperty(String name) {
        return (T) getProperties().get(name);
    }

    /**
     * @see {@link #getProperties()}
     * @see org.hippoecm.hst.content.beans.standard.HippoBean#getProperty()
     */
    public Map<String, Object> getProperty() {
        return this.getProperties();
    }

    public <T> T getBean(String relPath) {
        try {
            Object o = this.objectConverter.getObject(node, relPath);
            if (o == null) {
                log.debug("Cannot get bean for relpath {} for current bean {}.", relPath, this.getPath());
                return null;
            }
            return (T) o;
        } catch (ObjectBeanManagerException e) {
            log.warn("Cannot get Object at relPath '{}' for '{}'", relPath, this.getPath());
        }
        return null;
    }

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
            log.warn("Cannot get Object at relPath '{}' for '{}'", relPath, this.getPath());
        }
        return null;
    }

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

    public <T extends HippoBean> List<T> getLinkedBeans(String relPath, Class<T> beanMappingClass) {
        List<T> childBeans = new ArrayList<T>();
        String fromNode = null;
        String nodeName = relPath;
        if(relPath.contains("/")) {
            fromNode = relPath.substring(0, relPath.lastIndexOf("/"));
            nodeName = relPath.substring(relPath.lastIndexOf("/")+1);
        }
        Node relNode = node;
        if(fromNode != null) {
            try {
                relNode = relNode.getNode(fromNode);
            } catch (PathNotFoundException e) {
                log.debug("did not find relPath '{}' at '{}'. Return empty list", relPath, this.getPath());
                return childBeans;
            } catch (RepositoryException e) {
                log.warn("RepositoryException" , e);
                return childBeans;
            }
        }
        
        try {
            NodeIterator nodes = relNode.getNodes();
            while (nodes.hasNext()) {
                Node child = nodes.nextNode();
                if (child == null) {
                    continue;
                }
                Object bean;
                try {
                    bean = this.objectConverter.getObject(child);
                    if (child.getName().equals(nodeName)) {
                        if(bean instanceof HippoMirrorBean) {
                            HippoBean linked = ((HippoMirrorBean)bean).getReferencedBean();
                            if (linked != null && beanMappingClass.isAssignableFrom(linked.getClass())) {
                                childBeans.add((T) linked);
                            }
                        }
                    }
                } catch (ObjectBeanManagerException e) {
                    if(log.isDebugEnabled()) {
                        log.warn("Could not map jcr node to bean: '{}'", e);
                    } else {
                        log.warn("Could not map jcr node to bean: '{}'", e.getMessage());
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

    public <T> List<T> getChildBeansByName(String childNodeName) {
        List<T> childBeans = new ArrayList<T>();
        NodeIterator nodes;
        try {
            nodes = node.getNodes();
            while (nodes.hasNext()) {
                Node child = nodes.nextNode();
                if (child == null) {
                    continue;
                }
                if (child.getName().equals(childNodeName)) {
                    try {
                        Object bean = this.objectConverter.getObject(child);
                        if (bean != null) {
                            childBeans.add((T) bean);
                        }
                    } catch (ObjectBeanManagerException e) {
                        log.warn("Skipping bean: {}", e);
                    }
                }
            }
        } catch (RepositoryException e) {
            log.error("RepositoryException: Error while trying to create childBeans:", e);
            return new ArrayList<T>();
        }
        return childBeans;
    }

    public <T> List<T> getChildBeans(String jcrPrimaryNodeType) {
        Class annotatedClass = this.objectConverter.getAnnotatedClassFor(jcrPrimaryNodeType);
        if (annotatedClass == null) {
            log
                    .warn(
                            "Cannot get ChildBeans for jcrPrimaryNodeType '{}' because there is no annotated class for this node type. Return null",
                            jcrPrimaryNodeType);
            return new ArrayList<T>();
        }
        if (this.node == null) {
            log.warn("Cannot get ChildBeans for jcrPrimaryNodeType '{}' because the jcr node is detached. ",
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

                boolean nodeTypeMatch = false;
                if (child.getPrimaryNodeType().getName().equals(jcrPrimaryNodeType)) {
                    nodeTypeMatch = true;
                } else if (child.isNodeType(HippoNodeType.NT_HANDLE)) {
                    if (child.hasNode(child.getName())) {
                        child = child.getNode(child.getName());
                        if (child.getPrimaryNodeType().getName().equals(jcrPrimaryNodeType)) {
                            nodeTypeMatch = true;
                        }
                    }
                }

                if (!nodeTypeMatch) {
                    continue;
                }

                try {
                    Object bean = this.objectConverter.getObject(child);
                    if (bean != null && annotatedClass.isAssignableFrom(bean.getClass())) {
                        childBeans.add((T) bean);
                    }
                } catch (ObjectBeanManagerException e) {
                    log.warn("Skipping bean: {}", e);
                }

            }
        } catch (RepositoryException e) {
            log.error("RepositoryException: Cannot get ChildBeans for jcrPrimaryNodeType: '" + jcrPrimaryNodeType
                    + "' ", e);
            return new ArrayList<T>();
        }

        return childBeans;
    }

    public HippoBean getParentBean() {
        try {
            Node parentNode = valueProvider.getParentJcrNode();
            if (parentNode == null) {
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
                log.warn("Bean is not an instance of HippoBean. Return null : ", o);
            }
        } catch (ObjectBeanManagerException e) {
            log.warn("Failed to get parent object for '{}'", this.getPath());
        } catch (RepositoryException e) {
            log.warn("Failed to get parent object for '{}'", this.getPath());
        }
        return null;
    }

    public boolean isAncestor(HippoBean compare) {
        if (this.getPath() == null || compare.getPath() == null) {
            log.warn("Cannot compare the HippoBeans as one as a path that is null. Return false.");
            return false;
        }
        if (compare.getPath().startsWith(this.getPath())) {
            if (!isSelf(compare)) {
                return true;
            }
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
            log.warn("Cannot compare the HippoBeans as one as a path that is null. Return false.");
            return false;
        }
        if (this.getPath().startsWith(compare.getPath())) {
            if (!isSelf(compare)) {
                return true;
            }
        }
        return false;
    }

    public boolean isSelf(HippoBean compare) {
        if (this.getPath() == null || compare.getPath() == null) {
            log.warn("Cannot compare the HippoBeans as one as a path that is null. Return false.");
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
            if (compareItem.canonicalId == null) {
                HippoNode node = (HippoNode) compareItem.getNode();
                if (node == null) {
                    return false;
                }
                compareItem.canonicalId = compareItem.getCanonicalUUID();
                if (compareItem.canonicalId == null) {
                    return false;
                }
            }
            if (HippoItem.this.canonicalId == null) {
                HippoNode node = (HippoNode) HippoItem.this.getNode();
                if (node == null) {
                    return false;
                }
                HippoItem.this.canonicalId = HippoItem.this.getCanonicalUUID();
                if (HippoItem.this.canonicalId == null) {
                    return false;
                }
            }
            if (compareItem.canonicalId != null && HippoItem.this.canonicalId != null) {
                return compareItem.canonicalId.equals(HippoItem.this.canonicalId);
            }
            return false;
        }

    }

    /**
     *  The standard HippoItem has a natural ordering based on node name.
     *  if you need ordering on a different basis, override this method
     */

    public int compareTo(HippoBean hippoBean) {
        // if hippoFolder == null, a NPE will be thrown which is fine because the arg is not allowed to be null.
        if (this.getName() == null) {
            // should not be possible
            return -1;
        }
        if (hippoBean.getName() == null) {
            // should not be possible
            return 1;
        }
        if (this.getName().equals(hippoBean.getName())) {
            if (this.equals(hippoBean)) {
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
    public void detach() {
        if (this.path == null) {
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
            if (session.itemExists(this.path)) {
                Item item = session.getItem(this.path);
                if (item instanceof Node) {
                    this.valueProvider = new JCRValueProviderImpl((Node) item);
                } else {
                    log.warn("Cannot attach an item that is not a jcr property: '{}'", this.path);
                }
            }
        } catch (RepositoryException e) {
            log.error("Repository exception while trying to attach jcr node: {}", e);
        }
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
     * hashcode is based on the absolute path of the backing jcr node.
     */
    @Override
    public int hashCode() {
        return this.getPath() == null ? super.hashCode() : this.getPath().hashCode();
    }

}
