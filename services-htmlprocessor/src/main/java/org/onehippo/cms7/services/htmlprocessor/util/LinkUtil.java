/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.services.htmlprocessor.util;

import java.util.regex.Pattern;

public class LinkUtil {

    public static String INTERNAL_LINK_DEFAULT_HREF = "http://";
    public static Pattern EXTERNAL_LINK_HREF_PATTERN = Pattern.compile("^(#|/|[a-z][a-z0-9+-.]*:)", Pattern.CASE_INSENSITIVE);

    /**
     * @return true if the href value is an external link. The default internal link href value should not be handled
     * as external link.
     */
    public static boolean isExternalLink(final String href) {
        return !INTERNAL_LINK_DEFAULT_HREF.equals(href) && EXTERNAL_LINK_HREF_PATTERN.matcher(href).find();
    }
}
