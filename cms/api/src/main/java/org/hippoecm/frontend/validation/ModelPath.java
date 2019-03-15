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

import java.util.Arrays;
import java.util.stream.Collectors;

import org.apache.wicket.model.IDetachable;
import org.hippoecm.frontend.types.IFieldDescriptor;

/**
 * Path to a value that maintains references to {@link IFieldDescriptor}s.
 */
public final class ModelPath implements IDetachable {

    private final ModelPathElement[] elements;

    public ModelPath(final ModelPathElement[] elements) {
        this.elements = elements;
    }

    public ModelPathElement[] getElements() {
        return elements;
    }

    @Override
    public String toString() {
        return Arrays.stream(elements)
                .map(ModelPathElement::toString)
                .collect(Collectors.joining("/"));
    }

    public void detach() {
        Arrays.stream(elements).forEach(ModelPathElement::detach);
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        return Arrays.equals(this.getElements(), ((ModelPath) other).getElements());
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(getElements());
    }
}
