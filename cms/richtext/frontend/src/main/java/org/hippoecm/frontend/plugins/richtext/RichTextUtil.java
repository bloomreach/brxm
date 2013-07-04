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
package org.hippoecm.frontend.plugins.richtext;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.util.encoding.UrlDecoder;
import org.apache.wicket.util.encoding.UrlEncoder;

public class RichTextUtil {

    public static final String encode(String path) {
        String[] elements = StringUtils.split(path, '/');
        for (int i = 0; i < elements.length; i++) {
            elements[i] = UrlEncoder.PATH_INSTANCE.encode(elements[i], "UTF-8");
        }
        return StringUtils.join(elements, '/');
    }

    public static final String decode(String path) {
        String[] elements = StringUtils.split(path, '/');
        for (int i = 0; i < elements.length; i++) {
            elements[i] = UrlDecoder.PATH_INSTANCE.decode(elements[i], "UTF-8");
        }
        return StringUtils.join(elements, '/');
    }

}
