/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cms7.services.contenttype;

import java.util.List;
import java.util.Map;

/**
 * Represents a {@link ContentType} property or child element representing an underlying NodeType Child or Property Definition.
 * <p>
 * A ContentTypeItem name is always explicit, so no residual naming allowed.
 * The underlying EffectiveNodeTypeItem (Child or Property) however may be a residual (relaxed) property or child node.
 * </p>
 * <p>
 * Within a ContentType its ContentTypeItem elements can be accessed by through {@link ContentType#getItem(String)},
 * or as map of property or child elements through {@link ContentType#getProperties()} or {@link ContentType#getChildren()}.
 * </p>
 * <p>
 * For a {@link ContentType#isDerivedType()} both a property and child element may be defined by the same name, in which case only the
 * child element is accessible through the {@link ContentType#getItem(String)} method (see also JCR-2.0 5.1.8).
 * In that case the property element by that name still can be accessed through {@link ContentType#getProperties()}.
 * </p>
 * @see javax.jcr.nodetype.ItemDefinition
 */
public interface ContentTypeItem {

    /**
     * @return The qualified item name, never "*" (residual)
     * @see javax.jcr.nodetype.ItemDefinition#getName()
     */
    String getName();

    /**
     * @return The ContentType name which defines this item, which not necessarily is the same as the containing ContentType.
     * @see javax.jcr.nodetype.ItemDefinition#getDeclaringNodeType()
     */
    String getDefiningType();

    /**
     * @return True if this item denotes a NodeType Property, false if denotes a NodeType Child
     * @see #getEffectiveNodeTypeItem()
     */
    boolean isProperty();

    /**
     * @return True if there is no explicit ContentTypeItem definition backing this item but it only is derived from the underlying EffectiveNodeType Child or Property
     */
    boolean isDerivedItem();

    /**
     * Returns the type of this item, which may be a qualified name of a ContentType (Child) or an (enhanced variant) of a NodeType property type name.
     * <p>
     * For a {@link #isDerivedItem()} the value from {@link #getEffectiveType} will be returned.
     * </p>
     * @return The type of this item, which may be a qualified Child ContentType or an (enhanced variant) of a NodeType property
     */
    String getItemType();

    /**
     * @return The underlying {@link EffectiveNodeTypeItem#getType) for this item; in case of a {@link #isMultiTyped()} returning only the first item definition its type
     */
    String getEffectiveType();

    /**
     * @return The underlying EffectiveNodeTypeItem (Child or Property) for this item; in case of a {@link #isMultiTyped()} returning only the first item definition
     */
    EffectiveNodeTypeItem getEffectiveNodeTypeItem();

    /**
     * @return true if multiple EffectiveNodeTypeItem definition are allowed for this {@link #isDerivedItem()}
     * @see #getMultiTypes()
     */
    boolean isMultiTyped();

    /**
     * @return the list of all the allowed EffectiveNodeTypeItem definitions for this {@link #isDerivedItem()} or an empty list otherwise
     */
    List<EffectiveNodeTypeItem> getMultiTypes();

    /**
     * Primary items are for instance used in imagesets to define the cms preview resource.
     * Since an imageset contains multiple images, one of them needs to be marked as the 'preview' variant.
     * @return True if this item denotes the primary item
     */
    boolean isPrimaryItem();

    /**
     * @return True for a multi-valued Property or same-name-sibling allowing Child definition
     * @see javax.jcr.nodetype.PropertyDefinition#isMultiple()
     * @see javax.jcr.nodetype.NodeDefinition#allowsSameNameSiblings()
     */
    boolean isMultiple();

    /**
     * @return True if this item is required
     * @see javax.jcr.nodetype.ItemDefinition#isMandatory()
     */
    boolean isMandatory();

    /**
     * @return True if this item is autoCreated
     * @see javax.jcr.nodetype.ItemDefinition#isAutoCreated()
     */
    boolean isAutoCreated();

    /**
     * @return True if this item is protected
     * @see javax.jcr.nodetype.ItemDefinition#isProtected()
     */
    boolean isProtected();

    /**
     * @return True if this item {@link #isMultiple()} and supports ordering of its Property values or its same-name-sibling Child items.<br/>
     * Note: while multi-value properties are (JCR) ordered by definition, this method provides editor support for ordering prototype values or child items (if any),
     * and only for this specific child item while the JCR hasOrderableChildren definition on NodeType level concerns all its children.
     */
    boolean isOrdered();

    /**
     * @return The list of validators for this item, may be empty but never null.
     */
    List<String> getValidators();

    /**
     * @return The immutable map of item type specific properties, keyed on property name. The property values may be empty, single or multi-valued but never null.
     * The returned map itself may be empty but never null.
     */
    Map<String, List<String>> getItemProperties();
}
