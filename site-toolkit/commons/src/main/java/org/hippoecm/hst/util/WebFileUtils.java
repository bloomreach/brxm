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

import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.onehippo.cms7.services.webfiles.WebFilesService;

public class WebFileUtils {

    public static String getBundleName(HstRequestContext requestContext) {
        final Mount reqMount = requestContext.getResolvedMount().getMount();
        String bundleName = reqMount.getContextPath();
        if (bundleName.startsWith("/")) {
            bundleName = bundleName.substring(1);
        }
        return bundleName;
    }

    /**
     *
     * @param templateSource
     * @param bundleName the bundleName to use, which should not start with a '/'
     * @return The repository path to for the {@code templateSource} where in the path is prefixed with
     * {@link WebFilesService#JCR_ROOT_PATH} followed by the bundleName (typically the contextPath of the webfile for
     * a specific site webapp)
     */
    // RD-4598 TODO what if the bundleName is empty? Do we still support ROOT.wat deployment for a site?
    public static String webFilePathToJcrPath(final String templateSource, final String bundleName) {
        final String webFilePath = "/" + PathUtils.normalizePath(templateSource.substring(
                ContainerConstants.FREEMARKER_WEB_FILE_TEMPLATE_PROTOCOL.length()));
        if (bundleName.isEmpty()) {
            return WebFilesService.JCR_ROOT_PATH + webFilePath;
        } else {
            return WebFilesService.JCR_ROOT_PATH + "/" + bundleName + webFilePath;
        }
    }

    public static String jcrPathToWebFilePath(final String variantJcrPath, final String bundleName) {
        final String requiredPrefix;
        if (bundleName.isEmpty()) {
            requiredPrefix = WebFilesService.JCR_ROOT_PATH + "/";
        } else {
            requiredPrefix = WebFilesService.JCR_ROOT_PATH + "/" + bundleName + "/";
        }
        if (!variantJcrPath.startsWith(requiredPrefix)) {
            final String msg = String.format("Cannot translate '%s' to web file path because '%s' does not start" +
                    " with '%s'", variantJcrPath, variantJcrPath, requiredPrefix);
            throw new IllegalArgumentException(msg);
        }
        return ContainerConstants.FREEMARKER_WEB_FILE_TEMPLATE_PROTOCOL + "/" + variantJcrPath.substring(requiredPrefix.length());
    }

}
