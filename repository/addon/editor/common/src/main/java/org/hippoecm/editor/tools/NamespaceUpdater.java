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
package org.hippoecm.editor.tools;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.wicket.util.collections.MiniMap;
import org.hippoecm.frontend.types.ITypeDescriptor;
import org.hippoecm.frontend.types.ITypeStore;

public class NamespaceUpdater {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private ITypeStore currentConfig;
    private ITypeStore draftConfig;

    public NamespaceUpdater(ITypeStore current, ITypeStore draft) {
        currentConfig = current;
        draftConfig = draft;
    }

    public Map<String, TypeUpdate> getUpdate(String namespace) {
        Map<String, TypeUpdate> result = new HashMap<String, TypeUpdate>();

        List<ITypeDescriptor> list = draftConfig.getTypes(namespace);
        for (ITypeDescriptor descriptor : list) {
            if (descriptor.isNode()) {
                String type = descriptor.getType();
                if (type.indexOf(':') > 0) {
                    String prefix = type.substring(0, type.indexOf(':'));
                    if (namespace.equals(prefix)) {
                        ITypeDescriptor current = currentConfig.getTypeDescriptor(type);
                        if (current != null) {
                            ITypeDescriptor draft = draftConfig.getTypeDescriptor(type);

                            TypeUpdate update = new TypeConversion(currentConfig, draftConfig, current, draft)
                                    .getTypeUpdate();
                            result.put(type, update);
                        }
                    }
                }
            }
        }
        return result;
    }

    private static Map<String, TypeUpdate> convertCargo(Object cargo) {
        Map<String, TypeUpdate> updates = new HashMap<String, TypeUpdate>();
        for (Map.Entry<String, Object> entry : ((Map<String, Object>) cargo).entrySet()) {
            Map<String, Object> value = (Map<String, Object>) entry.getValue();
            TypeUpdate update = new TypeUpdate();
            update.newName = (String) value.get("newName");
            update.prototype = (String) value.get("prototype");
            update.renames = new HashMap<FieldIdentifier, FieldIdentifier>();

            Map<Map<String, String>, Map<String, String>> origRenames = (Map<Map<String, String>, Map<String, String>>) value
                    .get("renames");
            for (Map.Entry<Map<String, String>, Map<String, String>> rename : origRenames.entrySet()) {
                FieldIdentifier src = new FieldIdentifier();
                src.path = rename.getKey().get("path");
                src.type = rename.getKey().get("type");

                FieldIdentifier dest = new FieldIdentifier();
                dest.path = rename.getValue().get("path");
                dest.type = rename.getValue().get("type");

                update.renames.put(src, dest);
            }

            updates.put(entry.getKey(), update);
        }
        return updates;
    }

    public static Object toCargo(Map<String, TypeUpdate> updates) {
        Map<String, Object> result = new HashMap<String, Object>();
        for (Map.Entry<String, TypeUpdate> entry : updates.entrySet()) {
            Map<String, Object> update = new HashMap<String, Object>();
            update.put("newName", entry.getValue().newName);
            update.put("prototype", entry.getValue().prototype);

            Map<Map<String, String>, Map<String, String>> origRenames = new HashMap<Map<String, String>, Map<String, String>>();
            for (Map.Entry<FieldIdentifier, FieldIdentifier> fieldEntry : entry.getValue().renames.entrySet()) {
                Map<String, String> key = new MiniMap(2);
                key.put("path", fieldEntry.getKey().path);
                key.put("type", fieldEntry.getKey().type);

                Map<String, String> value = new MiniMap(2);
                value.put("path", fieldEntry.getValue().path);
                value.put("type", fieldEntry.getValue().type);

                origRenames.put(key, value);
            }
            update.put("renames", origRenames);

            result.put(entry.getKey(), update);
        }
        return result;
    }
}
