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
package org.hippoecm.hst.platform.configuration.channel;

import java.util.HashMap;
import java.util.Map;

import org.hippoecm.hst.configuration.channel.ChannelInfo;
import org.hippoecm.hst.core.parameters.Parameter;
import org.hippoecm.hst.platform.configuration.channel.ChannelUtils;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ChannelUtilsTest {

    static interface TestInfo extends ChannelInfo {
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

        ChannelInfo info1 = ChannelUtils.getChannelInfo(values, null, AnalyticsChannelInfoMixin.class,
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

    static interface ConflictableInfo1 extends ChannelInfo{
        @Parameter(name = "foo")
        String getFoo();
    }

    static interface ConflictableInfo2 extends ChannelInfo{
        @Parameter(name = "foo")
        String getFoo();
    }

    @Test
    public void testWhenMixinHavingSameMethodForSameProperty() throws Exception {
        Map<String, Object> values = new HashMap<String, Object>();
        values.put("foo", "bar");

        ConflictableInfo1 info1 = ChannelUtils.getChannelInfo(values, ConflictableInfo1.class, ConflictableInfo2.class);
        ConflictableInfo2 info2 = (ConflictableInfo2) info1;

        assertEquals("bar", info1.getFoo());
        assertEquals("bar", info2.getFoo());
    }

    static interface ConflictableInfo3 extends ChannelInfo{
        @Parameter(name = "foo")
        String getFoo1();
    }

    static interface ConflictableInfo4 extends ChannelInfo{
        @Parameter(name = "foo")
        String getFoo2();
    }

    @Test
    public void testWhenMixinHavingDifferentMethodsForSameProperty() throws Exception {
        Map<String, Object> values = new HashMap<String, Object>();
        values.put("foo", "bar");

        ConflictableInfo3 info3 = ChannelUtils.getChannelInfo(values, ConflictableInfo3.class, ConflictableInfo4.class);
        ConflictableInfo4 info4 = (ConflictableInfo4) info3;

        assertEquals("bar", info3.getFoo1());
        assertEquals("bar", info4.getFoo2());
    }

    static interface ConflictableInfo5 extends ChannelInfo{
        @Parameter(name = "foo1")
        String getFoo();
    }

    static interface ConflictableInfo6 extends ChannelInfo{
        @Parameter(name = "foo2")
        String getFoo();
    }

    @Test
    public void testWhenMixinHavingSameMethodForDifferentProperties() throws Exception {
        Map<String, Object> values = new HashMap<String, Object>();
        values.put("foo1", "bar1");
        values.put("foo2", "bar2");

        ConflictableInfo5 info5 = ChannelUtils.getChannelInfo(values, ConflictableInfo5.class, ConflictableInfo6.class);
        ConflictableInfo6 info6 = (ConflictableInfo6) info5;

        assertEquals("bar1", info5.getFoo());

        // NOTE: As two channel info have the same getter operation, the operation with the same name of the second
        // internface is hidden.
        assertEquals("bar1", info6.getFoo());
    }

    static interface ConflictableInfo7 extends ChannelInfo{
        @Parameter(name = "foo")
        String getFoo();
    }

    static interface ConflictableInfo8 extends ChannelInfo{
        @Parameter(name = "foo")
        Integer getFoo();
    }

    @Test
    public void testWhenMixinHavingSameMethodButDifferentReturnTypeForSameProperty() throws Exception {
        Map<String, Object> values = new HashMap<String, Object>();
        values.put("foo", "100");

        try {
            ConflictableInfo7 info7 = ChannelUtils.getChannelInfo(values, ConflictableInfo7.class, ConflictableInfo8.class);
            fail("Should fail because of methods with same signature getFoo() but incompatible return types:  [class java.lang.String, class java.lang.Integer]");
        } catch (IllegalArgumentException ignore) {
        }
    }
}
