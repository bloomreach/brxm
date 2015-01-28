/*
 * Copyright 2014-2015 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.services.webfiles.watch;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.StringUtils;

/**
 * Mock implementation of {@link Path} that mimics paths with a specific path separator.
 */
class MockPath implements Path {

    private final String[] nameElements;
    private final char separator;

    MockPath(final String path, char separator) {
        nameElements = StringUtils.split(path, separator);
        this.separator = separator;
    }

    @Override
    public int getNameCount() {
        return nameElements.length;
    }

    @Override
    public Path getName(final int index) {
        return new MockPath(nameElements[index], separator);
    }

    @Override
    public Path subpath(final int beginIndex, final int endIndex) {
        final List<String> subNames = Arrays.asList(nameElements).subList(beginIndex, endIndex);
        return new MockPath(StringUtils.join(subNames, separator), separator);
    }

    @Override
    public Path relativize(final Path other) {
        final String thisPath = toString();
        final String otherPath = other.toString();
        if (!StringUtils.startsWith(otherPath, thisPath)) {
            throw new IllegalArgumentException("cannot relativize " + otherPath + " against " + thisPath);
        }
        final String relPath = StringUtils.removeStart(otherPath, thisPath);
        return new MockPath(StringUtils.removeStart(relPath, Character.toString(separator)), separator);
    }

    @Override
    public String toString() {
        return StringUtils.join(Arrays.asList(nameElements), separator);
    }

    @Override
    public Iterator<Path> iterator() {
        List<Path> paths = new ArrayList(nameElements.length);
        for (String nameElement : nameElements) {
            paths.add(new MockPath(nameElement, separator));
        }
        return paths.iterator();
    }

    @Override
    public File toFile() {
        return new File(toString());
    }

    // Remaining method are not implemented

    @Override
    public FileSystem getFileSystem() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isAbsolute() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Path getRoot() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Path getFileName() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Path getParent() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean startsWith(final Path other) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean startsWith(final String other) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean endsWith(final Path other) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean endsWith(final String other) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Path normalize() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Path resolve(final Path other) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Path resolve(final String other) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Path resolveSibling(final Path other) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Path resolveSibling(final String other) {
        throw new UnsupportedOperationException();
    }

    @Override
    public URI toUri() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Path toAbsolutePath() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Path toRealPath(final LinkOption... options) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public WatchKey register(final WatchService watcher, final WatchEvent.Kind<?>[] events, final WatchEvent.Modifier... modifiers) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public WatchKey register(final WatchService watcher, final WatchEvent.Kind<?>... events) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public int compareTo(final Path other) {
        throw new UnsupportedOperationException();
    }
}
