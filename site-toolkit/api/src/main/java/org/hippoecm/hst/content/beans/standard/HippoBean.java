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

import java.util.List;
import java.util.Map;

import javax.jcr.Node;

import org.hippoecm.hst.content.beans.NodeAware;
import org.hippoecm.hst.content.beans.manager.ObjectConverterAware;
import org.hippoecm.hst.provider.jcr.JCRValueProvider;

public interface HippoBean extends NodeAware, ObjectConverterAware, Comparable<HippoBean> {

    /**
     * This returns the backing jcr node for this bean. 
     * 
     * @return the backing jcr node for this bean. <code>null</code> if the bean is detached
     */
    Node getNode();
    
    JCRValueProvider getValueProvider();

    /**
     * This returns the node name of the backing jcr node for this bean
     * 
     * @return the node name of the backing jcr node. This method never returns <code>null</code>
     */
    String getName();

    /**
     * This returns the absolute path of the backing jcr node for this bean, for example /documents/content/myprojec/news/article
     * 
     * When the jcr node is virtual, it returns the virtual path.
     * 
     * @return the absolute jcr path of the backing jcr node. This method never returns <code>null</code>
     */
    String getPath();
    
    /**
     * @return the jcr uuid of the backing canonical (physical) jcr node or <code>null</code> in case of any exception or when the jcr node is detached.
     * For {@link HippoDocumentBean}'s, the uuid of the handle might be more valuable, which you can get with {@link HippoDocumentBean#getCanonicalHandleUUID()}. 
     */
    String getCanonicalUUID();

    /**
     * Same as getProperty, where getProperty is only there for having a nice .getProperty['propname'] in jsp expression language
     * @see #getProperty()
     */
    Map<String, Object> getProperties();

    /**
     * Return types can be of type String, String[], Boolean, Boolean[], Long, Long[], Double, Double[] or Calendar, Calendar[]
     * @param <T>
     * @param name
     * @return The return type is either String, String[], Boolean, Boolean[], Long, Long[], Double, Double[] or Calendar, Calendar[]
     */
    <T> T getProperty(String name);

    /**
     * @return Map of all properties, where the values can be of type String, String[], Boolean, Boolean[], Long, Long[], Double, Double[] or Calendar, Calendar[]
     */
    Map<String, Object> getProperty();
    
    /**
     * 
     * This methods fetches a child <code>HippoBean</code> with the relative path relPath. A relPath is not allowed to start with a "/", 
     * as this is considered to be an absolute path. 
     * 
     * For example <code>getBean("x/y")</code> is a valid relative path. <code>"../x"</code> is also supported, but dis-encouraged as the <code>"../"</code> works directly on jcr level, which 
     * does not take the <code>hippo:handle</code> intermediate node into account in case of a HippoDocument bean. Always preferred to use is {@link #getParentBean()} instead
     * of using <code>"../"</code>. In case of a HippoDocument kind of bean, the {@link #getParentBean()} jumps to the parent of the handle, while <code>../</code> jumps to the handle, resulting
     * in the exact same bean in case of a HippoDocument
     *  
     * 
     * @param relPath a path that does not start with a "/"
     * @return returns the <code>HippoBean</code> with relative path <code>relPath</code> to this bean, or <code>null</code> when it does not exist, or when the relPath is not a valid relative path
     */
    <T> T getBean(String relPath);
    
    /**
     * Returns the parent bean wrt this bean. Note that this does not automatically imply
     * a bean with the parent jcr node of this bean. When the parent node is of type "hippo:handle",
     * the parent of the handle must be taken
     * @return the parent bean wrt this bean, or if the object converter cannot create a bean for the parent, return <code>null</code>
     */
    HippoBean getParentBean();
    
    /**
     * @param <T>
     * @param childNodeName
     * @return List<HippoBean> where the backing jcr nodes have the name childNodeName
     */
    <T> List<T> getChildBeansByName(String childNodeName);
    
    
    /**
     * Returns all the jcr nodes that are child nodes of this bean, and have primary jcr nodetype equal to param jcrPrimaryNodeType.
     * If a jcr child node is of primary nodetype 'hippo:handle', we look whether the underlying 'Document' has the corresponding 
     * jcrPrimaryNodeType. If so, we return the bean for this 'Document'.
     * 
     * @param <T> 
     * @param jcrPrimaryNodeType
     * @return List<HippoBean> where the backing jcr nodes are of type jcrPrimaryNodeType
     */
    <T> List<T> getChildBeans(String jcrPrimaryNodeType);
    
    /**
     * @return <code>true</code> is this HippoBean is an instanceof <code>{@link HippoDocumentBean}</code>
     */
    boolean isHippoDocumentBean();
    
    /**
     * @return <code>true</code> is this HippoBean is an instanceof <code>{@link HippoFolderBean}</code>
     */
    boolean isHippoFolderBean();
    
    /**
     * @return <code>true</code> when the backing jcr Node has no child nodes
     */
    boolean isLeaf();
    
    /**
     * Returns <code>true</code> when this <code>HippoBean</code> is an ancestor of the <code>compare</code> HippoBean. 
     * Note that this is done by the jcr path of the backing jcr node. In case of a virtual node, the virtual path 
     * is taken. 
     * @param compare 
     * @return <code>true</code> when this <code>HippoBean</code> is an ancestor of the <code>compare</code> HippoBean. 
     */
    boolean isAncestor(HippoBean compare);
    
    /**
     * Returns <code>true</code> when this <code>HippoBean</code> is an descendant of the <code>compare</code> HippoBean. 
     * Note that this is done by the jcr path of the backing jcr node. In case of a virtual node, the virtual path 
     * is taken. This means, that although the canonical nodes of the backing jcr nodes might return true for this method, this
     * does not automatically holds for the beans of the virtual nodes.
     * @param compare 
     * @return <code>true</code> when this <code>HippoBean</code> is an descendant of the <code>compare</code> HippoBean. 
     */
    boolean isDescendant(HippoBean compare);
    
    /**
     * Returns <code>true</code> when this <code>HippoBean</code> has a underlying jcr node with the same jcr path as the
     * <code>compare</code> HippoBean. This means, that two HippoBeans might have the same canonical underlying jcr node, but
     * do not return <code>true</code> because their virtual node might have different jcr paths.
     * @param compare
     * @return Returns <code>true</code> when this <code>HippoBean</code> has the same underlying jcr node path as the <code>compare</code> HippoBean. 
     */
    boolean isSelf(HippoBean compare);
    
    /**
     * A convenience method capable of comparing two HippoBean instances for you for the underlying jcr node. 
     * 
     * When the nodes being compared have the same canonical node (physical equivalence) this method returns true.
     * @param compare the object to compare to
     * @return <code>true</code> if the object compared has the same canonical node
     */
    
    boolean equalCompare(Object compare);
    
    /**
     * A convenience method capable of comparing two HippoBean instances for you for the underlying jcr node. 
     * 
     * When the nodes being compared have the same canonical node (physical equivalence) the get(Object o) returns true.
     * In expression language, for example jsp, you can use to compare as follows:
     * 
     * <code>${mydocument.equalComparator[otherdocument]}</code>
     * 
     * this only returns true when mydocument and otherdocument have the same canonical node
     * 
     * @return a ComparatorMap in which you can compare HippoBean's via the get(Object o)
     */
    Map<Object,Object> getEqualComparator();
    
    
}
