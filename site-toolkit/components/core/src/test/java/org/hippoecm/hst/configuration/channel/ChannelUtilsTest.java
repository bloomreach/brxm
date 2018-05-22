/*
 *  Copyright 2011-2018 Hippo B.V. (http://www.onehippo.com)
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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ChannelUtilsTest {

    static interface TestInfo extends ChannelInfo{
        @Parameter(name = "test-name")
        String getName();
    }

    static interface AnalyticsChannelInfoMixin extends ChannelInfo {

        @Parameter(name = "analyticsEnabled")
        Boolean isAnalyticsEnabled();

        @Parameter(name = "scriptlet")
        String getScriptlet();

    }

    static interface CategorizingChannelInfoMixin extends ChannelInfo {

        @Parameter(name = "categorizationEnabled")
        Boolean isCategorizationEnabled();

        @Parameter(name = "categories")
        String getCategories();

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

    @Test
    public void testProxyWithMixins() {
        Map<String, Object> values = new HashMap<String, Object>();
        values.put("test-name", "aap");
        values.put("analyticsEnabled", true);
        values.put("scriptlet", "(function() {})();");
        values.put("categorizationEnabled", true);
        values.put("categories", "foo,bar");

        TestInfo info1 = ChannelUtils.getChannelInfo(values, TestInfo.class, AnalyticsChannelInfoMixin.class,
                CategorizingChannelInfoMixin.class);
        assertEquals("aap", info1.getName());

        // cast it to a mixin type
        assertTrue(info1 instanceof AnalyticsChannelInfoMixin);
        AnalyticsChannelInfoMixin info2 = (AnalyticsChannelInfoMixin) info1;
        assertTrue(info2.isAnalyticsEnabled());
        assertEquals("(function() {})();", info2.getScriptlet());

        // or getChannelInfo directly to a mixin type
        info2 = ChannelUtils.getChannelInfo(values, TestInfo.class, AnalyticsChannelInfoMixin.class,
                CategorizingChannelInfoMixin.class);
        assertTrue(info2.isAnalyticsEnabled());
        assertEquals("(function() {})();", info2.getScriptlet());

        // cast it to a mixin type
        assertTrue(info1 instanceof CategorizingChannelInfoMixin);
        CategorizingChannelInfoMixin info3 = (CategorizingChannelInfoMixin) info1;
        assertTrue(info3.isCategorizationEnabled());
        assertEquals("foo,bar", info3.getCategories());

        // or getChannelInfo directly to a mixin type
        info3 = ChannelUtils.getChannelInfo(values, TestInfo.class, AnalyticsChannelInfoMixin.class,
                CategorizingChannelInfoMixin.class);
        assertTrue(info3.isCategorizationEnabled());
        assertEquals("foo,bar", info3.getCategories());

        Map<String, Object> properties = info1.getProperties();
        assertEquals(properties, values);

        try {
            properties.put("test-name", "noot");
            fail("properties should not be mutable");
        } catch (UnsupportedOperationException uoe) {
            // expected
        }
    }

    @Test
    public void testProxyOnlyWithMixins() {
        Map<String, Object> values = new HashMap<String, Object>();
        values.put("test-name", "aap");
        values.put("analyticsEnabled", true);
        values.put("scriptlet", "(function() {})();");
        values.put("categorizationEnabled", true);
        values.put("categories", "foo,bar");

        Object info1 = ChannelUtils.getChannelInfo(values, null, AnalyticsChannelInfoMixin.class,
                CategorizingChannelInfoMixin.class);

        assertFalse(info1 instanceof TestInfo);

        // cast it to a mixin type
        assertTrue(info1 instanceof AnalyticsChannelInfoMixin);
        AnalyticsChannelInfoMixin info2 = (AnalyticsChannelInfoMixin) info1;
        assertTrue(info2.isAnalyticsEnabled());
        assertEquals("(function() {})();", info2.getScriptlet());

        // cast it to a mixin type
        assertTrue(info1 instanceof CategorizingChannelInfoMixin);
        CategorizingChannelInfoMixin info3 = (CategorizingChannelInfoMixin) info1;
        assertTrue(info3.isCategorizationEnabled());
        assertEquals("foo,bar", info3.getCategories());
    }
}
