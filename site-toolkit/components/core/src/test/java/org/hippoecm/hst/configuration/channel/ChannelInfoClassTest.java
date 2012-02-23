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

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hippoecm.hst.core.parameters.Color;
import org.hippoecm.hst.core.parameters.HstValueType;
import org.hippoecm.hst.core.parameters.Parameter;
import org.junit.Test;

public class ChannelInfoClassTest {

    static interface TestInfo extends ChannelInfo {
        @Parameter(name = "test-name", required = true)
        String getTestName();
        
    }
    
    @Test
    public void requiredParameterIsFound() {
        int numberOfParameterAnnotationsOnChannelInfo = ChannelInfoClassProcessor.getProperties(ChannelInfo.class).size();
        List<HstPropertyDefinition> properties = ChannelInfoClassProcessor.getProperties(TestInfo.class);
        assertEquals(1 + numberOfParameterAnnotationsOnChannelInfo, properties.size());
        
        // make sure that "test-name" is amongst the three properties
        HstPropertyDefinition propDef = getPropertyDefinition("test-name", properties);
        assertNotNull(propDef);
        assertEquals(true, propDef.isRequired());
        assertEquals(HstValueType.STRING, propDef.getValueType());
            
    }

    @Test
    public void proxyProvidesCorrectValues() {
        List<HstPropertyDefinition> properties = ChannelInfoClassProcessor.getProperties(TestInfo.class);
        Map<String, Object> values = new HashMap<String, Object>();
        values.put("test-name", "aap");

        TestInfo info = ChannelUtils.getChannelInfo(values, TestInfo.class);
        assertEquals("aap", info.getTestName());
    }

    public static interface ExtendedTestInfo extends ChannelInfo {
        @Parameter(name = "color")
        @Color
        String getColor();
    }

    @Test
    public void additionalAnnotationsAreProvided() {
        int numberOfParameterAnnotationsOnChannelInfo = ChannelInfoClassProcessor.getProperties(ChannelInfo.class).size();
        List<HstPropertyDefinition> properties = ChannelInfoClassProcessor.getProperties(ExtendedTestInfo.class);
        assertEquals(1 + numberOfParameterAnnotationsOnChannelInfo, properties.size());

        
        HstPropertyDefinition hpd = getPropertyDefinition("color", properties);
        assertEquals("color", hpd.getName());
        assertEquals(HstValueType.STRING, hpd.getValueType());

        List<Annotation> annotations = hpd.getAnnotations();
        // only the @Color annotation should be present. The @Parameter annotation is not part of the HstPropertyDefinition#annotations
        assertEquals(1, annotations.size());
        assertEquals(Color.class, annotations.get(0).annotationType());
    }
    
    
    private HstPropertyDefinition getPropertyDefinition(String propertyName, List<HstPropertyDefinition> properties) {
        for(HstPropertyDefinition propDef : properties) {
            if (propDef.getName().equals(propertyName)) {
                return propDef;
            }
        }
        return null;
    }
}
