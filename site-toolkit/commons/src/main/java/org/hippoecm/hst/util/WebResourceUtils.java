/*
 *  Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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

import org.hippoecm.hst.core.request.HstRequestContext;

public class WebResourceUtils {

    private static final String DEFAULT_BUNDLE_NAME = "site";

    public static String getBundleName(HstRequestContext requestContext) {
        String bundleName = requestContext.getResolvedMount().getMount().getContextPath();
        if (bundleName == null || bundleName.length() == 0) {
            bundleName = DEFAULT_BUNDLE_NAME;
        } if (bundleName.startsWith("/")) {
            bundleName = bundleName.substring(1);
        }

        return bundleName;
    }
}
