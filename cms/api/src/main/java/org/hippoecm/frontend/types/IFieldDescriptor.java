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

import java.util.Set;

import org.apache.wicket.IClusterable;
import org.hippoecm.frontend.model.event.IObservable;

/**
 * The field descriptor contains the meta information of a field in a type.
 */
public interface IFieldDescriptor extends IClusterable, IObservable {

    /**
     * Symbolic name of the field.
     *
     * @return the name
     */
    String getName();

    /**
     * The descriptor of the type of the field.
     *
     * @return the type
     */
    ITypeDescriptor getTypeDescriptor();

    /**
     * The (JCR) path of the field.  This will correspond to the name of the child node or the property.
     *
     * @return the path
     */
    String getPath();

    /**
     * Can multiple instances of this field be created.
     *
     * @return true when multiple instances can be created
     */
    boolean isMultiple();

    /**
     * Will an instance of the field be created automatically for a new instance of the containing type.
     *
     * @return will an instance of the field be created
     */
    boolean isAutoCreated();

    /**
     * Is the field protected, i.e. can it not be set using the (JCR) api, but is it managed by the system itself.
     *
     * @return true when the field is protected
     */
    boolean isProtected();

    /**
     * When the field is mandatory, it must be present for the (JCR) session to be in a valid state.
     * Note that the use of mandatory fields is discouraged, as the presence of an invalid mandatory field on
     * one node can prevent a wholly different node from being persisted.
     *
     * @return whether the field is mandatory
     */
    boolean isMandatory();

    /**
     * When multiple instances of the field can be present, can they be reordered.  I.e. do instances behave like a
     * set or a list.
     *
     * @return are instances of the field ordered
     */
    boolean isOrdered();

    /**
     * Is this field the primary field of the containing type.  Only one field can be the primary field.
     *
     * @return whether this field is the primary field
     */
    boolean isPrimary();

    /**
     * For residual field definitions (name is '*'), the excluded names consist of the list of the paths for all
     * <strong>other</other> fields in the type.
     *
     * @return the excluded names
     */
    Set<String> getExcluded();

    /**
     * The symbolic names for validators associated with this field.
     *
     * @return the names of applicable validators
     */
    Set<String> getValidators();

    // The following methods are only valid in a writable field descriptor.
    // I.e. outside of the context of the document type editor, they may not be invoked.

    void setPath(String path) throws TypeException;

    void setMultiple(boolean multiple);

    void setAutoCreated(boolean autocreated);

    void setMandatory(boolean mandatory);

    void setOrdered(boolean isOrdered);

    void addValidator(String validator);

    void setExcluded(Set<String> set);

    void removeValidator(String validator);

}
