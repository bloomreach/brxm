/**
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *         http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.resourcebundle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.junit.Test;

/**
 * TestDefaultResourceBundleRegistry
 */
public class TestMapResourceBundle {

    @Test
    public void testMapBundle() throws Exception {
        Map<String, String> map = new HashMap<String, String>();
        map.put("msg1", "Hello");
        map.put("msg2", "World");

        ResourceBundle bundle = new MapResourceBundle(map);
        assertEquals("Hello", bundle.getString("msg1"));
        assertEquals("World", bundle.getString("msg2"));

        try {
            bundle.getString("unknown.key");
            fail("MissingResourceException should have occurred for unknown key.");
        } catch (MissingResourceException e) {
            // as expected
        }
    }

}
