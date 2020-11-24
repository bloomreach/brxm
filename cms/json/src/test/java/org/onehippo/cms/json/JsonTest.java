/*
 * Copyright 2017-2020 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.cms.json;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.PowerMockRunner;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"org.apache.logging.log4j.*", "javax.management.*"})
public class JsonTest {

    private ObjectNode object;

    @Before
    public void setUp() throws IOException {
        object = Json.object();
    }

    @Test
    public void createEmptyObject() {
        assertTrue(object.isObject());
        assertEquals(0, object.size());
    }

    @Test
    public void createObjectFromValidJson() throws IOException {
        ObjectNode object = Json.object("{ a: 1 } ");
        assertTrue(object.isObject());
        assertEquals(1, object.get("a").asInt());
    }

    @Test(expected = IOException.class)
    public void createObjectFromWrongJson() throws IOException {
        Json.object("[ a: 1 ] ");
    }

    @Test(expected = IOException.class)
    public void createObjectFromInvalidJson() throws IOException {
        Json.object("{ invalid json");
    }

    @Test
    public void createObjectFromEmptyString() throws IOException {
        ObjectNode object = Json.object("");
        assertTrue(object.isObject());
        assertEquals(0, object.size());
    }

    @Test
    public void createObjectFromBlankString() throws IOException {
        ObjectNode object = Json.object("    ");
        assertTrue(object.isObject());
        assertEquals(0, object.size());
    }

    @Test
    public void writeValueAsStringForEmptyObject() throws JsonProcessingException {
        assertEquals("{ }", Json.getMapper().writerWithDefaultPrettyPrinter().writeValueAsString(object));
    }

    @Test
    public void writeValueAsStringForNullThrowsException() throws JsonProcessingException {
        assertEquals("null", Json.getMapper().writeValueAsString(null));
    }

    @Test
    public void writeValueAsStringForObject() throws IOException {
        assertEquals("{\n"
                        + "  value : 1,\n"
                        + "  child : {\n"
                        + "    nestedValue1 : \"text\",\n"
                        + "    nestedValue2 : true\n"
                        + "  }\n"
                        + "}",
                Json.getMapper().writerWithDefaultPrettyPrinter().writeValueAsString(Json.object("{ value: 1, child: { nestedValue1: 'text', nestedValue2: true } }"))
        );
    }

}
