/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ObjectNode;

import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.easymock.PowerMock.replayAll;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.management.*")
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
    public void prettyStringForEmptyObject() throws JsonProcessingException {
        assertEquals("{ }", Json.prettyString(object));
    }

    @Test
    public void prettyStringForNullThrowsException() throws JsonProcessingException {
        assertEquals("null", Json.prettyString(null));
    }

    @Test
    public void prettyStringForObject() throws IOException {
        assertEquals("{\n"
                        + "  value : 1,\n"
                        + "  child : {\n"
                        + "    nestedValue1 : \"text\",\n"
                        + "    nestedValue2 : true\n"
                        + "  }\n"
                        + "}",
                Json.prettyString(Json.object("{ value: 1, child: { nestedValue1: 'text', nestedValue2: true } }"))
        );
    }

    @Test
    @PrepareForTest({Json.class})
    public void prettyStringFallsBackToUglyStringOnError() throws Exception {
        final ObjectWriter prettyPrinter = createMock(ObjectWriter.class);

        Whitebox.setInternalState(Json.class, "prettyPrinter", prettyPrinter);

        expect(prettyPrinter.writeValueAsString(anyString())).andThrow(createMock(JsonProcessingException.class));
        replayAll(prettyPrinter);

        assertEquals("{}", Json.prettyString(Json.object()));

        verify(prettyPrinter);
    }
}
