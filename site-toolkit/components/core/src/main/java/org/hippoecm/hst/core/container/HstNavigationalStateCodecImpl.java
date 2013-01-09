/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.core.container;

import java.io.UnsupportedEncodingException;

import org.apache.commons.codec.binary.Base64;

/**
 * HstNavigationalStateCodecImpl
 * 
 * @version $Id$
 */
public class HstNavigationalStateCodecImpl implements HstNavigationalStateCodec {

    public String encodeParameters(String value, String characterEncoding) throws UnsupportedEncodingException {
        if (characterEncoding != null) {
            value = new String(Base64.encodeBase64(value.getBytes(characterEncoding)));
        } else {
            value = new String(Base64.encodeBase64(value.getBytes()));
        }
        
        return value.replace('/', '-').replace('=', '_').replace('+', '.');
    }

    public String decodeParameters(String value, String characterEncoding) throws UnsupportedEncodingException {
        value = value.replace('-', '/').replace('_', '=').replace('.','+');
        
        if (characterEncoding != null) {
            return new String(Base64.decodeBase64(value.getBytes(characterEncoding)), characterEncoding);
        } else {
            return new String(Base64.decodeBase64(value.getBytes()));
        }
    }
}
