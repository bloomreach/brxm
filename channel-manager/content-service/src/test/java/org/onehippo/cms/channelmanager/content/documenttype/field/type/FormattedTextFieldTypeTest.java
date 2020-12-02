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

package org.onehippo.cms.channelmanager.content.documenttype.field.type;

import java.io.IOException;
import java.util.Optional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldTypeContext;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldTypeUtils;
import org.onehippo.cms.channelmanager.content.documenttype.util.LocalizationUtils;
import org.onehippo.cms.json.Json;
import org.onehippo.cms7.services.htmlprocessor.HtmlProcessorFactory;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.powermock.api.easymock.PowerMock.mockStatic;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"org.apache.logging.log4j.*", "javax.management.*", "com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "org.w3c.dom.*", "com.sun.org.apache.xalan.*", "javax.activation.*", "javax.net.ssl.*"})
@PrepareForTest({HtmlFieldConfig.class, HtmlProcessorFactory.class, LocalizationUtils.class, FieldTypeUtils.class})
public class FormattedTextFieldTypeTest {

    private static FormattedTextFieldType initField(final String defaultJson,
                                             final String defaultHtmlProcessorId,
                                             final String htmlProcessorId) throws IOException {
        mockStatic(HtmlFieldConfig.class);

        final FormattedTextFieldType field = new FormattedTextFieldType(defaultJson, defaultHtmlProcessorId);
        final FieldTypeContext fieldContext = new MockFieldTypeContext.Builder(field)
                .jcrName("myproject:htmlfield")
                .build();

        expect(fieldContext.getStringConfig("maxlength")).andReturn(Optional.empty());
        expect(HtmlFieldConfig.readJson(fieldContext, defaultJson)).andReturn(Json.object(defaultJson));
        expect(fieldContext.getStringConfig("htmlprocessor.id")).andReturn(Optional.of(htmlProcessorId));

        replayAll();

        field.init(fieldContext);

        return field;
    }

    @Test
    public void getType() {
        final FormattedTextFieldType field = new FormattedTextFieldType();
        assertEquals(FieldType.Type.HTML, field.getType());
    }

    @Test
    public void getConfig() throws IOException {
        final String defaultConfig = "{ a: 1 }";
        final FormattedTextFieldType field = initField(defaultConfig, "", "");
        assertEquals(Json.object(defaultConfig), field.getConfig());
    }

    @Test
    public void customHtmlProcessorId() throws IOException {
        mockStatic(HtmlProcessorFactory.class);
        expect(HtmlProcessorFactory.of(eq("custom-formatted")))
                .andReturn((HtmlProcessorFactory) () -> HtmlProcessorFactory.NOOP);


        initField("", "formatted", "custom-formatted");

        verifyAll();
    }
}
