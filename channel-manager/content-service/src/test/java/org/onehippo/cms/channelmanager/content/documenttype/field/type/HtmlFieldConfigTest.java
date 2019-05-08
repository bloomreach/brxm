/*
 * Copyright 2019 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cms.channelmanager.content.documenttype.field.type;

import java.io.IOException;
import java.util.Locale;
import java.util.Optional;

import org.junit.Test;
import org.onehippo.ckeditor.CKEditorConfig;
import org.onehippo.cms.channelmanager.content.documenttype.ContentTypeContext;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldTypeContext;

import com.fasterxml.jackson.databind.node.ObjectNode;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.replayAll;

public class HtmlFieldConfigTest {

    @Test(expected = IOException.class)
    public void configWithErrorsIsNull() throws IOException {
        final FieldTypeContext context = createFieldTypeContext();
        HtmlFieldConfig.readJson(context, "{ this is not valid json ");
    }

    @Test
    public void configContainsLanguage() throws IOException {
        final FieldTypeContext context = createFieldTypeContext();
        final ObjectNode json = HtmlFieldConfig.readJson(context, "{}");

        assertEquals("nl", json.get("language").asText());
    }

    @Test
    public void configIsCombined() throws IOException {
        final String defaultJson = "{ test: 1, plugins: 'a,b' }";
        final String overlayedJson = "{ test: 2 }";
        final String appendedJson = "{ plugins: 'c,d' }";

        final FieldTypeContext context = createFieldTypeContext(overlayedJson, appendedJson);
        final ObjectNode json = HtmlFieldConfig.readJson(context, defaultJson);

        assertEquals(2, json.get("test").asInt());
        assertEquals("a,b,c,d", json.get("plugins").asText());
    }

    @Test
    public void customConfigIsDisabledWhenNotConfigured() throws IOException {
        final FieldTypeContext context = createFieldTypeContext();
        final ObjectNode json = HtmlFieldConfig.readJson(context, "{}");

        assertEquals("", json.get(CKEditorConfig.CUSTOM_CONFIG).asText());
    }

    @Test
    public void customConfigIsKeptWhenConfigured() throws IOException {
        final String overlayedJson = "{ customConfig: 'myconfig.js' }";
        final FieldTypeContext context = createFieldTypeContext(overlayedJson, "");
        final ObjectNode json = HtmlFieldConfig.readJson(context, "{}");

        assertEquals("myconfig.js", json.get(CKEditorConfig.CUSTOM_CONFIG).asText());
    }

    private static FieldTypeContext createFieldTypeContext() {
        return createFieldTypeContext("", "");
    }

    private static FieldTypeContext createFieldTypeContext(final String overlayedJson, final String appendedJson) {
        final ContentTypeContext parentContext = createMock(ContentTypeContext.class);
        expect(parentContext.getLocale()).andReturn(new Locale("nl"));

        final FieldTypeContext fieldContext = createMock(FieldTypeContext.class);
        expect(fieldContext.getParentContext()).andReturn(parentContext);
        expect(fieldContext.getStringConfig("ckeditor.config.overlayed.json")).andReturn(Optional.of(overlayedJson));
        expect(fieldContext.getStringConfig("ckeditor.config.appended.json")).andReturn(Optional.of(appendedJson));

        replayAll();

        return fieldContext;
    }

}