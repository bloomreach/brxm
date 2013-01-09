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
package org.hippoecm.frontend.types;

import java.util.List;
import java.util.Map;

import org.apache.wicket.IClusterable;
import org.hippoecm.frontend.model.event.IObservable;

/**
 * The description of a (JCR) type.  The type system is based on the JCR one, but enriched with
 * additional information for the CMS interface.
 * <p>
 * One refinement over the JCR type system is the use of 'pseudo-types'.  These types do not directly
 * correspond to JCR types, but instead use a different type for their storage (the 'real' type).
 * This allows additional semantics to be provided on top of simpler storage, and is usually also represented
 * differently in the document editor.
 * <p>
 * The mutator methods / setters can only be used in a type editing context such as the document type editor.
 * In other cases, the type should be considered immutable and the mutator methods may not be invoked.
 */
public interface ITypeDescriptor extends IClusterable, IObservable {

    /**
     * The name of the type.  It can be used to retrieve the type from a type store.
     * 
     * @return name of the type
     */
    String getName();

    /**
     * The name of the underlying type can be different if this is a "pseudo" type.
     * This can be used to impose additional conditions on the original type, or to
     * associate different templates with the type.
     * <p>
     * For type descriptors that do not correspond to current or draft versions of a
     * namespace, the prefix will be the JCR prefix.  (E.g. myproject_0_0 when version
     * 0.1 is current)
     * 
     * @return the name of the real (JCR) type
     */
    String getType();

    /**
     * The super types of the type.  The type inherits fields from these types and the
     * primary item, if it is defined in any of them.  If the type is primitive, null
     * will be returned.
     * 
     * @return an immutable list of super types.
     */
    List<String> getSuperTypes();

    /**
     * Retrieve all types that descend from the type.
     * 
     * @return an immutable list of sub types.
     */
    List<ITypeDescriptor> getSubTypes();

    /**
     * The map of fields that are declared in this type of any of its super types.
     * If the type is primitive, null will be returned.
     * 
     * @return an immutable list of fields in the type
     */
    Map<String, IFieldDescriptor> getFields();

    /**
     * The map of fields that are declared in this type.  This does not include the
     * fields that are declared in any of the super types.
     * If the type is primitive, null will be returned.
     * 
     * @return the list of fields declared in the type
     */
    Map<String, IFieldDescriptor> getDeclaredFields();

    /**
     * Retrieve the field associated with a key.
     * 
     * @param key
     * @return the field descriptor
     */
    IFieldDescriptor getField(String key);

    /**
     * Is the type a compound or mixin type, corresponding to a node type.  False
     * for the primitive types or any of their pseudo variants. 
     * 
     * @return whether the type corresponds to a node type
     */
    boolean isNode();

    /**
     * Does the type correspond to a mixin, i.e. can it be added dynamically to a Node.
     *
     * @return whether the type corresponds to a mixin node type
     */
    boolean isMixin();

    /**
     * Returns true if this type is <code>typeName</code>
     * or a subtype of <code>typeName</code>, otherwise returns
     * <code>false</code>.
     * 
     * @param typeName the name of a node type.
     * @return a boolean
     */
    boolean isType(String typeName);

    /**
     * Returns true if validation is cascaded, i.e. whether fields with this type
     * are automatically validated.  When false, the field needs the "required"
     * validator to get the field value validated.
     * 
     * @return whether fields of the type are automatically validated
     */
    boolean isValidationCascaded();

    /**
     * Set the super types of the type.  If the type is a mixin type, then all of the
     * super types must be mixin types too.  This should not include nt:base for node
     * types.
     *
     * @param superTypes the list of super types
     */
    void setSuperTypes(List<String> superTypes);

    /**
     * Add a field to the type.
     *
     * @param descriptor the field that is added to the type
     */
    void addField(IFieldDescriptor descriptor) throws TypeException;

    /**
     * Remove a field from the type.
     *
     * @param name the name of the field that is removed
     */
    void removeField(String name) throws TypeException;

    /**
     * Declare one of the fields to be the primary item.  This is only valid when
     * the field has been declared in the type, not in any of its super types.
     * Additionally, none of the super types may have defined a primary item.
     *
     * @param name
     */
    void setPrimary(String name);

    void setIsNode(boolean isNode);

    void setIsMixin(boolean isMixin);

    /**
     * @param isCascaded are fields of this type always validated
     */
    void setIsValidationCascaded(boolean isCascaded);
}
