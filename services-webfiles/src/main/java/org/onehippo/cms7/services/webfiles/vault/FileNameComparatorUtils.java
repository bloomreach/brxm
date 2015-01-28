/*
 * Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.services.webfiles.vault;

import java.io.File;
import java.util.Comparator;

import org.apache.commons.io.FilenameUtils;

public class FileNameComparatorUtils {

    private FileNameComparatorUtils() {

    }

    static final Comparator<File> FILE_BASE_NAME_COMPARATOR = new Comparator<File>() {
        @Override
        public int compare(final File f1, final File f2) {
            final String fileName1 = f1.getName();
            final String fileName2 = f2.getName();
            return BASE_NAME_COMPARATOR.compare(fileName1, fileName2);

        }
    };

    static final Comparator<String> BASE_NAME_COMPARATOR = new Comparator<String>() {
        @Override
        public int compare(final String fileName1, final String fileName2) {
            final String baseName1 = FilenameUtils.getBaseName(fileName1);
            final String baseName2 = FilenameUtils.getBaseName(fileName2);
            int compare = baseName1.compareTo(baseName2);
            if (compare != 0) {
                return compare;
            }
            return FilenameUtils.getExtension(fileName1).compareTo(FilenameUtils.getExtension(fileName2));
        }
    };


}
