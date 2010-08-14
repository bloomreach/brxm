/*
 *  Copyright 2010 Hippo.
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

import static org.junit.Assert.assertEquals;
import net.sf.json.JSONObject;

import org.hippoecm.upgrade.Value.ValueType;
import org.junit.Test;

public class JsonOutputTest {

    static Value[] newString(String val) {
        return new Value[] { new Value(val, ValueType.UNKNOWN) };
    }
    
    @Test
    public void testRendering() {
        Item root = new Item("root");
        root.put("jcr:primaryType", newString("my:type"));
        root.put("test", newString("test"));
        root.put("multi", new Value[] { new Value("aap", ValueType.UNKNOWN), new Value("noot", ValueType.UNKNOWN) });
        root.put("aaa", newString("first property"));
        root.put("bool", new Value[] {new Value("true", ValueType.BOOLEAN)});
        root.put("double", new Value[] {new Value("-12.3", ValueType.DOUBLE)});
        root.put("long", new Value[] {new Value("12", ValueType.LONG)});

        Item child = new Item("child");
        child.put("jcr:primaryType", newString("my:child"));
        child.put("prop", newString("value"));
        root.add(child);

        JsonOutput jsonout = new JsonOutput();
        JSONObject jo = new JSONObject();
        jsonout.render(root, jo);
        
        String str = jo.toString(2);
        assertEquals(
                "{\n" +
                "  \"@jcr:primaryType\": \"my:type\",\n" +
                "  \"@aaa\": \"first property\",\n" +
                "  \"@bool\": true,\n" +
                "  \"@double\": -12.3,\n" +
                "  \"@long\": 12,\n" +
                "  \"@multi\":   [\n" +
                "    \"aap\",\n" +
                "    \"noot\"\n" +
                "  ],\n" +
                "  \"@test\": \"test\",\n" +
                "  \"child\":   {\n" + 
                "    \"@jcr:primaryType\": \"my:child\",\n" + 
                "    \"@prop\": \"value\"\n" + 
                "  }\n" +
                "}", str);
    }
}
