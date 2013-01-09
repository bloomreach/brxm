/*
 *  Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.configuration.channel;

import java.util.HashMap;
import java.util.Map;

import org.hippoecm.hst.core.parameters.Parameter;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ChannelUtilsTest {

    public static interface TestInfo extends ChannelInfo{
        @Parameter(name = "test-name")
        String getName();
    }

    @Test
    public void testProxy() {
        Map<String, Object> values = new HashMap<String, Object>();
        values.put("test-name", "aap");

        TestInfo info = ChannelUtils.getChannelInfo(values, TestInfo.class);
        assertEquals("aap", info.getName());

        Map<String, Object> properties = info.getProperties();
        assertEquals(properties, values);
        try {
            properties.put("test-name", "noot");
            fail("properties should not be mutable");
        } catch (UnsupportedOperationException uoe) {
            // expected
        }
    }
}
