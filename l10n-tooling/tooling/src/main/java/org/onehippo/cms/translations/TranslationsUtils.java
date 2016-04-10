/*
 *  Copyright 2016-2016 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms.translations;

import org.apache.commons.lang.StringUtils;

import static org.onehippo.cms.translations.BundleType.REPOSITORY;

public final class TranslationsUtils {
    
    private TranslationsUtils() {}
    
    public static String getLocalizedBundleFileName(String sourceBundleFileName, BundleType bundleType, String locale) {
        switch (bundleType) {
            case REPOSITORY: {
                return StringUtils.substringBefore(sourceBundleFileName, ".json") + "_" + locale + ".json";
            }
            case WICKET: {
                return sourceBundleFileName;
            }
            case ANGULAR: {
                return StringUtils.substringBeforeLast(sourceBundleFileName, "/") + "/" + locale + ".json";
            }
        }
        throw new IllegalArgumentException("Unknown bundle type: " + bundleType);
    }
    
    public static String registryKey(final ResourceBundle resourceBundle, final String bundleKey) {
        return registryKeyPrefix(resourceBundle) + bundleKey;
    }
    
    public static String registryKeyPrefix(final ResourceBundle resourceBundle) {
        if (resourceBundle.getType() == REPOSITORY) {
            return resourceBundle.getName() + "/";
        }
        return "";
        
    }
    
}
