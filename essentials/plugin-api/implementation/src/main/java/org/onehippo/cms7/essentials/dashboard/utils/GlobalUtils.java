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

package org.onehippo.cms7.essentials.dashboard.utils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.io.IOUtils;
import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.HippoRepositoryFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.CharMatcher;
import com.google.common.base.Charsets;
import com.google.common.base.Strings;

/**
 * @version "$Id$"
 */
public final class GlobalUtils {

    private static final String PREFIX_GET = "get";
    private static final Pattern NAMESPACE_PATTERN = Pattern.compile(":");
    private static final Pattern DOT_PATTERN = Pattern.compile("\\.");
    private static Logger log = LoggerFactory.getLogger(GlobalUtils.class);

    private GlobalUtils() {
    }

    /**
     * * Replaces {@code #placeholder#} variable with string replacement provided
     *
     * @param input       input string
     * @param placeholder placeholder name
     * @param replacement replacement string
     * @return input string replaced with replacements
     */
    public static String replacePlaceholders(final String input, final String placeholder, final String replacement) {
        if (Strings.isNullOrEmpty(input)) {
            return "";
        }

        final StringBuffer buffer = new StringBuffer(input);
        try {
            final Pattern pattern = Pattern.compile("(#" + placeholder + "#)", Pattern.MULTILINE);
            Matcher matcher = pattern.matcher(buffer);
            while (matcher.find()) {
                buffer.replace(matcher.start(), matcher.end(), replacement);
            }
        } catch (Exception ignore) {
            //ignore
        }
        return buffer.toString();
    }

    public static String readStreamAsText(final InputStream stream) {
        try {
            if (stream == null) {
                return null;
            }
            return IOUtils.toString(stream);
        } catch (IOException e) {
            log.error("Error reading files", e);
        } finally {
            IOUtils.closeQuietly(stream);
        }

        return "";
    }

    public static void populateDirectories(final Path startPath, final List<Path> existing) {
        existing.add(startPath);
        try (final DirectoryStream<Path> stream = Files.newDirectoryStream(startPath)) {
            for (Path path : stream) {
                if (Files.isDirectory(path)) {
                    populateDirectories(path, existing);
                }
            }
        } catch (IOException e) {
            log.error("", e);
        }

    }

    /**
     * Read text file content (UTF-8)
     *
     * @param path path to read from
     * @return StringBuilder instance containing file content.
     */
    public static StringBuilder readTextFile(final Path path) {
        final StringBuilder builder = new StringBuilder();
        try {
            final List<String> strings = Files.readAllLines(path, Charsets.UTF_8);
            for (String string : strings) {
                builder.append(string).append(System.getProperty("line.separator"));
            }
        } catch (IOException e) {
            log.error("Error reading source file", e);
        }
        return builder;
    }

    /**
     * Write text content to a file (UTF-8)
     *
     * @param content text content
     * @param path    path to save file to
     */
    public static void writeToFile(final CharSequence content, final Path path) {
        try (BufferedWriter writer = Files.newBufferedWriter(path, Charsets.UTF_8)) {
            writer.append(content);
            writer.flush();
        } catch (IOException e) {
            log.error("Error writing file {}", e);
        }
    }

    /**
     * Creates method name for namespaced property e.g. {@code "myproject:myporperty"}
     *
     * @param name
     * @return
     */
    public static String createMethodName(final String name) {
        String myName = name;
        if (Strings.isNullOrEmpty(myName) || myName.trim().equals(":")) {
            return EssentialConst.INVALID_METHOD_NAME;
        }
        myName = CharMatcher.WHITESPACE.removeFrom(myName);
        // replace all whitespaces:
        final int index = myName.indexOf(':');
        if (index == -1 || index == myName.length() - 1) {
            return PREFIX_GET + capitalize(myName.replace(':', ' ').trim());
        }
        final String[] parts = NAMESPACE_PATTERN.split(myName);
        if (parts.length < 1) {
            return EssentialConst.INVALID_METHOD_NAME;
        }
        return PREFIX_GET + capitalize(parts[1]);
    }

    private static String capitalize(final String name) {
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }

    public static String createClassName(final String name) {
        if (Strings.isNullOrEmpty(name) || name.trim().equals(":")) {
            return EssentialConst.INVALID_CLASS_NAME;
        }
        final int index = name.indexOf(':');
        if (index == -1 || index == name.length() - 1) {
            return capitalize(name.replace(':', ' ').trim());
        }
        final String[] parts = NAMESPACE_PATTERN.split(name);
        if (parts.length < 1) {
            return EssentialConst.INVALID_CLASS_NAME;
        }
        return capitalize(parts[1]);
    }


    /**
     * @param fullName e.g. {@code myproject:foo}
     * @return prefix e.g. {@code myproject}
     */
    public static String getNamespacePrefix(final String fullName) {
        if (Strings.isNullOrEmpty(fullName) || fullName.indexOf(':') == -1) {
            return null;
        }
        final String[] parts = NAMESPACE_PATTERN.split(fullName);
        return parts[0];
    }

    /**
     * @param fullName e.g. {@code myproject:foo}
     * @return prefix e.g. {@code foo}
     */
    public static String getNameAfterPrefix(final String fullName) {
        if (Strings.isNullOrEmpty(fullName) || fullName.indexOf(':') == -1) {
            return null;
        }
        final String[] parts = NAMESPACE_PATTERN.split(fullName);
        return parts[1];
    }

    @SuppressWarnings("HippoHstCallNodeRefreshInspection")
    public static void refreshSession(final Session session, final boolean keepChanges) {
        try {
            if (session != null) {
                session.refresh(keepChanges);
            }
        } catch (RepositoryException e) {
            log.error("Error refreshing session", e);
        }
    }

    public static Session createSession() {
        try {
            final HippoRepository repository = HippoRepositoryFactory.getHippoRepository("vm://");
            // TODO: use login name/password ??
            return repository.login("admin", "admin".toCharArray());
        } catch (RepositoryException e) {
            log.error("Error creating repository connection", e);
        }
        return null;
    }

    public static void cleanupSession(final Session session) {
        if (session != null) {
            session.logout();
        }

    }

    public static <T> T newInstance(final Class<T> clazz) {
        try {
            return clazz.newInstance();
        } catch (InstantiationException e) {
            log.error("Error instantiating", e);
        } catch (IllegalAccessException e) {
            log.error("Access exception", e);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static <T> T newInstance(final String className) {
        Class<?> aClass = loadCLass(className);
        if (aClass != null) {
            return (T) newInstance(aClass);
        }
        return null;
    }


    public static Class<?> loadCLass(final String clazz) {
        try {
            if (Strings.isNullOrEmpty(clazz)) {
                log.warn("Class name was null or empty");
                return null;
            }
            return Class.forName(clazz);
        } catch (ClassNotFoundException e) {
            log.error("Error loading class: [" + clazz + ']', e);
        }
        return null;
    }


    public static String decodeUrl(final String url) {
        try {
            return URLDecoder.decode(url, EssentialConst.UTF_8);
        } catch (UnsupportedEncodingException e) {
            log.error("Error using encoder", e);
        }
        return url;

    }

    public static String validFileName(final String input) {
        if (Strings.isNullOrEmpty(input)) {
            return null;
        }

        final String myInput = DOT_PATTERN.matcher(input).replaceAll("-");
        return CharMatcher.inRange('a', 'z')
                .or(CharMatcher.inRange('0', '9'))
                .or(CharMatcher.inRange('A', 'Z'))
                .or(CharMatcher.is('_'))
                .or(CharMatcher.is('-'))
                .retainFrom(myInput);
    }
}
