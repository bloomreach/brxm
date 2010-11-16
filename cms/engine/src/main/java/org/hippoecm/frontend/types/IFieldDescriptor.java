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

public interface IFieldDescriptor extends IClusterable, IObservable {
    final static String SVN_ID = "$Id$";

    String getName();

    ITypeDescriptor getTypeDescriptor();
    
    String getPath();

    void setPath(String path) throws TypeException;

    void setMultiple(boolean multiple);

    boolean isMultiple();

    boolean isAutoCreated();

    void setAutoCreated(boolean autocreated);

    boolean isProtected();

    boolean isMandatory();

    void setMandatory(boolean mandatory);

    boolean isOrdered();

    void setOrdered(boolean isOrdered);

    boolean isPrimary();

    Set<String> getExcluded();

    void setExcluded(Set<String> set);

    Set<String> getValidators();

    void addValidator(String validator);

    void removeValidator(String validator);

}
