/*
 *  Copyright 2008-2009 Hippo.
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
 * <h2>Helper class for encoding and decoding (the localname of) Qualified names</h2>
 * @deprecated use StringCodec instead
 */
public class ISO9075Helper {
    @SuppressWarnings("unused")
    private static final String SVN_ID = "$Id$";

    private ISO9075Helper() {};

    public static String encodeLocalName(String name) {
        return new StringCodecFactory.ISO9075Helper().encode(name);
    }

    public static String decodeLocalName(String name) {
        return new StringCodecFactory.ISO9075Helper().decode(name);
    }

    /** @deprecrated without replacement */
    public static String encodeColon(String name) {
        return StringCodecFactory.ISO9075Helper.encodeColon(name);
    }

   /** @deprecrated without replacement */
    public static String decodeColon(String name) {
        return StringCodecFactory.ISO9075Helper.decodeColon(name);
    }
}
