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
package org.onehippo.ckeditor;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class CKEditorConfigTest {

    private ObjectMapper mapper;

    @Before
    public void setUp() {
        mapper = new ObjectMapper();
        mapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
        mapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
    }

    @Test
    public void defaultFormattedTextConfigIsValidJson() throws IOException {
        final JsonNode json = mapper.readTree(CKEditorConfig.DEFAULT_FORMATTED_TEXT_CONFIG);
        assertNotNull(json);
    }

    @Test
    public void defaultRichTextConfigIsValidJson() throws IOException {
        final JsonNode json = mapper.readTree(CKEditorConfig.DEFAULT_RICH_TEXT_CONFIG);
        assertNotNull(json);
    }

    @Test
    public void combineConfig() throws IOException {
        final String defaultJson = "{"
                + "  codemirror: {"
                + "    autoFormatOnStart: true"
                + "  },"
                + "  ignoreEmptyParagraph: false,"
                + "  keystrokes: [ [ 77, 'maximize' ] ],"
                + "  plugins: 'foo,bar'"
                + "}";
        final String overlayedJson = "{"
                + "  codemirror: {"
                + "    autoFormatOnStart: false"
                + "  },"
                + "  ignoreEmptyParagraph: true"
                + "}";
        final String appendedJson = "{"
                + "  keystrokes: [ [ 88, 'showblocks' ] ],"
                + "  plugins: 'myplugin'"
                + "}";
        final ObjectNode combined = CKEditorConfig.combineConfig(defaultJson, overlayedJson, appendedJson);
        assertEquals("{\n"
                + "  codemirror : {\n"
                + "    autoFormatOnStart : false\n"
                + "  },\n"
                + "  ignoreEmptyParagraph : true,\n"
                + "  keystrokes : [ [ 77, \"maximize\" ], [ 88, \"showblocks\" ] ],\n"
                + "  plugins : \"foo,bar,myplugin\"\n"
                + "}",
                Json.prettyString(combined));
    }


    @Test
    public void extraAllowedContentOfRichTextFieldsCanBeExtended() throws IOException {
        // in addition to the default allowed content, allow a 'class' attribute on 'em' elements (new element)
        // and an extra 'id' property on img elements (new property on existing element in default config)
        final String appendedJson = "{"
                + "  extraAllowedContent: {"
                + "    em: {"
                + "      attributes: 'class'"
                + "    },"
                + "    img: {"
                + "      attributes: 'id'"
                + "    }"
                + "  }"
                + "}";
        final ObjectNode combined = CKEditorConfig.combineConfig(CKEditorConfig.DEFAULT_RICH_TEXT_CONFIG, "", appendedJson);
        final JsonNode extraAllowedContent = combined.get("extraAllowedContent");

        // our new 'em' rule is in there
        assertTrue(extraAllowedContent.has("em"));
        assertEquals("class", extraAllowedContent.get("em").get("attributes").asText());

        // the default rules for 'img' is extended to also allow the 'id' attribute
        assertTrue(extraAllowedContent.has("img"));
        assertEquals("border,hspace,vspace,id", extraAllowedContent.get("img").get("attributes").asText());

        // the default rule for 'p' is unaffected
        assertTrue(extraAllowedContent.has("p"));
        assertEquals("align", extraAllowedContent.get("p").get("attributes").asText());
    }

    @Test
    public void setDefaultLanguage() throws IOException {
        ObjectNode result = CKEditorConfig.setDefaults(Json.object(), "nl");
        assertEquals("nl", result.get("language").asText());
    }

    @Test
    public void convertKeystrokes() throws IOException {
        final ObjectNode config = Json.object("{ keystrokes: [ [ 88, 'foo' ], [ 'Ctrl', 'B', 'bar' ] ] }");
        final ObjectNode result = CKEditorConfig.setDefaults(config, "nl");
        assertEquals("[ [ 88, \"foo\" ], [ 1114178, \"bar\" ] ]",
                Json.prettyString(result.get("keystrokes")));
    }

    @Test
    public void customStylesSetReplacesLanguageParameter() throws IOException {
        final ObjectNode config = Json.object("{ stylesSet: 'mystyle_{language}:./mystyles.js' }");
        final ObjectNode result = CKEditorConfig.setDefaults(config, "nl");
        assertEquals("mystyle_nl:./mystyles.js", result.get("stylesSet").asText());
    }

    @Test
    public void defaultStylesSetIncludesLanguage() {
        final ObjectNode config = Json.object();
        final ObjectNode result = CKEditorConfig.setDefaults(config, "nl");
        assertEquals("hippo_nl:./hippostyles.js", result.get("stylesSet").asText());
    }

    @Test
    public void disableCustomConfigWhenNotSet() {
        ObjectNode result = CKEditorConfig.setDefaults(Json.object(), "nl");
        assertEquals("", result.get("customConfig").asText());
    }

    @Test
    public void leaveCustomConfigWhenSet() throws IOException {
        final ObjectNode config = Json.object("{ customConfig: 'myconfig.js' }");
        ObjectNode result = CKEditorConfig.setDefaults(config, "nl");
        assertEquals("myconfig.js", result.get("customConfig").asText());
    }
}