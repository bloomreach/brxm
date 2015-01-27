/*
 *  Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.api;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

public class StringCodecFactoryTest {

    @Test
    public void testNull() {
        Map<String, StringCodec> codecs = new HashMap<>();
        StringCodecFactory factory = new StringCodecFactory(codecs);

        assertThat(factory.getStringCodec(), nullValue());
        assertThat(factory.getStringCodec(null), nullValue());
        assertThat(factory.getStringCodec("encode"), nullValue());
        assertThat(factory.getStringCodec("encode", "en"), nullValue());
        assertThat(factory.getStringCodec("encode", "en_GB"), nullValue());
    }

    @Test
    public void testLocalizedCodecs() {
        StringCodec codecNL = new MockStringCodec();
        StringCodec codecDE = new MockStringCodec();
        StringCodec codecEN = new MockStringCodec();
        StringCodec codecEN_GB = new MockStringCodec();
        StringCodec codecNULL = new MockStringCodec();
        StringCodec codecDEFAULT = new MockStringCodec();

        Map<String, StringCodec> codecs = new HashMap<>();
        codecs.put(null, codecNULL);
        codecs.put("encode", codecDEFAULT);
        codecs.put("encode.nl", codecNL);
        codecs.put("encode.DE", codecDE);
        codecs.put("encode.en", codecEN);
        codecs.put("encode.en_GB", codecEN_GB);

        StringCodecFactory factory = new StringCodecFactory(codecs);

        assertThat(factory.getStringCodec(), is(codecNULL));
        assertThat(factory.getStringCodec(null), is(codecNULL));
        assertThat(factory.getStringCodec(null, null), is(codecNULL));
        assertThat(factory.getStringCodec("non-existing-key"), is(codecNULL));
        assertThat(factory.getStringCodec("non-existing-key", "non-existing-locale"), is(codecNULL));

        assertThat(factory.getStringCodec("encode"), is(codecDEFAULT));
        assertThat(factory.getStringCodec("ENCODE"), is(codecDEFAULT));
        assertThat(factory.getStringCodec("encode", "non-existing-locale"), is(codecDEFAULT));

        assertThat(factory.getStringCodec("encode", "nl"), is(codecNL));
        assertThat(factory.getStringCodec("encode", "NL"), is(codecNL));
        assertThat(factory.getStringCodec("ENCODE", "NL"), is(codecNL));

        assertThat(factory.getStringCodec("encode", "de"), is(codecDE));
        assertThat(factory.getStringCodec("encode", "DE"), is(codecDE));

        assertThat(factory.getStringCodec("encode", "en"), is(codecEN));
        assertThat(factory.getStringCodec("encode", "EN"), is(codecEN));
        assertThat(factory.getStringCodec("encode", "en_us"), is(codecEN));
        assertThat(factory.getStringCodec("encode", "en_US"), is(codecEN));
        assertThat(factory.getStringCodec("encode", "en_gb"), is(codecEN_GB));
    }

    private static class MockStringCodec implements StringCodec {

        @Override
        public String encode(final String plain) {
            return null;
        }

        @Override
        public String decode(final String encoded) {
            return null;
        }
    }

}
