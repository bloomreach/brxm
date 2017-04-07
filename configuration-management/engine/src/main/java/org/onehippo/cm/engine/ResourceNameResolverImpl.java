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
package org.onehippo.cm.engine;

import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class ResourceNameResolverImpl implements ResourceNameResolver {


    public static final String PATH_DELIMITER = "/";
    public static final String SEQ_ARRAY_PREFIX = "[";
    public static final String SEQ_ARRAY_SUFFIX = "]";
    public static final String SEQ_PREFIX = "-";

    private Set<String> knownPaths = new HashSet<>();

    @Override
    public String generateName(String filePath) {
        final String folderPath = filePath.toLowerCase().substring(0, filePath.lastIndexOf(PATH_DELIMITER) + 1);
        String filename = extractFilenameFromFullPath(filePath);

        final Set<String> knownFiles = knownPaths.stream()
                .filter(p -> p.toLowerCase().startsWith(folderPath.toLowerCase()))
                .map(this::extractFilenameFromFullPath).collect(Collectors.toSet());

        final String generatedName = generateUniqueName(filename, knownFiles, 0);

        final String finalPath = folderPath + generatedName;
        knownPaths.add(finalPath);
        return finalPath;
    }

    private String generateUniqueName(String candidate, Collection<String> knownFiles, int sequence) {

        final String newName = candidate + calculateNameSuffix(sequence);
        return knownFiles.contains(newName) ? generateUniqueName(candidate, knownFiles, sequence + 1) : newName;
    }

    private String calculateNameSuffix(int sequence) {
        return sequence == 0 ? StringUtils.EMPTY : SEQ_PREFIX + Integer.toString(sequence);
    }

    private String extractFilenameFromFullPath(String filePath) {
        return filePath.substring(filePath.lastIndexOf(PATH_DELIMITER) + 1);
    }
}
