/*
 *  Copyright 2009-2019 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.validation;

import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.wicket.model.IDetachable;
import org.hippoecm.frontend.types.IFieldDescriptor;

/**
 * Path element in a {@link ModelPath}.
 */
public final class ModelPathElement implements IDetachable {

    private final IFieldDescriptor field;
    private final String name;
    private final int index;

    public ModelPathElement(final IFieldDescriptor field, final String name, final int index) {
        this.field = field;
        this.name = name;
        this.index = index;
    }

    public IFieldDescriptor getField() {
        return field;
    }

    public String getName() {
        return name;
    }

    public int getIndex() {
        return index;
    }

    public void detach() {
        if (field instanceof IDetachable) {
            ((IDetachable) field).detach();
        }
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof ModelPathElement) {
            final ModelPathElement that = (ModelPathElement) obj;
            return that.field.equals(field) && that.name.equals(name) && that.index == index;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(31, 251).append(field).append(name).append(index).toHashCode();
    }

    @Override
    public String toString() {
        return name + "[" + index + "]";
    }

}
