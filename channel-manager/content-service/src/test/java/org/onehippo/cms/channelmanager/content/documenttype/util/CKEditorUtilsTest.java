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

package org.onehippo.cms.channelmanager.content.documenttype.util;

import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CKEditorUtilsTest {

    @Test
    public void readConfig() throws IOException {
        JsonNode json = CKEditorUtils.readConfig("  { one: 1\n, two: 'two' \n}  \n");
        assertEquals(1, json.get("one").asInt());
        assertEquals("two", json.get("two").asText());
    }

    @Test(expected = IOException.class)
    public void readConfigWithErrors() throws IOException {
        CKEditorUtils.readConfig("{ invalidJson: true");
    }
}