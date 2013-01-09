/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.upgrade;

import java.io.PrintStream;
import java.util.Map;
import java.util.Set;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

class JsonOutput extends AbstractOutput {
    
    JsonOutput() {
    }
    
    JsonOutput(PrintStream stream) {
        super(stream);
    }
    
    @Override
    void render(Item item) {
        JSONObject root = new JSONObject();
        JSONObject child = new JSONObject();
        render(item, child);
        root.put(item.name, child);
        out.println(root.toString(CanonicalSv.INDENT.length()));
    }

    void render(Item item, JSONObject jo) {
        if (item.containsKey("jcr:primaryType")) {
            jo.put("@jcr:primaryType", item.get("jcr:primaryType")[0].getValue());
        }

        for (Map.Entry<String, Value[]> entry : item.entrySet()) {
            if ("jcr:primaryType".equals(entry.getKey())) {
                continue;
            }
            Value[] values = entry.getValue();
            if (values.length != 1) {
                JSONArray array = new JSONArray();
                for (int i = 0; i < values.length; i++) {
                    array.add(values[i].getValue());
                }
                jo.put("@" + entry.getKey(), array);
            } else {
                jo.put("@" + entry.getKey(), entry.getValue()[0].getValue());
            }
        }
        for (Map.Entry<String, Set<Item>> entry : item.children.entrySet()) {
            if (entry.getValue().size() > 1) {
                JSONArray array = new JSONArray();
                for (Item child : entry.getValue()) {
                    JSONObject childObject = new JSONObject();
                    render(child, childObject);
                    array.add(childObject);
                }
                jo.put(entry.getKey(), array);
            } else {
                JSONObject childObject = new JSONObject();
                render(entry.getValue().iterator().next(), childObject);
                jo.put(entry.getKey(), childObject);
            }
        }
    }

}
