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
package org.hippoecm.repository.stringcodec;

import org.hippoecm.repository.api.StringCodec;
import org.hippoecm.repository.api.StringCodecFactory;
import org.hippoecm.repository.api.StringCodecFactory.IdentEncoding;
import org.hippoecm.repository.api.StringCodecService.Encoding;
import org.junit.Test;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class StringCodecServiceImplTest {

    @Test
    public void getDefaultIdentityEncoding() {
        assertEncoding(Encoding.IDENTITY, null, null);
    }

    @Test
    public void getDefaultDisplayNameEncoding() {
        assertEncoding(Encoding.DISPLAY_NAME, "encoding.display", null);
    }

    @Test
    public void getDefaultNodeNameEncoding() {
        assertEncoding(Encoding.NODE_NAME, "encoding.node", null);
    }

    @Test
    public void getIdentityEncodingForLocale() {
        assertEncoding(Encoding.IDENTITY, null, "fr");
    }

    @Test
    public void getDisplayNameEncodingForLocale() {
        assertEncoding(Encoding.DISPLAY_NAME, "encoding.display", "en");
    }

    @Test
    public void getNodeNameEncodingForLocale() {
        assertEncoding(Encoding.NODE_NAME, "encoding.node", "en_GB");
    }

    private void assertEncoding(final Encoding encoding, final String internalName, final String locale) {
        final StringCodecFactory factory = createMock(StringCodecFactory.class);

        final StringCodec configuredCodec = new IdentEncoding();
        expect(factory.getStringCodec(eq(internalName), eq(locale))).andReturn(configuredCodec);
        replay(factory);

        final StringCodecServiceImpl service = createService(factory);
        final StringCodec codec = service.getStringCodec(encoding, locale);

        assertThat(codec, equalTo(configuredCodec));
    }

    private StringCodecServiceImpl createService(StringCodecFactory factory) {
        final StringCodecModuleConfig config = createMock(StringCodecModuleConfig.class);
        expect(config.getStringCodecFactory()).andReturn(factory).anyTimes();
        replay(config);
        return new StringCodecServiceImpl(config);
    }
}