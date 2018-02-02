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

    import java.util.EnumMap;

    import org.hippoecm.repository.api.StringCodec;
    import org.hippoecm.repository.api.StringCodecFactory;
    import org.hippoecm.repository.api.StringCodecService;

    class StringCodecServiceImpl implements StringCodecService {

        private static final EnumMap<Encoding, String> STRING_CODEC_NAMES = initStringCodecNames();

        private static EnumMap<Encoding, String> initStringCodecNames() {
            final EnumMap<Encoding, String> result = new EnumMap<>(Encoding.class);
            result.put(Encoding.IDENTITY, null);
            result.put(Encoding.DISPLAY_NAME, "encoding.display");
            result.put(Encoding.NODE_NAME, "encoding.node");
            return result;
        }

        private final StringCodecModuleConfig config;

        StringCodecServiceImpl(final StringCodecModuleConfig config) {
            this.config = config;
        }

        @Override
        public StringCodec getStringCodec(final Encoding encoding) {
            return getStringCodec(encoding, null);
        }

        @Override
        public StringCodec getStringCodec(final Encoding encoding, final String locale) {
            final StringCodecFactory factory = config.getStringCodecFactory();
            final String stringCodecName = STRING_CODEC_NAMES.get(encoding);
            return factory.getStringCodec(stringCodecName, locale);
        }
    }
