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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import com.fasterxml.jackson.databind.JsonNode;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onehippo.cms.channelmanager.content.document.model.FieldValue;
import org.onehippo.cms.channelmanager.content.documenttype.ContentTypeContext;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldTypeContext;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldTypeUtils;
import org.onehippo.cms.channelmanager.content.documenttype.model.DocumentType;
import org.onehippo.cms.channelmanager.content.error.BadRequestException;
import org.onehippo.cms.channelmanager.content.error.InternalServerErrorException;
import org.onehippo.cms7.services.contenttype.ContentTypeItem;
import org.onehippo.cms7.services.processor.html.HtmlProcessor;
import org.onehippo.cms7.services.processor.html.HtmlProcessorConfig;
import org.onehippo.cms7.services.processor.html.HtmlProcessorFactory;
import org.onehippo.cms7.services.processor.html.HtmlProcessorImpl;
import org.onehippo.repository.mock.MockNode;
import org.onehippo.testutils.log4j.Log4jInterceptor;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.easymock.EasyMock.anyInt;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.easymock.PowerMock.mockStatic;
import static org.powermock.api.easymock.PowerMock.replay;
import static org.powermock.api.easymock.PowerMock.replayAll;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.management.*")
@PrepareForTest({FieldTypeUtils.class, HtmlProcessorFactory.class})
public class RichTextFieldTypeTest {

    private static final String FIELD_NAME = "test:richtextfield";
    private static final String VALUE_PROPERTY = "hippostd:content";

    private Node document;

    @Before
    public void setUp() throws RepositoryException {
        document = MockNode.root();
    }

    private RichTextFieldType initField() {
        return initField(null);
    }

    private RichTextFieldType initField(final HtmlProcessor htmlProcessor) {
        final ContentTypeContext parentContext = EasyMock.createMock(ContentTypeContext.class);
        expect(parentContext.getDocumentType()).andReturn(new DocumentType());
        expect(parentContext.getResourceBundle()).andReturn(Optional.empty());

        final ContentTypeItem contentTypeItem = EasyMock.createMock(ContentTypeItem.class);
        expect(contentTypeItem.getName()).andReturn(FIELD_NAME);
        expect(contentTypeItem.getValidators()).andReturn(Collections.emptyList()).anyTimes();
        expect(contentTypeItem.isMultiple()).andReturn(false);

        final FieldTypeContext fieldContext = EasyMock.createMock(FieldTypeContext.class);
        expect(fieldContext.getContentTypeItem()).andReturn(contentTypeItem).anyTimes();
        expect(fieldContext.getEditorConfigNode()).andReturn(Optional.empty()).anyTimes();
        expect(fieldContext.getParentContext()).andReturn(parentContext).anyTimes();

        final Locale locale = new Locale("nl");
        expect(parentContext.getLocale()).andReturn(locale);

        expect(fieldContext.getStringConfig("maxlength")).andReturn(Optional.empty());
        expect(fieldContext.getStringConfig("ckeditor.config.overlayed.json")).andReturn(Optional.empty());
        expect(fieldContext.getStringConfig("ckeditor.config.appended.json")).andReturn(Optional.empty());
        expect(fieldContext.getStringConfig("htmlprocessor.id")).andReturn(Optional.of("richtext"));

        expect(fieldContext.getBooleanConfig("linkpicker.language.context.aware")).andReturn(Optional.empty());
        expect(fieldContext.getBooleanConfig("linkpicker.last.visited.enabled")).andReturn(Optional.of(true));
        expect(fieldContext.getBooleanConfig("linkpicker.open.in.new.window.enabled")).andReturn(Optional.of(false));
        expect(fieldContext.getStringConfig("linkpicker.base.uuid")).andReturn(Optional.of("cafebabe"));
        expect(fieldContext.getStringConfig("linkpicker.cluster.name")).andReturn(Optional.of("linkpicker-cluster"));
        expect(fieldContext.getStringConfig("linkpicker.last.visited.key")).andReturn(Optional.of("linkpicker-last-visited-key"));
        expect(fieldContext.getMultipleStringConfig("linkpicker.last.visited.nodetypes")).andReturn(Optional.of(new String[]{"hippostd:folder"}));
        expect(fieldContext.getMultipleStringConfig("linkpicker.nodetypes")).andReturn(Optional.of(new String[]{"hippo:document"}));

        expect(fieldContext.getBooleanConfig("imagepicker.last.visited.enabled")).andReturn(Optional.of(true));
        expect(fieldContext.getStringConfig("imagepicker.base.uuid")).andReturn(Optional.of("cafebabe"));
        expect(fieldContext.getStringConfig("imagepicker.cluster.name")).andReturn(Optional.of("imagepicker-cluster"));
        expect(fieldContext.getStringConfig("imagepicker.last.visited.key")).andReturn(Optional.of("imagepicker-last-visited-key"));
        expect(fieldContext.getStringConfig("imagepicker.preferred.image.variant")).andReturn(Optional.of("hippogallery:original"));
        expect(fieldContext.getMultipleStringConfig("excluded.image.variants")).andReturn(Optional.of(new String[]{"hippogallery:thumbnail"}));
        expect(fieldContext.getMultipleStringConfig("imagepicker.last.visited.nodetypes")).andReturn(Optional.of(new String[]{"hippostd:folder"}));
        expect(fieldContext.getMultipleStringConfig("imagepicker.nodetypes")).andReturn(Optional.of(new String[]{"hippostd:gallery"}));
        expect(fieldContext.getMultipleStringConfig("included.image.variants")).andReturn(Optional.of(new String[0]));

        mockStatic(HtmlProcessorFactory.class);
        expect(HtmlProcessorFactory.of(eq("richtext")))
                .andReturn((HtmlProcessorFactory) () -> htmlProcessor != null ? htmlProcessor : HtmlProcessorFactory.NOOP).anyTimes();

        replayAll(parentContext, fieldContext, contentTypeItem);

        final RichTextFieldType field = new RichTextFieldType();
        field.init(fieldContext);

        return field;
    }

    private Node addValue(final String html) {
        try {
            final Node value = document.addNode(FIELD_NAME, "hippostd:html");
            value.setProperty("hippostd:content", html);
            return value;
        } catch (final RepositoryException e) {
            throw new RuntimeException(e);
        }
    }

    private String getValue() {
        try {
            return document.getNode(FIELD_NAME).getProperty(VALUE_PROPERTY).getString();
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }

    private interface Code {
        void run() throws Exception;
    }

    private static void assertWarningsLogged(final long count, final Code code, final Code... verifications) throws Exception {
        try (Log4jInterceptor listener = Log4jInterceptor.onWarn().trap(RichTextFieldType.class).build()) {
            try {
                code.run();
            } finally {
                assertThat(count + " warnings logged", listener.messages().count(), equalTo(count));
                for (final Code verification : verifications) {
                    verification.run();
                }
            }
        }
    }

    private static void assertNoWarningsLogged(final Code code, final Code... verifications) throws Exception {
        assertWarningsLogged(0, code, verifications);
    }

    @Test
    public void readHippoPickerConfig() throws Exception {
        final RichTextFieldType field = initField();
        final JsonNode hippopicker = field.getConfig().get("hippopicker");

        assertNoWarningsLogged(() -> {
            final JsonNode internalLink = hippopicker.get("internalLink");
            assertFalse(internalLink.has("language.context.aware"));
            assertThat(internalLink.get("last.visited.enabled").booleanValue(), equalTo(true));
            assertThat(internalLink.get("open.in.new.window.enabled").booleanValue(), equalTo(false));
            assertThat(internalLink.get("base.uuid").asText(), equalTo("cafebabe"));
            assertThat(internalLink.get("cluster.name").asText(), equalTo("linkpicker-cluster"));
            assertThat(internalLink.get("last.visited.key").asText(), equalTo("linkpicker-last-visited-key"));
            assertThat(internalLink.get("last.visited.nodetypes").toString(), equalTo("[\"hippostd:folder\"]"));
            assertThat(internalLink.get("nodetypes").toString(), equalTo("[\"hippo:document\"]"));

            final JsonNode image = hippopicker.get("image");
            assertThat(image.get("last.visited.enabled").booleanValue(), equalTo(true));
            assertThat(image.get("base.uuid").asText(), equalTo("cafebabe"));
            assertThat(image.get("cluster.name").asText(), equalTo("imagepicker-cluster"));
            assertThat(image.get("last.visited.key").asText(), equalTo("imagepicker-last-visited-key"));
            assertThat(image.get("preferred.image.variant").asText(), equalTo("hippogallery:original"));
            assertThat(image.get("excluded.image.variants").toString(), equalTo("[\"hippogallery:thumbnail\"]"));
            assertThat(image.get("last.visited.nodetypes").toString(), equalTo("[\"hippostd:folder\"]"));
            assertThat(image.get("nodetypes").toString(), equalTo("[\"hippostd:gallery\"]"));
            assertThat(image.get("included.image.variants").toString(), equalTo("[]"));
        });
    }

    @Test
    public void readSingleValue() throws Exception {
        final RichTextFieldType field = initField();
        addValue("<p>value</p>");
        assertNoWarningsLogged(() -> {
            final List<FieldValue> fieldValues = field.readFrom(document)
                    .orElseThrow(() -> new Exception("Failed to read from document"));
            assertThat(fieldValues.size(), equalTo(1));
            assertThat(fieldValues.get(0).getValue(), equalTo("<p>value</p>"));
        });
    }

    @Test
    public void readMultipleValue() throws Exception {
        final RichTextFieldType field = initField();
        addValue("<p>one</p>");
        addValue("<p>two</p>");
        field.setMaxValues(2);
        assertNoWarningsLogged(() -> {
            final List<FieldValue> fieldValues = field.readFrom(document)
                    .orElseThrow(() -> new Exception("Failed to read from document"));
            assertThat(fieldValues.size(), equalTo(2));
            assertThat(fieldValues.get(0).getValue(), equalTo("<p>one</p>"));
            assertThat(fieldValues.get(1).getValue(), equalTo("<p>two</p>"));
        });
    }

    @Test
    public void readOptionalEmptyValue() throws Exception {
        final RichTextFieldType field = initField();
        assertNoWarningsLogged(() -> assertFalse(field.readFrom(document).isPresent()));
    }

    @Test
    public void exceptionWhileReading() throws Exception {
        final RichTextFieldType field = initField();
        // make hippostd:content property multi-valued so reading it will throw an exception
        final Node value = addValue("");
        value.getProperty("hippostd:content").setValue(new String[]{"one", "two"});

        assertWarningsLogged(1, () -> assertFalse(field.readFrom(document).isPresent()));
    }

    @Test
    public void writeSingleValue() throws Exception {
        final RichTextFieldType field = initField();
        addValue("<p>value</p>");
        assertNoWarningsLogged(() -> {
            final FieldValue newValue = new FieldValue("<p>changed</p>");

            field.writeTo(document, Optional.of(Collections.singletonList(newValue)));

            final List<FieldValue> fieldValues = field.readFrom(document)
                    .orElseThrow(() -> new Exception("Failed to read from document"));
            assertThat(fieldValues.size(), equalTo(1));
            assertThat(fieldValues.get(0).getValue(), equalTo("<p>changed</p>"));
        });
    }

    @Test
    public void writeMultipleValues() throws Exception {
        final RichTextFieldType field = initField();
        addValue("<p>one</p>");
        addValue("<p>two</p>");
        field.setMaxValues(2);
        assertNoWarningsLogged(() -> {
            final FieldValue newValue1 = new FieldValue("<p>one changed</p>");
            final FieldValue newValue2 = new FieldValue("<p>two changed</p>");

            field.writeTo(document, Optional.of(Arrays.asList(newValue1, newValue2)));

            final List<FieldValue> fieldValues = field.readFrom(document)
                    .orElseThrow(() -> new Exception("Failed to read from document"));
            assertThat(fieldValues.size(), equalTo(2));
            assertThat(fieldValues.get(0).getValue(), equalTo("<p>one changed</p>"));
            assertThat(fieldValues.get(1).getValue(), equalTo("<p>two changed</p>"));
        });
    }

    @Test
    public void writeOptionalEmptyValue() throws Exception {
        final RichTextFieldType field = initField();
        field.setMinValues(0);
        assertNoWarningsLogged(() -> {
            field.writeTo(document, Optional.of(Collections.emptyList()));
            assertFalse(field.readFrom(document).isPresent());
        });
    }

    @Test(expected = BadRequestException.class)
    public void writeLessValuesThanMinimum() throws Exception {
        final RichTextFieldType field = initField();
        // default minimum is 1
        field.writeTo(document, Optional.of(Collections.emptyList()));
    }

    @Test(expected = BadRequestException.class)
    public void writeMoreValuesThanMaximum() throws Exception {
        final RichTextFieldType field = initField();
        // default maximum is 1
        final FieldValue newValue1 = new FieldValue("<p>one</p>");
        final FieldValue newValue2 = new FieldValue("<p>two</p>");
        field.writeTo(document, Optional.of(Arrays.asList(newValue1, newValue2)));
    }

    @Test
    public void writeLessValuesThanStored() throws Exception {
        final RichTextFieldType field = initField();
        addValue("<p>one</p>");
        addValue("<p>two</p>");
        addValue("<p>three</p>");
        assertThat(document.getNodes().getSize(), equalTo(3L));

        assertNoWarningsLogged(
                () -> {
                    final FieldValue newValue = new FieldValue("<p>changed</p>");
                    field.writeTo(document, Optional.of(Collections.singletonList(newValue)));
                }, () -> {
                    final List<FieldValue> fieldValues = field.readFrom(document)
                            .orElseThrow(() -> new Exception("Failed to read from document"));
                    assertThat(fieldValues.size(), equalTo(1));
                    assertThat(fieldValues.get(0).getValue(), equalTo("<p>changed</p>"));
                    assertThat(document.getNodes().getSize(), equalTo(1L));
                });
    }

    @Test(expected = InternalServerErrorException.class)
    public void exceptionWhileWriting() throws Exception {
        final RichTextFieldType field = initField();
        PowerMock.mockStaticPartial(FieldTypeUtils.class, "writeNodeValues");
        FieldTypeUtils.writeNodeValues(anyObject(), anyObject(), anyInt(), anyObject());
        expectLastCall().andThrow(new RepositoryException());
        replay(FieldTypeUtils.class);

        addValue("<p>value</p>");
        final FieldValue newValue = new FieldValue("<p>changed</p>");

        assertWarningsLogged(1,
                () -> field.writeTo(document, Optional.of(Collections.singletonList(newValue))),
                () -> {
                    final List<FieldValue> fieldValues = field.readFrom(document)
                            .orElseThrow(() -> new Exception("Failed to read from document"));
                    assertThat(fieldValues.size(), equalTo(1));
                    assertThat(fieldValues.get(0).getValue(), equalTo("<p>value</p>"));
                });
    }

    @Test
    public void validateRequired() {
        final RichTextFieldType field = initField();
        field.addValidator(FieldType.Validator.REQUIRED);

        assertTrue(field.isRequired());
        assertTrue(field.validate(Collections.singletonList(new FieldValue("test"))));
        assertFalse(field.validate(Collections.singletonList(new FieldValue(""))));
        assertFalse(field.validate(Arrays.asList(new FieldValue("test"), new FieldValue(""))));
    }

    @Test
    public void validateNotRequired() {
        final RichTextFieldType field = initField();
        assertFalse(field.isRequired());
        assertTrue(field.validate(Collections.singletonList(new FieldValue("test"))));
        assertTrue(field.validate(Collections.singletonList(new FieldValue(""))));
        assertTrue(field.validate(Arrays.asList(new FieldValue("test"), new FieldValue(""))));
    }

    @Test
    public void validateRequiredValue() {
        final RichTextFieldType field = initField();
        field.addValidator(FieldType.Validator.REQUIRED);

        assertTrue(field.isRequired());
        assertTrue(field.validateValue(new FieldValue("test")));
        assertFalse(field.validateValue(new FieldValue("")));
    }

    @Test
    public void validateNotRequiredValue() {
        final RichTextFieldType field = initField();
        assertFalse(field.isRequired());
        assertTrue(field.validateValue(new FieldValue("test")));
        assertTrue(field.validateValue(new FieldValue("")));
    }

    @Test
    public void processValueWhenReading() throws Exception {
        final HtmlProcessor processor = EasyMock.createMock(HtmlProcessor.class);
        expect(processor.read(eq("<p>value</p>"), anyObject())).andReturn("<p>processed</p>");
        replay(processor);

        final RichTextFieldType field = initField(processor);

        addValue("<p>value</p>");
        assertNoWarningsLogged(() -> {
            final List<FieldValue> fieldValues = field.readFrom(document)
                    .orElseThrow(() -> new Exception("Failed to read from document"));
            assertThat(fieldValues.get(0).getValue(), equalTo("<p>processed</p>"));
        });
    }

    @Test
    public void processValueWhenWriting() throws Exception {
        final HtmlProcessor processor = EasyMock.createMock(HtmlProcessor.class);
        expect(processor.write(eq("<p>value</p>"), anyObject())).andReturn("<p>processed</p>");
        replay(processor);

        final RichTextFieldType field = initField(processor);

        addValue("");
        assertNoWarningsLogged(() -> {
            final FieldValue newValue = new FieldValue("<p>value</p>");
            field.writeTo(document, Optional.of(Collections.singletonList(newValue)));
            assertThat(getValue(), equalTo("<p>processed</p>"));
        });
    }

    @Test
    public void linkedImagesHaveRelativePaths() throws Exception {
        final HtmlProcessor processor = new HtmlProcessorImpl(new HtmlProcessorConfig());
        final RichTextFieldType field = initField(processor);

        addValue("<img src=\"path/to/image.gif\" data-uuid=\"cafebabe\"/>");
        assertNoWarningsLogged(() -> {
            final List<FieldValue> fieldValues = field.readFrom(document)
                    .orElseThrow(() -> new Exception("Failed to read from document"));
            assertThat(fieldValues.get(0).getValue(), equalTo("<img src=\"../../path/to/image.gif\" data-uuid=\"cafebabe\" />"));
        });
    }

    @Test
    public void linkedImagesWithoutDataUuidAreNotTouched() throws Exception {
        final HtmlProcessor processor = new HtmlProcessorImpl(new HtmlProcessorConfig());
        final RichTextFieldType field = initField(processor);

        addValue("<img src=\"path/to/image.gif\"/>");
        assertNoWarningsLogged(() -> {
            final List<FieldValue> fieldValues = field.readFrom(document)
                    .orElseThrow(() -> new Exception("Failed to read from document"));
            assertThat(fieldValues.get(0).getValue(), equalTo("<img src=\"path/to/image.gif\" />"));
        });
    }
}

