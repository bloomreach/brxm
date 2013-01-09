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

import org.hippoecm.frontend.model.ocm.IStore;

/**
 * A type store makes type descriptors available.  Based on type names, descriptors are retrieved
 * from persistent or transient storage.
 */
public interface ITypeStore extends IStore<ITypeDescriptor> {

    /**
     * Retrieve the type descriptor of a particular name
     *
     * @param name the name of the type
     *
     * @return the type descriptor
     */
    ITypeDescriptor getTypeDescriptor(String name);

    /**
     * Retrieve all types in a namespace.  The namespace is identified by a prefix.
     *
     * @param namespace the prefix for the namespace
     *
     * @return the list of type descriptors in the specified namespace
     */
    List<ITypeDescriptor> getTypes(String namespace);
}
