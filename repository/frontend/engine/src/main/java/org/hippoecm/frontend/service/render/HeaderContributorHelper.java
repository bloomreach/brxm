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
package org.hippoecm.frontend.service.render;

import org.apache.wicket.RequestContext;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.behavior.HeaderContributor;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.protocol.http.WebRequest;
import org.apache.wicket.protocol.http.WicketURLDecoder;
import org.apache.wicket.util.string.PrependingStringBuffer;

public final class HeaderContributorHelper {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private HeaderContributorHelper() {
    }
    
    public static final HeaderContributor forCss(final String location) {
        return new HeaderContributor(new IHeaderContributor() {
            private static final long serialVersionUID = 1L;

            public void renderHead(IHeaderResponse response) {
                response.renderCSSReference(returnFixedRelativePath(location));
            }
        });
    }

    public static final HeaderContributor forJavaScript(final String location) {
        return new HeaderContributor(new IHeaderContributor() {
            private static final long serialVersionUID = 1L;

            public void renderHead(IHeaderResponse response) {
                response.renderJavascriptReference(returnFixedRelativePath(location));
            }
        });
    }

    // Adds ../ links to make the location relative to the root of the webapp,
    // provided it's not a fully-qualified URL.
    public static final String returnFixedRelativePath(String location) {
        // WICKET-59 allow external URLs, WICKET-612 allow absolute URLs.
        if (location.startsWith("http://") || location.startsWith("https://") || location.startsWith("/")) {
            return location;
        } else {
            return getFixedRelativePathPrefixToContextRoot() + location;
        }
    }

    public static final String getFixedRelativePathPrefixToContextRoot() {
        WebRequest request = (WebRequest) RequestCycle.get().getRequest();

        if (RequestContext.get().isPortletRequest()) {
            return request.getHttpServletRequest().getContextPath() + "/";
        }

        // Prepend to get back to the wicket handler.
        String tmp = RequestCycle.get().getRequest().getRelativePathPrefixToWicketHandler();
        PrependingStringBuffer prepender = new PrependingStringBuffer(tmp);

        String path = WicketURLDecoder.PATH_INSTANCE.decode(request.getPath());
        if (path == null || path.length() == 0) {
            path = "";
        }

        // Now prepend to get back from the wicket handler to the root context.

        // Find the absolute path for the wicket filter/servlet
        String wicketPath = "";

        // We're running as a filter.
        // Note: do not call RequestUtils.decode() on getServletPath ... it is
        //       already url-decoded (JIRA WICKET-1624)
        String servletPath = request.getServletPath();

        // We need to substitute the %3A (or the other way around) to be able to
        // get a good match, as parts of the path may have been escaped while
        // others arent

        // Add check if path is empty
        if (!"".equals(path) && servletPath.endsWith(path)) {
            int len = servletPath.length() - path.length() - 1;
            if (len < 0) {
                len = 0;
            }
            wicketPath = servletPath.substring(0, len);
        }
        // We're running as a servlet
        else {
            wicketPath = servletPath;
        }

        int start = 0;
        // add skip for starting slash
        if (wicketPath.startsWith("/")) {
            start = 1;
        }
        for (int i = start; i < wicketPath.length(); i++) {
            if (wicketPath.charAt(i) == '/') {
                prepender.prepend("../");
            }
        }
        return prepender.toString();
    }

}
