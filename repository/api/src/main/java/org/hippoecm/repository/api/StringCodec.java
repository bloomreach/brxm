/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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

/**
 * Strategy interface for encoding and decoding strings.
 * <p/>
 * StringCodec objects should not retain state between calls and are therefore reusable.
 * Encoding and decoding should also be thread-safe.
 */
public interface StringCodec {

    /**
     * Encodes a string of characters.
     * @param plain the string to encode
     * @return the encoded string
     */
    public String encode(String plain);

    /**
     * Decodes a string of characters.  Some encoding strategies are one-way, in which case the decoding might return null.
     * @param encoded the previously encoded string
     * @return the decoded string or null if no decoding is possible
     */
    public String decode(String encoded);
}
