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

package org.onehippo.cms.channelmanager.content.documenttype.field.type;

import java.util.Collections;
import java.util.Locale;
import java.util.Optional;

import org.easymock.EasyMock;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onehippo.ckeditor.CKEditorConfig;
import org.onehippo.cms.channelmanager.content.documenttype.ContentTypeContext;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldTypeContext;
import org.onehippo.cms.channelmanager.content.documenttype.model.DocumentType;
import org.onehippo.cms.channelmanager.content.documenttype.util.NamespaceUtils;
import org.onehippo.cms7.services.contenttype.ContentTypeItem;
import org.onehippo.cms7.services.processor.html.HtmlProcessorFactory;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.powermock.api.easymock.PowerMock.mockStatic;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.management.*")
@PrepareForTest({NamespaceUtils.class, HtmlProcessorFactory.class})
public class FormattedTextFieldTypeTest {

    private static final String DEFAULT_HTMLPROCESSOR_ID = "formatted";

    private FormattedTextFieldType initField(final String defaultJson, final String overlayedJson, final String appendedJson) {
        return initField(defaultJson, overlayedJson, appendedJson, DEFAULT_HTMLPROCESSOR_ID, DEFAULT_HTMLPROCESSOR_ID);
    }

    private FormattedTextFieldType initField(final String defaultJson, final String overlayedJson, final String appendedJson,
                                             final String defaultHtmlProcessorId, final String htmlProcessorId) {
        final ContentTypeContext parentContext = EasyMock.createMock(ContentTypeContext.class);
        expect(parentContext.getDocumentType()).andReturn(new DocumentType());
        expect(parentContext.getResourceBundle()).andReturn(Optional.empty());

        final ContentTypeItem contentTypeItem = EasyMock.createMock(ContentTypeItem.class);
        expect(contentTypeItem.getName()).andReturn("myproject:htmlfield");
        expect(contentTypeItem.getValidators()).andReturn(Collections.emptyList()).anyTimes();
        expect(contentTypeItem.isMultiple()).andReturn(false);

        final FieldTypeContext fieldContext = EasyMock.createMock(FieldTypeContext.class);
        expect(fieldContext.getContentTypeItem()).andReturn(contentTypeItem).anyTimes();
        expect(fieldContext.getEditorConfigNode()).andReturn(Optional.empty()).anyTimes();
        expect(fieldContext.getParentContext()).andReturn(parentContext).anyTimes();

        final Locale locale = new Locale("nl");
        expect(parentContext.getLocale()).andReturn(locale);

        mockStatic(NamespaceUtils.class);
        expect(NamespaceUtils.getConfigProperty(fieldContext, "maxlength")).andReturn(Optional.empty());
        expect(NamespaceUtils.getConfigProperty(fieldContext, "ckeditor.config.overlayed.json")).andReturn(Optional.of(overlayedJson));
        expect(NamespaceUtils.getConfigProperty(fieldContext, "ckeditor.config.appended.json")).andReturn(Optional.of(appendedJson));
        expect(NamespaceUtils.getConfigProperty(fieldContext, "htmlprocessor.id")).andReturn(Optional.of(htmlProcessorId));

        replayAll(parentContext, fieldContext, contentTypeItem);

        final FormattedTextFieldType field = new FormattedTextFieldType(defaultJson, defaultHtmlProcessorId);
        field.init(fieldContext);

        return field;
    }

    @Test
    public void getType() {
        final FormattedTextFieldType field = new FormattedTextFieldType("", DEFAULT_HTMLPROCESSOR_ID);
        assertEquals(FieldType.Type.HTML, field.getType());
    }

    @Test
    public void configWithErrorsIsNull() {
        final FormattedTextFieldType field = new FormattedTextFieldType("{ this is not valid json ", DEFAULT_HTMLPROCESSOR_ID);
        assertNull(field.getConfig());
    }

    @Test
    public void configContainsLanguage() {
        final FormattedTextFieldType field = initField("", "", "");
        assertEquals("nl", field.getConfig().get("language").asText());
    }

    @Test
    public void configIsCombined() {
        final FormattedTextFieldType field = initField("{ test: 1, plugins: 'a,b' }", "{ test: 2 }", "{ plugins: 'c,d' }");
        assertEquals(2, field.getConfig().get("test").asInt());
        assertEquals("a,b,c,d", field.getConfig().get("plugins").asText());
    }

    @Test
    public void customConfigIsDisabledWhenNotConfigured() {
        final FormattedTextFieldType field = initField("", "", "");
        assertEquals("", field.getConfig().get(CKEditorConfig.CUSTOM_CONFIG).asText());
    }

    @Test
    public void customConfigIsKeptWhenConfigured() {
        final FormattedTextFieldType field = initField("{ customConfig: 'myconfig.js' }", "", "");
        assertEquals("myconfig.js", field.getConfig().get(CKEditorConfig.CUSTOM_CONFIG).asText());
    }

    @Test
    public void customHtmlProcessorId() {
        mockStatic(HtmlProcessorFactory.class);
        expect(HtmlProcessorFactory.of(eq("custom-formatted")))
                .andReturn((HtmlProcessorFactory) () -> HtmlProcessorFactory.NOOP);


        initField("", "", "", DEFAULT_HTMLPROCESSOR_ID, "custom-formatted");
        verifyAll();
    }
}