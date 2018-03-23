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
package org.onehippo.cms.channelmanager.content.documenttype.field;

import java.util.Optional;

import com.fasterxml.jackson.databind.node.ObjectNode;

import org.junit.Before;
import org.junit.Test;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class FieldTypeConfigTest {

    private FieldTypeContext context;
    private FieldTypeConfig config;

    @Before
    public void setUp() {
        context = createMock("context", FieldTypeContext.class);
        config = new FieldTypeConfig(context);
    }

    @Test
    public void booleans() {
        expect(context.getBooleanConfig(eq("one"))).andReturn(Optional.of(true));
        expect(context.getBooleanConfig(eq("two"))).andReturn(Optional.of(false));
        expect(context.getBooleanConfig(eq("three"))).andReturn(Optional.empty());
        replay(context);

        ObjectNode json = config.booleans("one", "two", "three").build();

        assertThat(json.size(), equalTo(2));
        assertTrue(json.get("one").isBoolean());
        assertTrue(json.get("two").isBoolean());
        assertThat(json.get("one").booleanValue(), equalTo(true));
        assertThat(json.get("two").booleanValue(), equalTo(false));
    }

    @Test
    public void strings() {
        expect(context.getStringConfig(eq("one"))).andReturn(Optional.of("value"));
        expect(context.getStringConfig(eq("two"))).andReturn(Optional.of(""));
        expect(context.getStringConfig(eq("three"))).andReturn(Optional.empty());
        replay(context);

        ObjectNode json = config.strings("one", "two", "three").build();

        assertThat(json.size(), equalTo(2));
        assertTrue(json.get("one").isTextual());
        assertTrue(json.get("two").isTextual());
        assertThat(json.get("one").textValue(), equalTo("value"));
        assertThat(json.get("two").textValue(), equalTo(""));
    }

    @Test
    public void multipleStrings() {
        expect(context.getMultipleStringConfig(eq("one"))).andReturn(Optional.of(new String[]{"value1", "value2"}));
        expect(context.getMultipleStringConfig(eq("two"))).andReturn(Optional.of(new String[0]));
        expect(context.getMultipleStringConfig(eq("three"))).andReturn(Optional.empty());
        replay(context);

        ObjectNode json = config.multipleStrings("one", "two", "three").build();

        assertThat(json.size(), equalTo(2));
        assertTrue(json.get("one").isArray());
        assertTrue(json.get("two").isArray());
        assertThat(json.get("one").toString(), equalTo("[\"value1\",\"value2\"]"));
        assertThat(json.get("two").toString(), equalTo("[]"));
    }

    @Test
    public void mixedTypes() {
        expect(context.getBooleanConfig(eq("one"))).andReturn(Optional.of(true));
        expect(context.getStringConfig(eq("two"))).andReturn(Optional.of("value"));
        expect(context.getMultipleStringConfig(eq("three"))).andReturn(Optional.of(new String[]{"a", "b"}));
        replay(context);

        ObjectNode json = config
                .booleans("one")
                .strings("two")
                .multipleStrings("three")
                .build();

        assertThat(json.size(), equalTo(3));
        assertTrue(json.get("one").isBoolean());
        assertThat(json.get("one").booleanValue(), equalTo(true));
        assertTrue(json.get("two").isTextual());
        assertThat(json.get("two").textValue(), equalTo("value"));
        assertTrue(json.get("three").isArray());
        assertThat(json.get("three").toString(), equalTo("[\"a\",\"b\"]"));
    }

    @Test
    public void removePrefix() {
        expect(context.getStringConfig(eq("one"))).andReturn(Optional.of("first"));
        expect(context.getStringConfig(eq("foo-two"))).andReturn(Optional.of("second"));
        expect(context.getStringConfig(eq("foo-three"))).andReturn(Optional.of("third"));
        expect(context.getStringConfig(eq("bar-four"))).andReturn(Optional.of("fourth"));
        replay(context);

        ObjectNode json = config
                .strings("one")
                .removePrefix("foo-")
                .strings("foo-two")
                .strings("foo-three")
                .removePrefix("bar-")
                .strings("bar-four")
                .build();

        assertThat(json.size(), equalTo(4));
        assertThat(json.get("one").textValue(), equalTo("first"));
        assertThat(json.get("two").textValue(), equalTo("second"));
        assertThat(json.get("three").textValue(), equalTo("third"));
        assertThat(json.get("four").textValue(), equalTo("fourth"));
    }
}