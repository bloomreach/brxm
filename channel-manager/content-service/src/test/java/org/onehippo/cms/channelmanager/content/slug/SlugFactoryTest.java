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
package org.onehippo.cms.channelmanager.content.slug;

import org.hippoecm.repository.api.StringCodec;
import org.hippoecm.repository.api.StringCodecService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"org.apache.logging.log4j.*", "javax.management.*", "com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "org.w3c.dom.*", "com.sun.org.apache.xalan.*", "javax.activation.*", "javax.net.ssl.*"})
@PrepareForTest({HippoServiceRegistry.class})
public class SlugFactoryTest {

    @Before
    public void setup() {
        PowerMock.mockStatic(HippoServiceRegistry.class);
    }

    @Test
    public void createSlugWithoutLocale() {
        final StringCodecService codecService = createMock(StringCodecService.class);
        final StringCodec codec = createMock(StringCodec.class);

        expect(HippoServiceRegistry.getService(StringCodecService.class)).andReturn(codecService);
        expect(codecService.getStringCodec(eq(StringCodecService.Encoding.NODE_NAME), eq(null))).andReturn(codec);
        expect(codec.encode("a document name")).andReturn("a-document-name");

        replayAll(codecService, codec);

        assertThat(SlugFactory.createSlug("a document name", null), equalTo("a-document-name"));

        verifyAll();
    }

    @Test
    public void createSlugWithLocale() {
        final StringCodecService codecService = createMock(StringCodecService.class);
        final StringCodec codec = createMock(StringCodec.class);

        expect(HippoServiceRegistry.getService(StringCodecService.class)).andReturn(codecService);
        expect(codecService.getStringCodec(eq(StringCodecService.Encoding.NODE_NAME), eq("de"))).andReturn(codec);
        expect(codec.encode("a document name")).andReturn("a-document-name");

        replayAll(codecService, codec);

        assertThat(SlugFactory.createSlug("a document name", "de"), equalTo("a-document-name"));

        verifyAll();
    }
}