/*
 *  Copyright 2015-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.usagestatistics;


import java.security.MessageDigest;

import javax.xml.bind.DatatypeConverter;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import org.hippoecm.frontend.session.UserSession;

public class UsageStatisticsUtils {

    private static final LoadingCache<String, String> CACHE = CacheBuilder.newBuilder()
            .maximumSize(50)
            .build(new CacheLoader<String, String>() {
                @Override
                public String load(final String key) throws Exception {
                    final MessageDigest md5 = MessageDigest.getInstance("MD5");
                    md5.update(key.getBytes());
                    return DatatypeConverter.printHexBinary(md5.digest()).toLowerCase();

                }
            });

    /**
     * Utility method that manages encrypted values of frequently used Strings. Input values are transformed into a 32
     * character hex string using a MD5 digest. For speedy retrieval of frequently used strings, new values are cached
     * in a LoadingCache.
     *
     * @param plain String to be used to calculate the MD5 digest
     * @return A 32 character hex string of the MD5 digest
     */
    public static String encryptParameterValue(final String plain) {
        return CACHE.getUnchecked(plain);
    }

    public static String getLanguage() {
        return UserSession.get().getLocale().getLanguage();
    }
}
