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
import javax.jcr.RepositoryException;

import org.hippoecm.hst.content.beans.NodeAware;
import org.hippoecm.hst.content.beans.manager.ObjectConverterAware;
import org.hippoecm.hst.provider.jcr.JCRValueProvider;
import org.hippoecm.repository.api.HippoNode;

public interface HippoBean extends IdentifiableContentBean, NodeAware, ObjectConverterAware, Comparable<HippoBean> {

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
     * This returns the localized node name of the backing jcr node for this bean. If it is a {@link HippoNode} is returns {@link HippoNode#getLocalizedName()}, 
     * otherwise {@link Node#getName()}
     * 
     * @return the localized node name of the backing jcr node and <code>null</code> when some {@link RepositoryException} happens
     */
    String getLocalizedName();
    
    /**
     * Returns the jcr uuid of the backing canonical (physical) jcr node or <code>null</code> when 
     * <p>
     * <ul>
     *  <li>there is no canonical node (for example this might be the case for faceted navigation folders)</li>
     *  <li>the jcr node is detached</li>
     *  <li>a repository exception happened</li>
     * </ul>
     * </p>
     * <p>
     * For {@link HippoDocumentBean}'s, the uuid of the handle might be more valuable, which you can get with {@link HippoDocumentBean#getCanonicalHandleUUID()}.
     * </p> 
     * @see HippoDocumentBean#getCanonicalHandleUUID()
     * @return the jcr uuid of the backing canonical (physical) jcr node or <code>null</code> 
     */
    String getCanonicalUUID();
    
    /**
     * Returns the jcr path of the backing canonical (physical) jcr node or <code>null</code> when 
     * <p>
     * <ul>
     *  <li>there is no canonical node (for example this might be the case for faceted navigation folders)</li>
     *  <li>the jcr node is detached</li>
     *  <li>a repository exception happened</li>
     * </ul>
     * </p>
     * <p>
     * For {@link HippoDocumentBean}'s, the uuid of the handle might be more valuable, which you can get with {@link HippoDocumentBean#getCanonicalHandleUUID()}.
     * </p> 
     * @see HippoDocumentBean#getCanonicalHandlePath()
     * @return the jcr path of the backing canonical (physical) jcr node or <code>null</code> 
     */
    String getCanonicalPath();

    /**
     * Same as {@link #getProperty(String)}, where getProperty is only there for having a nice .getProperty['propname'] in jsp expression language
     */
    Map<String, Object> getProperties();

    /**
     * Return types can be of type String, String[], Boolean, Boolean[], Long, Long[], Double, Double[] or Calendar, Calendar[]
     * @param <T>
      * @param name the name of the property
     * @return The value of the property and <code>null</code> if it does not exist. The return type is either String, String[], Boolean, Boolean[], Long, Long[], Double, Double[] or Calendar, Calendar[]
     */
    <T> T getProperty(String name);
    
    /**
     * If the property does not exist, return the <code>defaultValue</code>. If the property does exist, the same value as {@link #getProperty(String)} will be returned
     * Return types can be of type String, String[], Boolean, Boolean[], Long, Long[], Double, Double[] or Calendar, Calendar[]
     * @param <T>
     * @param name the name of the property
     * @return The value of the property and <code>defaultValue</code> if it does not exist. The return type is either String, String[], Boolean, Boolean[], Long, Long[], Double, Double[] or Calendar, Calendar[]
     * 
     */
    <T> T getProperty(String name, T defaultValue);

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
     * @See {@link #getBean(String)}. Now, only if a bean found of type <code>beanMappingClass</code>, it is returned, and otherwise <code>null</code> is returned.
     * @param <T>
     * @param relPath a path that does not start with a "/"
     * @param beanMappingClass the class {@link T} that the child bean must be off
     * @return returns the <code>HippoBean</code> of (sub)type beanMappingClass with relative path <code>relPath</code> to this bean, or <code>null</code> when it does not exist, is not of (sub)type beanMappingClass, or when the relPath is not a valid relative path
     */
    <T extends HippoBean> T getBean(String relPath, Class<T> beanMappingClass);
      
    /**
     * <p>
     * Returns all the child beans as a {@link List} with elements of type {@link T}. When a child bean
     * is found that is not of type <code>beanMappingClass</code>, it is skipped
     * </p>
     * <p>
     * If you want all child beans that can be mapped to a {@link HippoBean}, just call 
     * <code>List<HippoBean> beans = getBeans(HippoBean.class);</code>
     * </p>
     * @param <T> the return type of the child bean
     * @param beanMappingClass the class {@link T} that the child beans must be off
     * @return List<HippoBean> where the backing jcr nodes have the name childNodeName
     */
    <T extends HippoBean> List<T> getChildBeans(Class<T> beanMappingClass);
    
    /**
     * Returns all the child beans of name <code>childNodeName</code> as a {@link List} with elements of type {@link T}. When a found bean is not of type {@link T} a
     * {@link ClassCastException} is thrown. 
     * @param <T> the return type of the child bean
     * @param childNodeName
     * @return List<HippoBean> where the backing jcr nodes have the name childNodeName
     * @throws ClassCastException 
     */
    <T> List<T> getChildBeansByName(String childNodeName) throws ClassCastException;
    
    /**
     * <p>
     * Returns all the child beans of name <code>childNodeName</code> as a {@link List} with elements of type {@link T}. When a child bean
     * is found that is not of type <code>beanMappingClass</code>, it is skipped
     * </p>
     * <p>If the <code>beanMappingClass</code> is <code>null</code>, it is ignored. Then, this method returns the same a {@link #getChildBeansByName(String)} and
     * can throw a {@link ClassCastException}
     * </p>
     * @param <T> the return type of the child bean
     * @param childNodeName
     * @param beanMappingClass the class {@link T} that the child beans must be off. 
     * @return List<HippoBean> where the backing jcr nodes have the name childNodeName
     */
    <T extends HippoBean> List<T> getChildBeansByName(String childNodeName, Class<T> beanMappingClass);
    
   
    /**
     * Returns all the child beans of this bean, where the backing jcr node primary node type equals jcrPrimaryNodeType.
     * If a jcr child node is of primary nodetype 'hippo:handle', we look whether the underlying 'Document' has the corresponding 
     * jcrPrimaryNodeType. If so, we return the bean for this 'Document'.
     * 
     * @param <T> the return type of the {@link List} elements
     * @param jcrPrimaryNodeType the primary type the child beans should be off
     * @return List<HippoBean> where the backing jcr nodes are of type jcrPrimaryNodeType
     */
    <T> List<T> getChildBeans(String jcrPrimaryNodeType);
    
    /**
     * This method returns the <code>HippoBean</code> linked by <code>relPath</code> of type beanMappingClass, or <code>null</code> if no bean found or not of (sub)type beanMappingClass.
     * 
     * Only a bean can be returned if, and only if, the bean at <code>relPath</code> is a bean of type {@link HippoMirrorBean} (thus either a hippo:mirror or
     * hippo:facetselect). If a mirror bean is found, and the mirror points to a bean of (sub)type <code>beanMappingClass</code>, then, this bean is returned. In all other cases,
     * <code>null</code> is returned
     * 
     * @param <T>
     * @param relPath (path not starting with a "/")
     * @param beanMappingClass
     * @return The linked <code>HippoBean</code> of (sub)type beanMappingClass where the link has relative path <code>relPath</code> to this bean, or <code>null</code> when it does not exist, is not of (sub)type beanMappingClass, or when the relPath is not a valid relative path
     */
    <T extends HippoBean> T getLinkedBean(String relPath, Class<T> beanMappingClass);
    
    /**
     * This method returns all the <code>HippoBean</code>'s linked by <code>relPath</code> of type beanMappingClass as a List, or an <code>Empty</code> list if no bean found or not of (sub)type beanMappingClass.
     * 
     * if the relPath is something like: foo/bar/my:links, then, all first the node foo/bar is fetched, and then all beans are returned that have
     * name 'my:links'
     * 
     * @see {@link #getBean(String, Class)}
     * @param <T>
     * @param relPath (path not starting with a "/")
     * @param beanMappingClass
     * @return
     */
    <T extends HippoBean> List<T> getLinkedBeans(String relPath, Class<T> beanMappingClass);
  
    
    /**
     * Returns the parent bean wrt this bean. Note that this does not automatically imply
     * a bean with the parent jcr node of this bean. When the parent node is of type "hippo:handle",
     * the parent of the handle must be taken
     * @return the parent bean wrt this bean, or if this bean backing jcr node is null or object converter cannot create a bean for the parent, return <code>null</code>
     */
    HippoBean getParentBean();
    
    
    /**
     * Expert: Returns the 'real' contextual (preview / live context) bean version of this bean. Most of the time, this is just the current bean. However, 
     * when the current bean is below some parent bean because it was mirrored by this parent, then, this method returns
     * the 'real' contextual version, where the {@link #getParentBean()} also returns the contextualized version of the physical parent
     * 
     * <b>note: this is quite an expensive check </b>
     * @return the contextual bean for this bean, or <code>null</code> if it fails to contextualize this bean 
     */
    HippoBean getContextualBean();
    
    /**
     * Expert: Returns the parent bean in the context of live/preview.
     * @see {@link #getParentBean()}, only this method returns the 'real' contextual parent bean. Suppose I have some HippoBean (= myBean), that 
     * I got through a mirror, in other, words, the HippoBean is below the document (=docA) that had the mirror (link). {@link #getParentBean()} will
     * return <code>docA</code>, but this is not the 'real' contextual parent bean of <code>myBean</code>. The 'real' contextual parent can be 
     * fetched through this method. Note, that when <code>myBean</code> was not the result of a mirror, that {@link #getParentBean()} will then return the 
     * same bean
     * 
     * <b>note: this is quite an expensive check </b>
     * @return the 'unmirrored' parent bean in wrt this bean, but still in context, or if this bean backing jcr node is null or if the object converter cannot create a bean for the parent, return <code>null</code>
     */
    HippoBean getContextualParentBean();
    
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
     * In general, only a {@link HippoDocumentBean} and {@link HippoFolderBean} can have a {@link HippoAvailableTranslationsBean}. However, to make sure that on
     * any {@link HippoBean} you can call {@link #getAvailableTranslationsBean()}, we add it to the base {@link HippoBean} as well. If the bean is not a document or folder, this method
     * will return a no-operation {@link HippoAvailableTranslationsBean} instance.
     * @return A {@link HippoAvailableTranslationsBean}. If there are no translations for this {@link HippoBean}, a no-operation {@link HippoAvailableTranslationsBean} will be returned 
     */
    <T extends HippoBean> HippoAvailableTranslationsBean<T> getAvailableTranslationsBean();
    
    /**
     * A convenience method capable of comparing two HippoBean instances for you for the underlying jcr node. 
     * 
     * When the nodes being compared have the same canonical node (physical equivalence) this method returns true.
     * If there is no canonical node the virtual jcr path is used to compare the items: if these paths are the same, true is returned
     * 
     * <b>Note: this method compares the jcr path of the backing canonical jcr nodes. The {@link #equals(Object)} tests
     * the jcr node path, which might be a virtual path. So this method can return true while {@link #equals(Object)} returns false</b>
     * 
     * @param compare the object to compare to
     * @return <code>true</code> if the object compared has the same canonical node or the same virtual path when they don't have a canonical
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
     * returns true when mydocument and otherdocument have the same canonical node
     * If there is no canonical node the virtual jcr path is used to compare the items: if these paths are the same, true is returned
     * 
     * <b>Note: this method compares the jcr path of the backing canonical jcr nodes. The {@link #equals(Object)} tests
     * the jcr node path, which might be a virtual path. So this method can return true while {@link #equals(Object)} returns false</b>
     * 
     * @return a ComparatorMap in which you can compare HippoBean's via the get(Object o)
     */
    Map<Object,Object> getEqualComparator();
    
    
}
