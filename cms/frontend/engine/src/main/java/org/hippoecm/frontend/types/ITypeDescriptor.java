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
package org.hippoecm.frontend.types;

import java.util.List;
import java.util.Map;

import javax.jcr.Value;

import org.apache.wicket.IClusterable;
import org.hippoecm.frontend.model.event.IObservable;

public interface ITypeDescriptor extends IClusterable, IObservable {
    final static String SVN_ID = "$Id$";

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
     * Set the super types of the type.  If the type is a mixin type, then all of the
     * super types must be mixin types too.  This should not include nt:base for node
     * types.
     * @param superTypes the list of super types
     */
    void setSuperTypes(List<String> superTypes);

    /**
     * The map of fields that are declared in this type of any of its super types.
     * If the type is primitive, null will be returned.
     * @return an immutable list of fields in the type
     */
    Map<String, IFieldDescriptor> getFields();

    /**
     * The map of fields that are declared in this type.  This does not include the
     * fields that are declared in any of the super types.
     * If the type is primitive, null will be returned.
     * @return the list of fields declared in the type
     */
    Map<String, IFieldDescriptor> getDeclaredFields();

    /**
     * Retrieve the field associated with a key.
     * @param key
     * @return the field descriptor
     */
    IFieldDescriptor getField(String key);

    /**
     * Add a field to the type.
     * @param descriptor the field that is added to the type
     */
    void addField(IFieldDescriptor descriptor);

    /**
     * Remove a field from the type.
     * @param name the name of the field that is removed
     */
    void removeField(String name);

    /**
     * Declare one of the fields to be the primary item.  This is only valid when
     * the field has been declared in the type, not in any of its super types.
     * Additionally, none of the super types may have defined a primary item.
     * @param name
     */
    void setPrimary(String name);

    /**
     * Is the type a compound or mixin type, corresponding to a node type.  False
     * for the primitive types or any of their pseudo variants. 
     * @return whether the type corresponds to a node type
     */
    boolean isNode();

    void setIsNode(boolean isNode);

    boolean isMixin();

    void setIsMixin(boolean isMixin);

    /**
     * Returns true if this type is <code>typeName</code>
     * or a subtype of <code>typeName</code>, otherwise returns
     * <code>false</code>.
     * @param typeName the name of a node type.
     * @return a boolean
     */
    boolean isType(String typeName);

    Value createValue();
    
}
