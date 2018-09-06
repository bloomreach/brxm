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

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.onehippo.cms7.services.webfiles.WebFilesService;

public class WebFileUtils {

    public static final String DEFAULT_BUNDLE_NAME = "site";

    public static String getBundleName(HstRequestContext requestContext) {
        final Mount reqMount = requestContext.getResolvedMount().getMount();
        String bundleName = reqMount.getContextPath();
        if (StringUtils.isEmpty(bundleName)) {
            //If webfile bundle name is null, use context path of parent mount as webfile bundle name
            //TODO SS: This code will be removed as hst platform changes are integrated
            bundleName = reqMount.getParent() != null ? reqMount.getParent().getContextPath() : DEFAULT_BUNDLE_NAME;
        }

        if (bundleName.startsWith("/")) {
            bundleName = bundleName.substring(1);
        }
        return bundleName;
    }

    public static String webFilePathToJcrPath(final String templateSource) {
        final String webFilePath = "/" + PathUtils.normalizePath(templateSource.substring(
                ContainerConstants.FREEMARKER_WEB_FILE_TEMPLATE_PROTOCOL.length()));
        final String bundleName = getBundleName();
        return WebFilesService.JCR_ROOT_PATH + "/" + bundleName + webFilePath;
    }

    public static String jcrPathToWebFilePath(final String variantJcrPath) {
        final String bundleName = getBundleName();
        final String requiredPrefix = WebFilesService.JCR_ROOT_PATH + "/" + bundleName + "/";
        if (!variantJcrPath.startsWith(requiredPrefix)) {
            final String msg = String.format("Cannot translate '%s' to web file path because '%s' does not start" +
                    " with '%s'", variantJcrPath, variantJcrPath, requiredPrefix);
            throw new IllegalArgumentException(msg);
        }
        return ContainerConstants.FREEMARKER_WEB_FILE_TEMPLATE_PROTOCOL + "/" + variantJcrPath.substring(requiredPrefix.length());
    }

    private static String getBundleName() {
        final HstRequestContext ctx = RequestContextProvider.get();
        if (ctx == null) {
            throw new IllegalStateException("Cannot serve freemarker template from web file because there is no HstRequestContext.");
        }
        return getBundleName(ctx);
    }

}
