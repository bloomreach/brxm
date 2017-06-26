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
import java.nio.file.Path;

public class FilePathUtils {

    public static final boolean UNIXFS = File.separator.equals("/");

    public static boolean isUnixFs() {
        return UNIXFS;
    }

    public static String nativePath(final String path) {
        return UNIXFS ? path : path.replace('/', '\\');
    }

    public static String unixPath(final String path) {
        return UNIXFS ? path : path.replace('\\', '/');
    }

    public static Path getParentSafely(final Path path) {
        final Path parent = path.getParent();
        return parent != null ? parent : path.getFileSystem().getPath("/");
    }
}
