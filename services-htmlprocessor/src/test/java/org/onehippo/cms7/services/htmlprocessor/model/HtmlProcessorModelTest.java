/*
 *  Copyright 2017-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.services.htmlprocessor.model;

import java.io.IOException;

import org.apache.logging.log4j.Level;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.cms7.services.htmlprocessor.HtmlProcessor;
import org.onehippo.cms7.services.htmlprocessor.HtmlProcessorFactory;
import org.onehippo.testutils.log4j.Log4jInterceptor;

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.onehippo.cms7.services.htmlprocessor.richtext.TestUtil.assertLogMessage;

public class HtmlProcessorModelTest {

    private HtmlProcessor processor;
    private HtmlProcessorFactory processorFactory;

    @Before
    public void setUp() throws Exception {
        processor = EasyMock.createMock(HtmlProcessor.class);
        processorFactory = createProcessorFactory(processor);
    }

    @Test
    public void testSet() throws Exception {
        final String html = "<p>text</p>";
        final Model<String> valueModel = EasyMock.createMock(Model.class);
        valueModel.set(html);
        expectLastCall();
        expect(processor.write(eq(html), eq(null))).andReturn(html);

        replay(valueModel, processor, processorFactory);

        final HtmlProcessorModel processorModel = new HtmlProcessorModel(valueModel, processorFactory);
        processorModel.set(html);

        verify(valueModel, processorFactory);
    }

    @Test
    public void testGet() throws Exception {
        final String html = "<p>text</p>";
        final Model<String> valueModel = EasyMock.createMock(Model.class);
        expect(valueModel.get()).andReturn(html);
        expect(processor.read(eq(html), eq(null))).andReturn(html);

        replay(valueModel, processor, processorFactory);

        final HtmlProcessorModel processorModel = new HtmlProcessorModel(valueModel, processorFactory);
        assertEquals(html, processorModel.get());

        verify(valueModel, processorFactory);
    }

    @Test
    public void testGetWithExceptionReturnsEmptyString() throws Exception {
        final String html = "<p>text</p>";
        final Model<String> valueModel = EasyMock.createMock(Model.class);
        expect(valueModel.get()).andReturn(html);

        final HtmlProcessorFactory processorFactory = createProcessorFactory(processor);
        expect(processor.read(eq(html), eq(null))).andThrow(new IOException("Expected exception"));

        replay(valueModel, processor, processorFactory);

        final HtmlProcessorModel processorModel = new HtmlProcessorModel(valueModel, processorFactory);
        assertEquals("", processorModel.get());

        verify(valueModel, processor, processorFactory);
    }

    @Test
    public void testGetLogMessages() throws Exception {
        final HtmlProcessorFactory processorFactory = createProcessorFactory(processor);

        expect(processor.read(eq("test"), eq(null))).andThrow(new IOException("Expected exception"));
        replay(processor, processorFactory);

        final HtmlProcessorModel processorModel = new HtmlProcessorModel(Model.of("test"), processorFactory);
        try (Log4jInterceptor interceptor = Log4jInterceptor.onWarn().trap(HtmlProcessorModel.class).build()) {
            processorModel.get();
            assertLogMessage(interceptor, "Value not retrieved because html processing failed : java.io.IOException: " +
                    "Expected exception", Level.WARN);
        }
    }

    @Test
    public void testSetWarningMessages() throws Exception {

        expect(processor.write(eq("test"), eq(null))).andThrow(new IOException("Expected exception"));
        replay(processor, processorFactory);

        final HtmlProcessorModel processorModel = new HtmlProcessorModel(Model.of(""), processorFactory);
        try (Log4jInterceptor interceptor = Log4jInterceptor.onWarn().trap(HtmlProcessorModel.class).build()) {
            processorModel.set("test");
            assertLogMessage(interceptor, "Value not set because html processing failed : java.io.IOException: " +
                    "Expected exception", Level.WARN);
        }
        verify(processor, processorFactory);
    }

    private static HtmlProcessorFactory createProcessorFactory(final HtmlProcessor processor) {
        final HtmlProcessorFactory processorFactory = EasyMock.createMock(HtmlProcessorFactory.class);
        expect(processorFactory.getProcessor()).andReturn(processor);
        return processorFactory;
    }
}
