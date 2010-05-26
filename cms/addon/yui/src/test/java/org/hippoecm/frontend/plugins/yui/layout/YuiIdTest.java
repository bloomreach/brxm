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
package org.hippoecm.frontend.plugins.yui.layout;

import static org.junit.Assert.assertEquals;
import net.sf.json.JSONObject;
import net.sf.json.JsonConfig;

import org.junit.Test;

public class YuiIdTest {

    public static class TestSetting {

        public YuiId getId() {
            YuiId id = new YuiId("child");
            id.setParentId("parent");
            return id;
        }
    }

    @Test
    public void testSerialization() {
        JsonConfig jsonConfig = new JsonConfig();
        jsonConfig.registerJsonValueProcessor(YuiId.class, new YuiIdProcessor());

        String result = JSONObject.fromObject(new TestSetting(), jsonConfig).toString();
        assertEquals("{\"id\":\"parent:child\"}", result);
    }
}
