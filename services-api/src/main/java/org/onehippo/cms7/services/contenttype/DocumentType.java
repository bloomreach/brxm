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

import java.util.Map;
import java.util.Set;

/**
 * An immutable Hippo Document Type representation providing a more relaxed and enhanced form of an {@link EffectiveNodeType}
 * exposing only non-residual and non-duplicate named {@link DocumentTypeField} elements (of both Child Node and Property type) and with additional meta-data describing these Fields.
 * <p>
 * A DocumentType is always backed by an underlying EffectiveNodeType, and for all EffectiveNodeTypes an DocumentType is provided.
 * EffectiveNodeTypes which do not have a corresponding DocumentType pre-defined, a DocumentType definition is automatically derived, see {@link #isDerivedType()}.
 * </p>
 * <p>
 * Some pre-defined DocumentTypes may represent an aggregate ({@link #isAggregate()}) which means that the DocumentType also combines one or more mixin types, besides possibly having superTypes as well.
 * This can happen when a pre-defined DocumentType is enhanced with one or more mixins after its initial definition, as well as its underlying JCR NodeType definition was created,
 * Currently the Jackrabbit JCR repository doesn't support adding extra mixins to an existing NodeType definition, thus for DocumentTypes these must be aggregated separately.
 * </p>
 * <p>
 * DocumentType definitions representing existing the EffectiveNodeType of an JCR Node instance can also be an aggregate if the Node instance has additional mixins besides its primary NodeType.
 * </p>
 */
public interface DocumentType {

    /**
     * @return The immutable version of the DocumentTypes instance used to create this definition
     */
    long version();

    /**
     * @return True if there is no DocumentType definition backing this type but it only and fully is derived from the underlying EffectiveNodeType
     */
    boolean isDerivedType();

    /**
     * @return True if this is an aggregation of multiple DocumentTypes like through a combination of DocumentTypes or JCR Node Type mixins
     */
    boolean isAggregate();

    /**
     * @return the immutable and aggregated or effective JCR Repository {@link javax.jcr.nodetype.NodeType} representation
     * which underlies this DocumentType definition
     */
    EffectiveNodeType getEffectiveNodeType();

    /**
     * @return the Qualified name of the Document Type (see also JCR-2.0 3.2.5.2); or the list of aggregated Document Type names as [<name>,...] if {@link #isAggregate()}
     * @see javax.jcr.nodetype.NodeTypeDefinition#getName()
     */
    String getName();

    /**
     * @return The namespace prefix as used by this Document Type (derived from its name); or null if {@link #isAggregate()}
     */
    String getPrefix();

    /**
     * @return The natural ordered set of names of all directly or inherited parent DocumentType or JCR NodeTypes for this DocumentType.
     * Never null but may be empty.
     * @see javax.jcr.nodetype.NodeType#getSupertypes()
     */
    Set<String> getSuperTypes();

    /**
     * @return The natural ordered set of aggregated DocumentTypes or JCR NodeTypes mixins, at least containing {@link #getName()} even if not {@link #isAggregate()}
     */
    Set<String> getAggregatedTypes();

    /**
     * @param documentTypeName The name of a document type
     * @return True if the name of this document type or any of its {@link #getSuperTypes()}  is equal to documentTypeName
     * @see javax.jcr.nodetype.NodeType#isNodeType(String)
     */
    boolean isDocumentType(String documentTypeName);

    /**
     * @return True if this DocumentType can only be used as compound within another DocumentType
     */
    boolean isCompound();

    /**
     * @return True if this DocumentType can only be used to define a Mixin on another DocumentType
     * @see javax.jcr.nodetype.NodeTypeDefinition#isMixin()
     */
    boolean isMixin();

    /**
     * @return True if this DocumentType serves (only) as super type and/or as template for a new DocumentType (inheriting its characteristics)
     */
    // TODO: check if this actually matches the purpose
    boolean isTemplate();

    // TODO
    boolean isCascadeValidate();

    /**
     * @return The aggregated map of DocumentFields, keyed by their field name.
     * @see javax.jcr.nodetype.NodeType#getChildNodeDefinitions()
     */
    Map<String, DocumentTypeField> getFields();
}
