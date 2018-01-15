/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cms7.essentials.dashboard.utils;

import java.util.Locale;

import org.apache.commons.lang.StringUtils;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.repository.l10n.LocalizationService;
import org.onehippo.repository.l10n.ResourceBundle;

/**
 * Utility methods for loading values from the repository's "translations" resource bundles.
 *
 * Note that Essentials only supports English, so we hard-code the locale to English.
 */
public class LocalizationUtils {
    private static final String HIPPO_TYPES = "hippo:types";
    private static final String JCR_NAME = "jcr:name";
    private static final Locale LOCALE = Locale.ENGLISH;

    /**
     * Retrieve the "localized" display name for a content type.
     *
     * @param jcrName JCR name of the content type
     * @return "localized" name, or JCR name if no localization is available
     */
    public static String getContentTypeDisplayName(final String jcrName) {
        final String fullName = jcrName.contains(":") ? jcrName : "hipposys:" + jcrName;
        final String bundleName = HIPPO_TYPES + "." + fullName;
        final LocalizationService service = HippoServiceRegistry.getService(LocalizationService.class);
        if (service != null) {
            final ResourceBundle bundle = service.getResourceBundle(bundleName, LOCALE);
            if (bundle != null) {
                final String displayName = bundle.getString(JCR_NAME);
                if (!StringUtils.isBlank(displayName)) {
                    return bundle.getString(JCR_NAME);
                }
            }
        }
        return jcrName;
    }
}
