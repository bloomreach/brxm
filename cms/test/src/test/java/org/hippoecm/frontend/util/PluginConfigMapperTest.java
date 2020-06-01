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
package org.hippoecm.frontend.util;

import static org.junit.Assert.assertEquals;

import org.hippoecm.frontend.plugin.config.impl.JavaPluginConfig;
import org.junit.Test;

public class PluginConfigMapperTest {

    @Test
    public void testEmpty() throws MappingException {
        JavaPluginConfig jpc = new JavaPluginConfig();
        MappingTestBean bean = new MappingTestBean();
        PluginConfigMapper.populate(bean, jpc);

        assertEquals(0, bean.getTestInt());
        assertEquals(false, bean.isTestBool());
        assertEquals(null, bean.getTestString());
    }

    @Test
    public void testValues() throws MappingException {
        JavaPluginConfig jpc = new JavaPluginConfig();
        jpc.put("test.int", 1);
        jpc.put("test.bool", true);
        jpc.put("test.string", "value");

        MappingTestBean bean = new MappingTestBean();
        PluginConfigMapper.populate(bean, jpc);

        assertEquals(1, bean.getTestInt());
        assertEquals(true, bean.isTestBool());
        assertEquals("value", bean.getTestString());
    }

    @Test
    public void testStringValues() throws MappingException {
        JavaPluginConfig jpc = new JavaPluginConfig();
        jpc.put("test.int", "1");
        jpc.put("test.bool", "true");
        jpc.put("test.string", "value");

        MappingTestBean bean = new MappingTestBean();
        PluginConfigMapper.populate(bean, jpc);

        assertEquals(1, bean.getTestInt());
        assertEquals(true, bean.isTestBool());
        assertEquals("value", bean.getTestString());
    }

}
