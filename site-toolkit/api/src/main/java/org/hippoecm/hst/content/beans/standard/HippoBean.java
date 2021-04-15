/*
 *  Copyright 2008-2021 Hippo B.V. (http://www.onehippo.com)
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

import org.hippoecm.hst.content.annotations.PageModelIgnore;
import org.hippoecm.hst.content.beans.NodeAware;
import org.hippoecm.hst.content.beans.manager.ObjectConverterAware;
import org.hippoecm.hst.provider.jcr.JCRValueProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface HippoBean extends IdentifiableContentBean, NodeAware, ObjectConverterAware, Comparable<HippoBean> {

    Logger log = LoggerFactory.getLogger(HippoBean.class);

    /**
     * This returns the backing jcr node for this bean.
     *
     * @return the backing jcr node for this bean. <code>null</code> if the bean is detached
     */
    @PageModelIgnore
    Node getNode();

    @PageModelIgnore
    JCRValueProvider getValueProvider();

    /**
     * This returns the node name of the backing jcr node for this bean
     *
     * @return the node name of the backing jcr node. This method never returns <code>null</code>
     */
    String getName();

    /**
     * Returns the display name of the backing jcr node for this bean as determined by its {@code hippo:name} property
     * or the node name of the backing jcr node if no such property exists
     *
     * @return the display name of the backing jcr node for this bean as determined by its {@code hippo:name} property
     * or the node name of the backing jcr node if no such proeprty exists
     */
    String getDisplayName();

    /**
     * @return the primary nodetype name of the backing JCR Node and {@code null} if no backing jcr node is found
     */
    default String getContentType() {
        try {
            final Node jcrNode = getValueProvider() == null ? null : getValueProvider().getJcrNode();
            return jcrNode == null ? null : jcrNode.getPrimaryNodeType().getName();
        } catch (RepositoryException e) {
            log.error("Exception while trying to get primary node type name", e);
            return null;
        }
    }

    /**
     * This returns the absolute path of the backing jcr node for this bean, for example /documents/content/myprojec/news/article
     *
     * When the jcr node is virtual, it returns the virtual path.
     *
     * @return the absolute jcr path of the backing jcr node.
     */
    @PageModelIgnore
    String getPath();

    /**
     * Returns the canonical path
     * @return
     */
    @PageModelIgnore
    default String getComparePath() {
        return getPath();
    }

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
    @PageModelIgnore
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
    @PageModelIgnore
    String getCanonicalPath();

    /**
     * Same as {@link #getProperty(String)}, where getProperty is only there for having a nice .getProperty['propname'] in jsp expression language
     */
    @PageModelIgnore
    Map<String, Object> getProperties();

    /**
     * Returns the value of a document field which is marked as <code>single</code>.
     * If the value is an array, then returns the first element.
     * @param <T>
     * @param name the name of the property
     * @return The value of the property or <code>null</code> if it doesn't exist. Return types are String, Boolean, Long, Double or Calendar
     */
    <T> T getSingleProperty(String name);

    /**
     * Returns the value of a document field which is marked as <code>single</code>.
     * If the value is an array, then returns the first element.
     * @param <T>
     * @param name the name of the property
     * @return The value of the property and <code>defaultValue</code> if it doesn't exist. Allowed return types are String, Boolean, Long, Double or Calendar
     * 
     */
    <T> T getSingleProperty(String name, T defaultValue);

    /**
     * Returns the value of a document field which is marked as <code>multiple</code>.
     * If the value is not an array, then returns the value by creating an array.
     * @param <T>
     * @param name the name of the property
     * @return The value of the property and <code>null</code> if it doesn't exist. Allowed return types are String[], Boolean[], Long[], Double[] or Calendar[]
     */
    <T> T[] getMultipleProperty(String name);

    /**
     * Returns the value of a document field which is marked as <code>multiple</code>.
     * If the value is not an array, then returns the value by creating an array.
     * @param <T>
     * @param name the name of the property
     * @return The value of the property and <code>defaultValue</code> if it doesn't exist. Allowed return types are String[], Boolean[], Long[], Double[] or Calendar[]
     * 
     */
    <T> T[] getMultipleProperty(String name, T[] defaultValue);

    /**
     * @return Map of all properties, where the values can be of type String, String[], Boolean, Boolean[], Long, Long[], Double, Double[] or Calendar, Calendar[]
     */
    @PageModelIgnore
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
     * Only if a bean found of type <code>beanMappingClass</code>, it is returned, and otherwise <code>null</code> is returned.
     * @param <T>
     * @param relPath a path that does not start with a "/"
     * @param beanMappingClass the class <code>T</code> that the child bean must inherit from
     * @return returns the <code>HippoBean</code> of (sub)type beanMappingClass with relative path <code>relPath</code>
     * to this bean, or <code>null</code> when it does not exist, is not of (sub)type beanMappingClass, or when the relPath is not a valid relative path
     * @see #getBean(String)
     */
    <T extends HippoBean> T getBean(String relPath, Class<T> beanMappingClass);
      
    /**
     * <p>
     * Returns all the child beans as a {@link List} with elements of type <code>T</code>. When a child bean
     * is found that is not of type <code>beanMappingClass</code>, it is skipped
     * </p>
     * <p>
     * If you want all child beans that can be mapped to a {@link HippoBean}, just call 
     * <code>List<HippoBean> beans = getBeans(HippoBean.class);</code>
     * </p>
     * @param <T> the return type of the child bean
     * @param beanMappingClass the class <code>T</code> that the child beans must inherit from
     * @return List<HippoBean> where the backing jcr nodes have the name childNodeName
     */
    <T extends HippoBean> List<T> getChildBeans(Class<T> beanMappingClass);
    
    /**
     * Returns all the child beans of name <code>childNodeName</code> as a {@link List} with elements of type
     * <code>T</code>. When a found bean is not of type <code>T</code> a {@link ClassCastException} is thrown.
     * @param <T> the return type of the child bean
     * @param childNodeName
     * @return List<HippoBean> where the backing jcr nodes have the name childNodeName
     * @throws ClassCastException 
     */
    <T> List<T> getChildBeansByName(String childNodeName) throws ClassCastException;
    
    /**
     * <p>
     * Returns all the child beans of name <code>childNodeName</code> as a {@link List} with elements of type <code>T</code>. When a child bean
     * is found that is not of type <code>beanMappingClass</code>, it is skipped
     * </p>
     * <p>If the <code>beanMappingClass</code> is <code>null</code>, it is ignored. Then, this method returns the same a {@link #getChildBeansByName(String)} and
     * can throw a {@link ClassCastException}
     * </p>
     * @param <T> the return type of the child bean
     * @param childNodeName
     * @param beanMappingClass the class <code>T</code> that the child beans must inherit from
     * @return List<HippoBean> where the backing jcr nodes have the name childNodeName
     */
    <T extends HippoBean> List<T> getChildBeansByName(String childNodeName, Class<T> beanMappingClass);
    
   
    /**
     * Returns all the child beans of this bean, where the backing jcr node primary node type equals jcrPrimaryNodeType.
     * If a jcr child node is of primary nodetype 'hippo:handle', we look whether the underlying 'Document' has the corresponding 
     * jcrPrimaryNodeType. If so, we return the bean for this 'Document'.
     * 
     * @param <T> the return type of the {@link List} elements
     * @param jcrPrimaryNodeType the primary type the child beans should inherit from
     * @return List<HippoBean> where the backing jcr nodes are of type jcrPrimaryNodeType
     */
    <T> List<T> getChildBeans(String jcrPrimaryNodeType);
    
    /**
     * <p>
     * This method returns the <code>HippoBean</code> linked by <code>relPath</code> of type beanMappingClass. It returns
     * a {@link HippoBean} if and only if
     * <ol>
     *     <li>
     *         <code>relPath</code> points to a node of (sub)type <code>hippo:mirror</code> or
     *         of (sub)type <code>hippo:facetselect</code>
     *     </li>
     *     <li>
     *        the <code>hippo:docbase</code> of the mirror points to an existing node that results in a {@link HippoBean}
     *        of type <code>beanMappingClass</code>
     *     </li>
     * </ol>
     * In all other case, <code>null</code> is returned.
     *
     * @param <T> the expected type of the linked bean
     * @param relPath (path not starting with a "/")
     * @param beanMappingClass the expected class of the linked bean
     * @return The linked <code>HippoBean</code> or <code>null</code>
     */
    <T extends HippoBean> T getLinkedBean(String relPath, Class<T> beanMappingClass);
    
    /**
     * Same as {@link #getLinkedBean(String, Class)} only now all linked beans found at <code>relPath</code> are returned.
     * When no linked beans found, an empty list is returned.
     * @see #getLinkedBean(String, Class)
     */
    <T extends HippoBean> List<T> getLinkedBeans(String relPath, Class<T> beanMappingClass);

    /**
     * @param uuid the uuid of the node for which to get the {@link HippoBean}
     * @param beanMappingClass the expected class of the linked bean
     * @param <T> the type of the returned bean
     * @return The {@link HippoBean} of type <code>T</code> for <code>uuid</code> or <code>null</code> if no node for
     * <code>uuid</code> found or not of correct type
     */
    <T extends HippoBean> T getBeanByUUID(String uuid, Class<T> beanMappingClass);
    
    /**
     * Returns the parent bean wrt this bean. Note that this does not automatically imply
     * a bean with the parent jcr node of this bean. When the parent node is of type "hippo:handle",
     * the parent of the handle must be taken
     * @return the parent bean wrt this bean, or if this bean backing jcr node is null or object converter cannot create a bean for the parent, return <code>null</code>
     */
    @PageModelIgnore
    HippoBean getParentBean();

    /**
     * Returns the canonical version of the current {@link HippoBean} or <code>null</code> in case that the backing {@link Node} is pure virtual, which
     * is the case for example for some faceted navigation nodes. When an exception happens <code>null</code> can be returned
     * @param <T> 
     * @return the canonical version of the current {@link HippoBean} or <code>null</code> in case that the backing {@link Node} is pure virtual or when
     * some exception happened
     */
    @PageModelIgnore
    <T extends HippoBean> T getCanonicalBean();
    
    /**
     * @return <code>true</code> is this HippoBean is an instanceof <code>{@link HippoDocumentBean}</code>
     */
    @PageModelIgnore
    boolean isHippoDocumentBean();
    
    /**
     * @return <code>true</code> is this HippoBean is an instanceof <code>{@link HippoFolderBean}</code>
     */
    @PageModelIgnore
    boolean isHippoFolderBean();
    
    /**
     * @return <code>true</code> when the backing jcr Node has no child nodes
     */
    @PageModelIgnore
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
     * any {@link HippoBean} you can call {@link #getAvailableTranslations()}, we add it to the base {@link HippoBean} as well.
     * @return A {@link HippoAvailableTranslationsBean}.
     */
    @PageModelIgnore
    <T extends HippoBean> HippoAvailableTranslationsBean<T> getAvailableTranslations();
    
    /**
     * A convenience method capable of comparing two HippoBean instances for you for the underlying jcr node. 
     * 
     * When the nodes being compared have the same canonical node (physical equivalence) this method returns true.
     * If there is no canonical node the virtual jcr path is used to compare the items: if these paths are the same, true is returned
     * 
     * <b>Note: this method compares the jcr path of the backing canonical jcr nodes. All implementing classes override
     * {@link Object#equals(Object)} to compare the jcr node path, which might be a virtual path. So this method can
     * return true while {@link Object#equals(Object)} returns false</b>
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
     * <b>Note: this method compares the jcr path of the backing canonical jcr nodes. All implementing classes override
     * {@link Object#equals(Object)} to compare the jcr node path, which might be a virtual path. So this method can
     * return true while {@link Object#equals(Object)} returns false</b>
     * 
     * @return a ComparatorMap in which you can compare HippoBeans via the get(Object o)
     */
    @PageModelIgnore
    Map<Object,Object> getEqualComparator();

    /**
     * @return {@code true} if the backing node from {@link #getNode()} is a versioned node
     */
    @PageModelIgnore
    boolean isVersionedNode();

}
