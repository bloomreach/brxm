/*
 *  Copyright 2009 Hippo.
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

import org.apache.wicket.model.IDetachable;
import org.apache.wicket.util.string.Strings;
import org.hippoecm.frontend.types.IFieldDescriptor;

/**
 * Path to a value that maintains references to {@link IFieldDescriptor}s.
 */
public final class ModelPath implements IDetachable {

    private static final long serialVersionUID = 1L;

    private ModelPathElement[] elements;

    public ModelPath(ModelPathElement[] elements) {
        this.elements = elements;
    }
    
    public ModelPathElement[] getElements() {
        return elements;
    }

    @Override
    public String toString() {
        String[] strings = new String[elements.length];
        for (int i = 0; i < elements.length; i++) {
            strings[i] = elements[i].toString();
        }
        return Strings.join("/", strings);
    }

    public void detach() {
        for (ModelPathElement element : elements) {
            element.detach();
        }
    }
    
}
