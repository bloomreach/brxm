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
package org.hippoecm.repository.api;

import org.onehippo.cms7.services.SingletonService;

/**
 * Provides {@link StringCodec} instances for certain encodings, possibly customized for a certain locale.
 */
@SingletonService
public interface StringCodecService {

    enum Encoding {
        /**
         * Return the same string.
         */
        IDENTITY,

        /**
         * Generate a name to display to a user.
         */
        DISPLAY_NAME,

        /**
         * Generate a JCR node name.
         */
        NODE_NAME,
    }

    /**
     * Returns the default string codec for a certain encoding.
     * Equivalent to calling {@link #getStringCodec(Encoding, String)} with a null locale.
     *
     * @param encoding the type of encoding
     *
     * @return the default string codec for the given encoding
     */
    StringCodec getStringCodec(final Encoding encoding);

    /**
     * Returns the string codec for a certain encoding that is best suited for the given locale.
     *
     * @param encoding the type of encoding
     * @param locale   the locale the string codec will be used for. Can be null.
     *
     * @return the best matching string codec, or the default string codec if no specific one could be found.
     */
    StringCodec getStringCodec(final Encoding encoding, final String locale);

}
