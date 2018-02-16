/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.cms7.utilities.io;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;

public class FilePathUtils {
    protected static final String DEFAULT_FILE_NAME = "data";

    /**
     * Call {@link #cleanFileName} on each segment of the given file path candidate.
     * @param path a file path candidate, using UNIX-style forward-slash conventions
     * @return a file path that avoids common problems for file/dir naming on Windows
     * @throws IllegalArgumentException if path == null
     */
    public static String cleanFilePath(String path) {
        if (path == null) {
            throw new IllegalArgumentException("file path candidate must not be null");
        }

        return Arrays.stream(path.split("/"))
                .map(FilePathUtils::cleanFileName)
                .collect(Collectors.joining("/"));
    }

    /**
     * A (too) simple function to detect common problems with generated file names, mostly derived from restrictions
     * on Windows, defined <a href="https://msdn.microsoft.com/en-us/library/aa365247">here.</a>
     * <ol>
     *     <li>replace control chars with '-': <>:;"/\|?* and ASCII 0-31</li>
     *     <li>trim whitespace, period, and '-' from both sides</li>
     *     <li>replace Windows-reserved names with "data": CON, PRN, AUX, NUL, COM1, COM2, COM3, COM4, COM5, COM6, COM7, COM8, COM9, LPT1, LPT2, LPT3, LPT4, LPT5, LPT6, LPT7, LPT8, and LPT9</li>
     * </ol>
     * Note that this method will strip a leading period from an "extension-only" hidden UNIX file name, such as
     * ".gitconfig", since this is not normally allowed for a user-visible file name.
     * @param name input candidate file name
     * @return a file name that avoids these common problems for Windows file names
     * @throws IllegalArgumentException if name == null
     */
    public static String cleanFileName(String name) {
        if (name == null) {
            throw new IllegalArgumentException("file name candidate must not be null");
        }

        // replace naughty chars
        name = name.replaceAll("[\\x00-\\x20<>:;\"/|?*\\\\]+", "-");

        // trim whitespace, period, and dash from start and end
        name = StringUtils.strip(name, "-.\t\n\u000B\f\r");

        // if we've got nothing left, use a default stand-in value
        if (StringUtils.isBlank(name)) {
            name = DEFAULT_FILE_NAME;
        }

        // don't allow special reserved Windows port names
        switch (name) {
            case "CON":
            case "PRN":
            case "AUX":
            case "NUL":
            case "COM1":
            case "COM2":
            case "COM3":
            case "COM4":
            case "COM5":
            case "COM6":
            case "COM7":
            case "COM8":
            case "COM9":
            case "LPT1":
            case "LPT2":
            case "LPT3":
            case "LPT4":
            case "LPT5":
            case "LPT6":
            case "LPT7":
            case "LPT8":
            case "LPT9":
                name = DEFAULT_FILE_NAME;
        }
        return name;
    }
}
