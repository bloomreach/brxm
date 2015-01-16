/*
 *  Copyright 2014-2015 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.util;

import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.onehippo.cms7.services.webresources.WebResourcesService;

public class WebResourceUtils {

    public static final String DEFAULT_BUNDLE_NAME = "site";

    public static String getBundleName(HstRequestContext requestContext) {
        String bundleName = requestContext.getResolvedMount().getMount().getContextPath();
        if (bundleName == null || bundleName.length() == 0) {
            bundleName = DEFAULT_BUNDLE_NAME;
        } else if (bundleName.startsWith("/")) {
            bundleName = bundleName.substring(1);
        }
        return bundleName;
    }

    public static String webResourcePathToJcrPath(final String templateSource) {
        String webResourcePath = "/" + PathUtils.normalizePath(templateSource.substring(
                ContainerConstants.FREEMARKER_WEBRESOURCE_TEMPLATE_PROTOCOL.length()));
        final String bundleName = getBundleName();
        return WebResourcesService.JCR_ROOT_PATH + "/" + bundleName + webResourcePath;
    }

    public static String jcrPathToWebResourcePath(final String variantJcrPath) {
        final String bundleName = getBundleName();
        final String requiredPrefix = WebResourcesService.JCR_ROOT_PATH + "/" + bundleName + "/";
        if (!variantJcrPath.startsWith(requiredPrefix)) {
            String msg = String.format("Cannot translate '%s' to web resouce path because '%s' does not start" +
                    " with '%s'", variantJcrPath, variantJcrPath, requiredPrefix);
            throw new IllegalArgumentException(msg);
        }
        return ContainerConstants.FREEMARKER_WEBRESOURCE_TEMPLATE_PROTOCOL + "/" + variantJcrPath.substring(requiredPrefix.length());
    }

    private static String getBundleName() {
        final HstRequestContext ctx = RequestContextProvider.get();
        if (ctx == null) {
            String msg = String.format("Cannot serve freemarker template from webresource because there is no HstRequestContext.");
            throw new IllegalStateException(msg);
        }
        return getBundleName(ctx);
    }

}
