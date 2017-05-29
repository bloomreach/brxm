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
package org.hippoecm.frontend.plugins.richtext.htmlprocessor;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.request.cycle.RequestCycle;
import org.onehippo.cms7.services.htmlprocessor.richtext.URLEncoder;

public class WicketURLEncoder implements URLEncoder {

    public static final URLEncoder INSTANCE = new WicketURLEncoder();

    private WicketURLEncoder() {}

    @Override
    public String encode(final String url) {
        String[] elements = StringUtils.split(url, '/');
        for (int i = 0; i < elements.length; i++) {
            elements[i] = org.apache.wicket.util.encoding.UrlEncoder.PATH_INSTANCE.encode(elements[i], "UTF-8");
        }

        final String encodedUrl = StringUtils.join(elements, '/');
        RequestCycle requestCycle = RequestCycle.get();
        return requestCycle != null ? requestCycle.getResponse().encodeURL(encodedUrl).toString() : encodedUrl;
    }
}
