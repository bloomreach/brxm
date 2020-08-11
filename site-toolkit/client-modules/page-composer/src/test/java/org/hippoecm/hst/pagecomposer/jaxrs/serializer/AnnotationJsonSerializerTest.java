/*
 *  Copyright 2012-2018 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.hst.pagecomposer.jaxrs.serializer;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import org.hippoecm.hst.configuration.channel.ChannelInfo;
import org.hippoecm.hst.configuration.channel.HstPropertyDefinition;
import org.hippoecm.hst.pagecomposer.jaxrs.serializer.channels.AllInOneChannelInfo;
import org.hippoecm.hst.pagecomposer.jaxrs.serializer.channels.MobileInfo;
import org.hippoecm.hst.pagecomposer.jaxrs.serializer.channels.NoAttributesAnnotationChannelInfo;
import org.hippoecm.hst.pagecomposer.jaxrs.serializer.channels.WebsiteInfo;
import org.hippoecm.hst.pagecomposer.jaxrs.serializer.util.JsonTreeAnnotationsComparator;
import org.hippoecm.hst.platform.configuration.channel.ChannelInfoClassProcessor;
import org.junit.Before;
import org.junit.Test;


/**
 * Test class for the {@link AnnotationJsonSerializer}
 */
public class AnnotationJsonSerializerTest {
    
    private ObjectMapper cmsRestJacksonObjectMapper;

    @Before
    public void setUp() {
        final AnnotationJsonSerializer annotationJsonSerializer = new AnnotationJsonSerializer(Annotation.class); 
        // Unknown version is just used here for test in production code the version is injected based on the value
        // of the POM project version
        final SimpleModule cmsRestJacksonJsonModule = new SimpleModule("CmsRestJacksonJsonModule", Version.unknownVersion());
        cmsRestJacksonJsonModule.addSerializer(annotationJsonSerializer);
        cmsRestJacksonObjectMapper = new ObjectMapper();
        cmsRestJacksonObjectMapper.enableDefaultTyping();
        cmsRestJacksonObjectMapper.enableDefaultTypingAsProperty(ObjectMapper.DefaultTyping.OBJECT_AND_NON_CONCRETE, AnnotationJsonSerializer.TYPE_ATTRIBUTE);
        cmsRestJacksonObjectMapper.registerModule(cmsRestJacksonJsonModule);
    }

    @Test
    public void testBasicAnnotationSerializationWithWebsiteInfo() throws JsonGenerationException, JsonMappingException,
            IOException, CouldNotFindHstPropertyDefinition, SecurityException, IllegalArgumentException,
            IllegalAccessException, InvocationTargetException {

        runIt(WebsiteInfo.class);
    }

    @Test
    public void testBasicAnnotationSerializationWithMobileInfo() throws JsonGenerationException, JsonMappingException,
            IOException, CouldNotFindHstPropertyDefinition, SecurityException, IllegalArgumentException,
            IllegalAccessException, InvocationTargetException {

        runIt(MobileInfo.class);
    }

    @Test
    public void testNoAttributesAnnotationChannelInfo() throws JsonGenerationException, JsonMappingException, IOException,
            CouldNotFindHstPropertyDefinition, SecurityException, IllegalArgumentException, IllegalAccessException,
            InvocationTargetException {

        runIt(NoAttributesAnnotationChannelInfo.class);
    }

    @Test
    public void testAllInOneChannelInfo() throws JsonGenerationException, JsonMappingException, IOException,
            CouldNotFindHstPropertyDefinition, SecurityException, IllegalArgumentException, IllegalAccessException,
            InvocationTargetException {
        
        runIt(AllInOneChannelInfo.class);
    }
    
    protected void runIt(Class<? extends ChannelInfo> channelInfoClass) throws JsonGenerationException,
            JsonMappingException, IOException, CouldNotFindHstPropertyDefinition, SecurityException,
            IllegalArgumentException, IllegalAccessException, InvocationTargetException {

        final StringWriter serializedJson = new StringWriter();
        final List<HstPropertyDefinition> hstPropDefs = ChannelInfoClassProcessor
                .getProperties(channelInfoClass);

        cmsRestJacksonObjectMapper.writeValue(serializedJson, hstPropDefs);

        // Define a simple object mapper and use to deserialize the JSON serialization of HstPropertyDefinition list
        // just serialized in the previous step
        final ObjectMapper simpleMapper = new ObjectMapper();
        final JsonNode jsonTree = simpleMapper.readValue(new StringReader(serializedJson.getBuffer().toString()),
                JsonNode.class);

        JsonTreeAnnotationsComparator.assertEquivalentHstPropertyDefinitions(jsonTree, hstPropDefs);
    }

}
