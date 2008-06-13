/*
 * Copyright 2008 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.frontend.sa.template.config;

import java.util.LinkedList;
import java.util.List;

import org.hippoecm.frontend.plugins.standardworkflow.types.ITypeStore;
import org.hippoecm.frontend.plugins.standardworkflow.types.TypeDescriptor;

public class MixedTypeStore implements ITypeStore {
    private static final long serialVersionUID = 1L;

    private List<ITypeStore> configs;

    public MixedTypeStore(List<ITypeStore> configs) {
        this.configs = configs;
    }

    public TypeDescriptor getTypeDescriptor(String name) {
        for (ITypeStore config : configs) {
            TypeDescriptor descriptor = config.getTypeDescriptor(name);
            if (descriptor != null) {
                return descriptor;
            }
        }
        return null;
    }

    public List<TypeDescriptor> getTypes(String namespace) {
        List<TypeDescriptor> types = new LinkedList<TypeDescriptor>();
        for (ITypeStore config : configs) {
            List<TypeDescriptor> configTypes = config.getTypes(namespace);
            if (configTypes != null) {
                types.addAll(configTypes);
            }
        }
        return types;
    }
}
