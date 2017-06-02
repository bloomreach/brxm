/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cm.model.serializer;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.onehippo.cm.model.util.FileConfigurationUtils;

/**
 * Unique file name generator
 */
public class ResourceNameResolverImpl implements ResourceNameResolver {

    private static class FileEntry {

        private final String path;
        private final String basename;
        private final String extension;

        FileEntry(String finalPath) {
            Path filePath = Paths.get(finalPath);
            path = filePath.getParent().toString();
            String filename = filePath.getFileName().toString();
            basename = StringUtils.substringBeforeLast(filename, SEPARATOR);
            extension = StringUtils.substringAfterLast(filename, SEPARATOR);
        }

        public String getPath() {
            return path;
        }

        public String getExtension() {
            return extension;
        }

        public String getFileName() {
            return basename + (!StringUtils.isEmpty(extension) ? SEPARATOR + extension : StringUtils.EMPTY);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof FileEntry)) return false;
            FileEntry fileEntry = (FileEntry) o;
            return Objects.equals(path, fileEntry.path) &&
                    Objects.equals(basename, fileEntry.basename) &&
                    Objects.equals(extension, fileEntry.extension);
        }

        @Override
        public int hashCode() {
            return Objects.hash(path, basename, extension);
        }
    }


    public static final String SEPARATOR = ".";
    public static final String PATH_DELIMITER = "/";
    public static final String SEQ_PREFIX = "-";

    private Set<FileEntry> knownFileEntries = new HashSet<>();

    /**
     * Generates file unique name and add it to known list along with its full path.
     * @param filePath
     * @return unique filename within already known paths
     */
    @Override
    public String generateName(String filePath) {

        final String folderPath = filePath.toLowerCase().substring(0, filePath.lastIndexOf(PATH_DELIMITER));
        final String filename = StringUtils.substringAfterLast(filePath, PATH_DELIMITER);

        final Set<String> knownFiles = knownFileEntries.stream().filter(p -> p.getPath().toLowerCase().equals(folderPath.toLowerCase()))
                .map(FileEntry::getFileName).collect(Collectors.toSet());

        final String generatedName = FileConfigurationUtils.generateUniquePath(filename, knownFiles::contains, 0);

        final String finalPath = Paths.get(folderPath, generatedName).toString();

        final FileEntry fileEntry = new FileEntry(finalPath);
        knownFileEntries.add(fileEntry);
        return finalPath;
    }

}
