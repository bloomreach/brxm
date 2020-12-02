/*
 * Copyright 2020 Hippo B.V. (http://www.onehippo.com)
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
 *
 */

package org.hippoecm.hst.platform.utils;

import java.util.regex.Pattern;

public final class UUIDUtils {

    static final Pattern UUID_PATTERN = Pattern.compile(
            "^[\\p{XDigit}]{8}-[\\p{XDigit}]{4}-[\\p{XDigit}]{4}-[\\p{XDigit}]{4}-[\\p{XDigit}]{12}$");

    private UUIDUtils() {
    }

    public static boolean isValidUUID(String value) {
        return value != null
                && UUID_PATTERN.matcher(value).matches();
    }
}
