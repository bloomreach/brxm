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
package org.hippoecm.frontend.plugins.standardworkflow.export;

import java.util.HashMap;
import java.util.Map;

import org.hippoecm.frontend.legacy.template.FieldDescriptor;
import org.hippoecm.frontend.legacy.template.TypeDescriptor;
import org.hippoecm.frontend.legacy.template.config.TypeConfig;
import org.hippoecm.repository.standardworkflow.RemodelWorkflow.FieldIdentifier;
import org.hippoecm.repository.standardworkflow.RemodelWorkflow.TypeUpdate;

public class TypeConversion extends TypeUpdate {
    private static final long serialVersionUID = 1L;

    private Map<FieldIdentifier, FieldIdentifier> fields;

    public TypeConversion(TypeConfig currentConfig, TypeConfig draftConfig, TypeDescriptor current, TypeDescriptor draft) {
        if (draft != null) {
            newName = draft.getName();
        } else {
            newName = current.getName();
        }

        fields = new HashMap<FieldIdentifier, FieldIdentifier>();
        for (Map.Entry<String, FieldDescriptor> entry : current.getFields().entrySet()) {
            FieldDescriptor origField = entry.getValue();
            TypeDescriptor descriptor = currentConfig.getTypeDescriptor(origField.getType());
            if (descriptor.isNode()) {
                continue;
            }

            FieldIdentifier oldId = new FieldIdentifier();
            oldId.path = origField.getPath();
            oldId.type = currentConfig.getTypeDescriptor(origField.getType()).getType();

            if (draft != null) {
                FieldDescriptor newField = draft.getField(entry.getKey());
                if (newField != null) {
                    FieldIdentifier newId = new FieldIdentifier();
                    newId.path = newField.getPath();
                    TypeDescriptor newType = draftConfig.getTypeDescriptor(newField.getType());
                    if (newType == null) {
                        // FIXME: test namespace prefix before resorting to the current config.
                        newType = currentConfig.getTypeDescriptor(newField.getType());
                    }
                    newId.type = newType.getType();

                    fields.put(oldId, newId);
                }
            } else {
                fields.put(oldId, oldId);
            }
        }
    }

    public Map<FieldIdentifier, FieldIdentifier> getRenames() {
        return fields;
    }
}
