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

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.Path;
import java.util.function.Predicate;

import org.apache.commons.lang3.StringUtils;

import static org.onehippo.cm.model.Constants.FILE_NAME_EXT_SEPARATOR;
import static org.onehippo.cm.model.Constants.FILE_NAME_SEQ_PREFIX;

/**
 * Static utility class -- not intended to be instantiated.
 */
public abstract class FilePathUtils {

    private static final boolean UNIXFS = File.separator.equals("/");

    /**
     * @return true iff the local File.separator is a UNIX-style forward-slash
     */
    public static boolean isUnixFs() {
        return UNIXFS;
    }

    /**
     * @param unixPath a UNIX-style path with forward-slash separators
     * @return a path that matches native style with either forward- or back-slashes for separators
     */
    public static String nativePath(final String unixPath) {
        return UNIXFS ? unixPath : unixPath.replace('/', '\\');
    }

    /**
     * @param nativePath a path that matches native style with either forward- or back-slashes for separators
     * @return a UNIX-style path with forward-slash separators
     */
    public static String unixPath(final String nativePath) {
        return UNIXFS ? nativePath : nativePath.replace('\\', '/');
    }

    /**
     * Returns a Path parent, or in case of root Path entry in a ZipFileSystem the ZipFileSystem 'root' path, as in that
     * case calling {@link Path#getParent()} will return null.
     * @param path the Path for which to get or derive the parent Path
     * @return either the parent of the given path or the root of the Path's FileSystem, if path.getParent() == null
     */
    public static Path getParentOrFsRoot(final Path path) {
        final Path parent = path.getParent();
        return parent != null ? parent : path.getFileSystem().getPath("/");
    }

    /**
     * Resource or File URLs like for example as returned from {@link Class#getResource(String)} have URL encoded spaces
     * (%20) which for a native file path needs to be decoded back to a proper space.
     * This decoding should <em>NOT</em> be done using {@link URLDecoder#decode(String, String)} as that would also
     * <em>incorrectly</em> decode special characters like '+' into spaces!
     * Furthermore, Windows file paths will be encoded in a non-native way, like /C/foo.txt instead of C:\foo.txt
     * <p>
     * To properly convert these File URLs into native File paths (String), the File class should be used as
     * an intermediate step by first converting the URL to an URI, then using {@link File#File(URI)} to create a platform
     * native File, and finally return its platform native {@link File#getPath()}.
     * </p>
     * @param fileURL a resource URL using the file:/ protocol
     * @return the platform native File path
     * @throws IllegalArgumentException when the URL cannot be converted to an proper/valid URI
     */
    public static String getNativeFilePath(final URL fileURL) {
        try {
            return new File(fileURL.toURI()).getPath();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Use a simple numbering convention to generate a unique path given a candidate path and a test of uniqueness.
     *
     * @param candidate   a path that might be used directly, if it is already unique, or modified to create a unique
     *                    path
     * @param isNotUnique a test of uniqueness for a proposed path
     * @param sequence    the number of the current attempt, used for loop control -- callers typically pass 0
     * @return a path, based on candidate, that is unique according to isNotUnique
     */
    public static String generateUniquePath(final String candidate, final Predicate<String> isNotUnique, final int sequence) {

        String name = StringUtils.substringBeforeLast(candidate, FILE_NAME_EXT_SEPARATOR);
        String extension = StringUtils.substringAfterLast(candidate, FILE_NAME_EXT_SEPARATOR);

        final String newName = name + calculateNameSuffix(sequence) + (!StringUtils.isEmpty(extension) ? FILE_NAME_EXT_SEPARATOR + extension : StringUtils.EMPTY);
        return isNotUnique.test(newName) ? generateUniquePath(candidate, isNotUnique, sequence + 1) : newName;
    }

    /**
     * Helper for {@link #generateUniquePath(String, Predicate, int)}.
     */
    private static String calculateNameSuffix(final int sequence) {
        return sequence == 0 ? StringUtils.EMPTY : FILE_NAME_SEQ_PREFIX + Integer.toString(sequence);
    }
}
