/*
 *  Copyright 2012 Hippo.
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
package org.hippoecm.frontend.service.restproxy.custom;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.hippoecm.frontend.service.restproxy.test.utils.AnnotationsCollector.collect;

import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.Version;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.module.SimpleModule;
import org.codehaus.jackson.type.TypeReference;
import org.hippoecm.frontend.service.restproxy.test.annotations.AllInOneAnnotation;
import org.hippoecm.frontend.service.restproxy.test.annotations.Color;
import org.hippoecm.frontend.service.restproxy.test.annotations.DocumentLink;
import org.hippoecm.frontend.service.restproxy.test.annotations.DropDownList;
import org.hippoecm.frontend.service.restproxy.test.annotations.FieldGroup;
import org.hippoecm.frontend.service.restproxy.test.annotations.FieldGroupList;
import org.hippoecm.frontend.service.restproxy.test.annotations.ImageSetPath;
import org.hippoecm.frontend.service.restproxy.test.annotations.JcrPath;
import org.hippoecm.frontend.service.restproxy.test.annotations.NoAttributesAnnotation;
import org.hippoecm.frontend.service.restproxy.test.annotations.ParametersInfo;
import org.hippoecm.frontend.service.restproxy.test.channels.AllInOneChannelInfo;
import org.hippoecm.frontend.service.restproxy.test.channels.MobileInfo;
import org.hippoecm.frontend.service.restproxy.test.channels.NoAttributesAnnotationChannelInfo;
import org.hippoecm.frontend.service.restproxy.test.channels.WebsiteInfo;
import org.junit.Before;
import org.junit.Test;

/**
 * A test class for testing {@link AnnotationJsonDeserializer}
 */
public class AnnotationJsonDeserializerTest {

    private static final List<Class<? extends Annotation>> annotations;
    private ObjectMapper objectMapper;

    static {
        annotations = new ArrayList<Class<? extends Annotation>>(10);
        annotations.add(AllInOneAnnotation.class);
        annotations.add(Color.class);
        annotations.add(DocumentLink.class);
        annotations.add(DropDownList.class);
        annotations.add(FieldGroup.class);
        annotations.add(FieldGroupList.class);
        annotations.add(ImageSetPath.class);
        annotations.add(JcrPath.class);
        annotations.add(NoAttributesAnnotation.class);
        annotations.add(ParametersInfo.class);
    }

    @Before
    public void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.enableDefaultTyping();
        objectMapper.enableDefaultTypingAsProperty(ObjectMapper.DefaultTyping.OBJECT_AND_NON_CONCRETE, "@class");
        SimpleModule cmsRestJacksonJsonModule = new SimpleModule("CmsRestJacksonJsonModule", Version.unknownVersion());

        cmsRestJacksonJsonModule.addDeserializer(Annotation.class, new AnnotationJsonDeserializer());
        objectMapper.registerModule(cmsRestJacksonJsonModule);
    }

    @Test
    public void testAllInOneChannelInfo() throws JsonParseException, JsonMappingException, IOException {
        TypeReference<List<? extends Annotation>> typeRef = new TypeReference<List<? extends Annotation>>() {
        };

        List<? extends Annotation> expected = objectMapper.readValue(
                new InputStreamReader(Thread.currentThread().getContextClassLoader()
                        .getResourceAsStream("jackson-json-serialized-annotations/allinonechannelinfo.json")), typeRef);

        List<? extends Annotation> actual = collect(AllInOneChannelInfo.class,
                AnnotationJsonDeserializerTest.annotations);

        assertNotNull(expected);
        assertNotNull(actual);
        AnnotationJsonDeserializerTest.assertEquivalentAllInOneAnnotations((AllInOneAnnotation) expected.get(0),
                (AllInOneAnnotation) actual.get(0));

    }

    @Test
    public void testMobileInfo() throws JsonParseException, JsonMappingException, IOException {
        runIt(MobileInfo.class, "jackson-json-serialized-annotations/mobileinfo.json");
    }

    @Test
    public void testNoAttributesAnnotationChannelInfo() throws JsonParseException, JsonMappingException, IOException {
        runIt(NoAttributesAnnotationChannelInfo.class,
                "jackson-json-serialized-annotations/noattributesannotationchannelinfo.json");
    }

    @Test
    public void testWebsiteInfo() throws JsonParseException, JsonMappingException, IOException {
        runIt(WebsiteInfo.class, "jackson-json-serialized-annotations/websiteinfo.json");
    }

    protected void runIt(final Class<?> clazz, final String resourcePath) throws JsonParseException,
            JsonMappingException, IOException {

        TypeReference<List<? extends Annotation>> typeRef = new TypeReference<List<? extends Annotation>>() {
        };

        List<? extends Annotation> expected = objectMapper.readValue(new InputStreamReader(Thread.currentThread()
                .getContextClassLoader().getResourceAsStream(resourcePath)), typeRef);

        List<? extends Annotation> actual = collect(clazz, AnnotationJsonDeserializerTest.annotations);
        assertNotNull(expected);
        assertNotNull(actual);
        assertEquals(actual, expected);
    }

    private static void assertEquivalentAllInOneAnnotations(AllInOneAnnotation expected, AllInOneAnnotation actual) {
        assertEquals(expected.byteValue(), actual.byteValue());
        assertEquals(expected.shortValue(), actual.shortValue());
        assertEquals(expected.intValule(), actual.intValule());
        assertEquals(expected.longValue(), actual.longValue());
        assertEquals(expected.floatValue(), actual.floatValue());
        assertEquals(expected.maxFloatValue(), actual.maxFloatValue());
        assertEquals(expected.minFloatValue(), actual.minFloatValue());
        assertEquals(expected.minNormalFloatValue(), actual.minNormalFloatValue());
        assertEquals(expected.doubleValue(), actual.doubleValue());
        assertEquals(expected.maxDoubleValue(), actual.maxDoubleValue());
        assertEquals(expected.minDoubleValue(), actual.minDoubleValue());
        assertEquals(expected.minNormalDoubleValue(), actual.minNormalDoubleValue());
        assertEquals(expected.trueValue(), actual.trueValue());
        assertEquals(expected.falseValue(), actual.falseValue());
        assertEquals(expected.charValue(), actual.charValue());
        assertEquals(expected.stringValue(), actual.stringValue());
        assertTrue(Arrays.equals(expected.byteArrayValule(), actual.byteArrayValule()));
        assertTrue(Arrays.equals(expected.shartArrayValue(), actual.shartArrayValue()));
        assertTrue(Arrays.equals(expected.intArrayValue(), actual.intArrayValue()));
        assertTrue(Arrays.equals(expected.longArrayValue(), actual.longArrayValue()));
        assertTrue(Arrays.equals(expected.floatArrayValue(), actual.floatArrayValue()));
        assertTrue(Arrays.equals(expected.doubleArrayValue(), actual.doubleArrayValue()));
        assertTrue(Arrays.equals(expected.booleanArrayValue(), actual.booleanArrayValue()));
        assertTrue(Arrays.equals(expected.charArrayValue(), actual.charArrayValue()));
        assertTrue(Arrays.equals(expected.strinArrayValue(), actual.strinArrayValue()));
    }

}
