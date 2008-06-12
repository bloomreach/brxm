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
package org.hippoecm.frontend.sa.plugins.standardworkflow.export;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hippoecm.frontend.sa.plugins.standardworkflow.types.ITypeStore;
import org.hippoecm.frontend.sa.plugins.standardworkflow.types.TypeDescriptor;
import org.hippoecm.repository.standardworkflow.RemodelWorkflow.TypeUpdate;

public class NamespaceUpdater {

    private ITypeStore currentConfig;
    private ITypeStore draftConfig;

    public NamespaceUpdater(ITypeStore current, ITypeStore draft) {
        currentConfig = current;
        draftConfig = draft;
    }

    public Map<String, TypeUpdate> getUpdate(String namespace) {
        Map<String, TypeUpdate> result = new HashMap<String, TypeUpdate>();

        List<TypeDescriptor> list = draftConfig.getTypes(namespace);
        for (TypeDescriptor descriptor : list) {
            if (descriptor.isNode()) {
                String type = descriptor.getType();
                if (type.indexOf(':') > 0) {
                    String prefix = type.substring(0, type.indexOf(':'));
                    if (namespace.equals(prefix)) {
                        TypeDescriptor current = currentConfig.getTypeDescriptor(type);
                        if (current != null) {
                            TypeDescriptor draft = draftConfig.getTypeDescriptor(type);

                            TypeUpdate update = new TypeConversion(currentConfig, draftConfig, current, draft);
                            result.put(type, update);
                        }
                    }
                }
            }
        }
        return result;
    }
}
