/*
 *  Copyright 2008 Hippo.
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
import org.apache.wicket.RequestContext;
import org.apache.wicket.protocol.http.WicketURLDecoder;
import org.apache.wicket.protocol.http.WicketURLEncoder;
import org.apache.wicket.protocol.http.portlet.PortletRequestContext;

public class RichTextUtil {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    public static final String encode(String path) {
        String[] elements = StringUtils.split(path, '/');
        for (int i = 0; i < elements.length; i++) {
            elements[i] = WicketURLEncoder.PATH_INSTANCE.encode(elements[i], "UTF-8");
        }
        return StringUtils.join(elements, '/');
    }

    public static final String decode(String path) {
        String[] elements = StringUtils.split(path, '/');
        for (int i = 0; i < elements.length; i++) {
            elements[i] = WicketURLDecoder.PATH_INSTANCE.decode(elements[i], "UTF-8");
        }
        return StringUtils.join(elements, '/');
    }

    public static final String encodeResourceURL(String url) {
        // if it is in portlet environment, just wrap the url in portlet resource url.
        RequestContext requestContext = RequestContext.get();
        if (requestContext.isPortletRequest()) {
            url = ((PortletRequestContext) requestContext).encodeResourceURL(url).toString();
            // use relative url (FIXME: why?)
            if (url.startsWith("http:") || url.startsWith("https:")) {
                int offset = url.indexOf('/', 8);
                if (offset != -1) {
                    url = url.substring(offset);
                }
            }
        }
        return url;
    }

}
