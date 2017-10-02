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

import java.util.HashSet;
import java.util.Set;

import org.onehippo.cm.model.util.FilePathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Unique file name generator
 */
public class ResourceNameResolverImpl implements ResourceNameResolver {

    private static final Logger log = LoggerFactory.getLogger(ResourceNameResolver.class);

    private Set<String> knownFileEntries = new HashSet<>();

    @Override
    public String generateName(final String filePath) {
        final String generatedPath = FilePathUtils.generateUniquePath(filePath,
                entry -> knownFileEntries.stream().anyMatch(entry::equalsIgnoreCase), 0);
        knownFileEntries.add(generatedPath);
        return generatedPath;
    }

    @Override
    public void seedName(final String filePath) {
        if (!knownFileEntries.add(filePath)) {
            log.warn("Resource file path {} already seeded before within this module context which indicates a cross-linked resource path", filePath);
        }
    }

    @Override
    public void clear() {
        knownFileEntries.clear();
    }
}
