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
package org.onehippo.cm.model.util;

import java.util.function.Predicate;

import org.apache.commons.lang3.StringUtils;

import static org.onehippo.cm.model.Constants.FILE_NAME_EXT_SEPARATOR;
import static org.onehippo.cm.model.Constants.FILE_NAME_SEQ_PREFIX;

public class FileConfigurationUtils {

    /**
     * Use a simple numbering convention to generate a unique path given a candidate path and a test of uniqueness.
     *
     * @param candidate   a path that might be used directly, if it is already unique, or modified to create a unique
     *                    path
     * @param isNotUnique a test of uniqueness for a proposed path
     * @param sequence    the number of the current attempt, used for loop control -- callers typically pass 0
     * @return a path, based on candidate, that is unique according to isNotUnique
     */
    public static String generateUniquePath(final String candidate, Predicate<String> isNotUnique, int sequence) {

        String name = StringUtils.substringBeforeLast(candidate, FILE_NAME_EXT_SEPARATOR);
        String extension = StringUtils.substringAfterLast(candidate, FILE_NAME_EXT_SEPARATOR);

        final String newName = name + calculateNameSuffix(sequence) + (!StringUtils.isEmpty(extension) ? FILE_NAME_EXT_SEPARATOR + extension : StringUtils.EMPTY);
        return isNotUnique.test(newName) ? generateUniquePath(candidate, isNotUnique, sequence + 1) : newName;
    }

    /**
     * Helper for {@link #generateUniquePath(String, Predicate, int)}.
     */
    private static String calculateNameSuffix(int sequence) {
        return sequence == 0 ? StringUtils.EMPTY : FILE_NAME_SEQ_PREFIX + Integer.toString(sequence);
    }
}
