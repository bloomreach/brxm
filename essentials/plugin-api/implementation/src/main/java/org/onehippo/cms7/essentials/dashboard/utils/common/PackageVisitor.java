/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.dashboard.utils.common;

import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @version "$Id: PackageVisitor.java 164013 2013-05-11 14:05:39Z mmilicevic $"
 */
public class PackageVisitor extends SimpleFileVisitor<Path> {

    private boolean first = true;
    private Map<Path, String> packageMap = new HashMap<>();

    @Override
    public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) {
        final String name = dir.getFileName().toString();
        // skip hidden files:
        if (name.startsWith(".")) {
            return FileVisitResult.SKIP_SUBTREE;
        }

        if (first) {
            first = false;
            // skip root, java folder
            return FileVisitResult.CONTINUE;
        }
        // get parent package:
        final Path parent = dir.getParent();
        if (packageMap.containsKey(parent)) {
            final String parentPackage = packageMap.get(parent);
            final String packageName = parentPackage + '.' + name;
            packageMap.put(dir, packageName);
        } else {
            // root package:
            packageMap.put(dir, name);
        }
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) {
        // skip files
        return FileVisitResult.CONTINUE;
    }

    public Collection<String> getPackages() {
        return packageMap.values();

    }
}
