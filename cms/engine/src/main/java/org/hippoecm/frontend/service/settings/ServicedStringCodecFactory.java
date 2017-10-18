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
package org.hippoecm.frontend.service.settings;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.hippoecm.frontend.util.CodecUtils;
import org.hippoecm.repository.api.StringCodec;
import org.hippoecm.repository.api.StringCodecFactory;
import org.hippoecm.repository.api.StringCodecService;
import org.hippoecm.repository.api.StringCodecService.Encoding;
import org.onehippo.cms7.services.HippoServiceRegistry;

/**
 * String codec factory that delegates the lookup of codecs to the {@link StringCodecService}.
 */
public class ServicedStringCodecFactory extends StringCodecFactory {

    private static final Map<String, Encoding> ENCODINGS = initEncodings();

    private static Map<String, Encoding> initEncodings() {
        final Map<String, Encoding> result = new HashMap<>();
        result.put(CodecUtils.ENCODING_DISPLAY, Encoding.DISPLAY_NAME);
        result.put(CodecUtils.ENCODING_NODE, Encoding.NODE_NAME);
        return result;
    }

    private final StringCodecService stringCodecService;

    ServicedStringCodecFactory() {
        super(Collections.emptyMap());
        stringCodecService = HippoServiceRegistry.getService(StringCodecService.class);
    }

    @Override
    public StringCodec getStringCodec() {
        return stringCodecService.getStringCodec(Encoding.IDENTITY);
    }

    @Override
    public StringCodec getStringCodec(final String encodingName) {
        final Encoding encoding = ENCODINGS.getOrDefault(encodingName, Encoding.IDENTITY);
        return stringCodecService.getStringCodec(encoding);
    }

    @Override
    public StringCodec getStringCodec(final String encodingName, final String locale) {
        final Encoding encoding = ENCODINGS.getOrDefault(encodingName, Encoding.IDENTITY);
        return stringCodecService.getStringCodec(encoding, locale);
    }
}
