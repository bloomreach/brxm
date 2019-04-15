/*
 *  Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.content.beans.dynamic;

import java.util.regex.Pattern;

import javax.annotation.Nonnull;

import org.apache.commons.lang.StringUtils;

import com.google.common.base.CharMatcher;
import com.google.common.base.Strings;

/**
 * Utility class to use in dynamic bean creation
 */
public class DynamicBeanUtils {
    public static final String PREFIX_GET = "get";
    public static final Pattern NAMESPACE_PATTERN = Pattern.compile(":");
    public static final Pattern DOT_PATTERN = Pattern.compile("\\.");
    public static final Pattern PATTERN_SPACES = Pattern.compile("\\s");
    public static final String INVALID_CLASS_NAME = "InvalidClassName";

    public static String createJavaClassName(String name) {
        if (name.indexOf(',') != -1) {
            name = name.split(",")[0];
        }
        return createClassName(name);
    }

    public static String createClassName(final String input) {
        if (Strings.isNullOrEmpty(input) || input.trim().equals(":")) {
            return INVALID_CLASS_NAME;
        }

        // remove all spaces:
        final String name = PATTERN_SPACES.matcher(input).replaceAll("");
        final int index = name.indexOf(':');
        if (index == -1 || index == name.length() - 1) {
            return StringUtils.capitalize(name.replace(':', ' ').trim());
        }
        final String[] parts = NAMESPACE_PATTERN.split(name);
        if (parts.length < 1) {
            return INVALID_CLASS_NAME;
        }
        return StringUtils.capitalize(parts[1]);
    }

    @Nonnull
    public static String createMethodName(final String name) {
        String myName = name;
        if (Strings.isNullOrEmpty(myName) || myName.trim().equals(":")) {
            throw new IllegalArgumentException(String.format("Unable to construct method name from: %s", name));
        }
        myName = CharMatcher.whitespace().removeFrom(myName);
        // replace all whitespaces:
        final int index = myName.indexOf(':');
        if (index == -1 || index == myName.length() - 1) {
            return PREFIX_GET + StringUtils.capitalize(myName.replace(':', ' ').trim());
        }
        final String[] parts = NAMESPACE_PATTERN.split(myName);
        if (parts.length < 1) {
            throw new IllegalArgumentException(String.format("Unable to construct method name from: %s", name));
        }
        return PREFIX_GET + StringUtils.capitalize(parts[1]);
    }

}
