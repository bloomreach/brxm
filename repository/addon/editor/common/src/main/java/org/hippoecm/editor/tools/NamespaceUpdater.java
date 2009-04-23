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
import java.util.Map;

import org.apache.wicket.util.collections.MiniMap;

public final class NamespaceUpdater {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    @SuppressWarnings("unchecked")
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
