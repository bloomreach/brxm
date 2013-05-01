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
 * Represents a {@link DocumentType} field element which can denote a NodeType Child or Property Definition.
 * A DocumentTypeField name is always explicit, so no residual naming allowed.
 * A DocumentTypeField name is unique within a DocumentType, so mixing single/multi-value properties and/or Child nodes with the same name are not allowed.
 * The underlying EffectiveNodeTypeItem (Child or Property) however may be (and typically is) a residual (relaxed) property or child node.
 * @see javax.jcr.nodetype.ItemDefinition
 */
public interface DocumentTypeField {

    /**
     * @return The qualified field name, never "*" (residual)
     * @see javax.jcr.nodetype.ItemDefinition#getName()
     */
    String getName();

    /**
     * @return The DocumentType name which defines this field, which not necessarily is the same as the containing DocumentType.
     * @see javax.jcr.nodetype.ItemDefinition#getDeclaringNodeType()
     */
    String getDefiningType();

    /**
     * @return True if this field denotes a NodeType Property, false if denotes a NodeType Child
     * @see #getEffectiveNodeTypeItem()
     */
    boolean isPropertyField();

    /**
     * @return True if there is no DocumentTypeField definition backing this field but it only and fully is derived from the underlying EffectiveNodeType Child or Property
     */
    boolean isDerivedField();

    /**
     * Returns the type of this field, which may be a qualified name of a DocumentType (Child) or an (enhanced variant) of a NodeType property type name.
     *
     * For a {@link #isDerivedField()} the value from {@link #getItemType} will be returned.
     *
     * @return The type of this field, which may be a qualified Child DocumentType or an (enhanced variant) of a NodeType property
     */
    String getFieldType();

    /**
     * @return The underlying {@link EffectiveNodeTypeItem#getType) for this field.
     */
    String getItemType();

    /**
     * @return The underlying EffectiveNodeTypeItem (Child or Property) for this field
     */
    EffectiveNodeTypeItem getEffectiveNodeTypeItem();

    /**
     * @return True if this fields denotes the primary item for the underlying EffectiveNodeType
     */
    // TODO: is this (still) used?
    boolean isPrimaryField();

    /**
     * @return True for a multi-valued Property or same-name-sibling allowing Child definition
     * @see javax.jcr.nodetype.PropertyDefinition#isMultiple()
     * @see javax.jcr.nodetype.NodeDefinition#allowsSameNameSiblings()
     */
    boolean isMultiple();

    /**
     * @return True if this field is required
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
     * @return True if this field {@link #isMultiple()} and supports ordering of its Property values or its same-name-sibling Child items
     * Note: while multi-value properties are ordered by definition, this method provides editor support for ordering prototype values or child items (if any)
     */
    boolean isOrdered();

    /**
     * @return The list of validators for this field, may be empty but never null.
     */
    List<String> getValidators();

    /**
     * @return The immutable map of field type specific properties, keyed on property name. The property values may be empty, single or multi-valued but never null.
     * The returned map itself may be empty but never null.
     */
    Map<String, List<String>> getFieldProperties();
}
