/*
 *  Copyright 2011 Hippo.
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

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hippoecm.hst.core.parameters.Color;
import org.hippoecm.hst.core.parameters.HstValueType;
import org.hippoecm.hst.core.parameters.Parameter;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;

public class ChannelInfoClassTest {

    static interface TestInfo extends ChannelInfo {
        @Parameter(name = "test-name", required = true)
        String getTestName();
    }

    @Test
    public void requiredParameterIsFound() {
        List<HstPropertyDefinition> properties = ChannelInfoClassProcessor.getProperties(TestInfo.class);
        assertEquals(1, properties.size());
        HstPropertyDefinition definition = properties.get(0);
        assertEquals("test-name", definition.getName());
        assertEquals(true, definition.isRequired());
        assertEquals(HstValueType.STRING, definition.getValueType());
    }

    @Test
    public void proxyProvidesCorrectValues() {
        List<HstPropertyDefinition> properties = ChannelInfoClassProcessor.getProperties(TestInfo.class);
        Map<String, Object> values = new HashMap<String, Object>();
        values.put(properties.get(0).getName(), "aap");

        TestInfo info = (TestInfo) ChannelUtils.getChannelInfo(values, TestInfo.class);
        assertEquals("aap", info.getTestName());
    }

    public static interface ExtendedTestInfo extends ChannelInfo {
        @Parameter(name = "color")
        @Color
        String getColor();
    }

    @Test
    public void additionalAnnotationsAreProvided() {
        List<HstPropertyDefinition> definitions = ChannelInfoClassProcessor.getProperties(ExtendedTestInfo.class);
        assertEquals(1, definitions.size());

        HstPropertyDefinition hpd = definitions.get(0);
        assertEquals("color", hpd.getName());
        assertEquals(HstValueType.STRING, hpd.getValueType());

        List<Annotation> annotations = hpd.getAnnotations();
        assertEquals(1, annotations.size());
        assertEquals(Color.class, annotations.get(0).annotationType());
    }
}
