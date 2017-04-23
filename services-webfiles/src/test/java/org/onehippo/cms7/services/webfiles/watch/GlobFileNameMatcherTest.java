/*
 * Copyright 2014-2017 Hippo B.V. (http://www.onehippo.com)
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

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Arrays;

import org.apache.logging.log4j.Level;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onehippo.cms7.services.webfiles.LogRecorder;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GlobFileNameMatcherTest {

    private static LogRecorder logRecorder;

    private GlobFileNameMatcher matcher;

    @BeforeClass
    public static void beforeClass() throws Exception {
        logRecorder = new LogRecorder(GlobFileNameMatcher.log);
        GlobFileNameMatcher.log = logRecorder.getLogger();
    }

    @Before
    public void setUp() {
        matcher = new GlobFileNameMatcher();
    }

    @After
    public void tearDown() {
        logRecorder.clear();
    }

    @Test
    public void includeNothing_excludesAllFiles() {
        matcher.excludeDirectories("a");
        assertExcludedDir("a");
        assertIncludedDir("b");
        assertExcludedFile("a");
        assertExcludedFile("b");
        assertExcludedFile("file.txt");
    }

    @Test
    public void excludedAndIncludedPatterns_areIndependent() {
        matcher.includeFiles("a");
        matcher.excludeDirectories("a");
        assertIncludedFile("a");
        assertExcludedDir("a");
    }

    @Test
    public void excludeIncludedSubPattern() {
        matcher.includeFiles("[a-c]");
        matcher.excludeDirectories("b");
        assertIncludedFile("a");
        assertIncludedFile("b");
        assertExcludedDir("b");
        assertIncludedFile("c");
    }

    @Test
    public void includeFile() {
        matcher.includeFiles("file.txt");
        assertIncludedFile("file.txt");
        assertIncludedFile("subdir", "file.txt");
        assertIncludedFile("subdir", "subsubdir", "file.txt");
        assertExcludedFile("file1.txt");
        assertExcludedFile("file.txt.bak");
    }

    @Test
    public void nullDoesNotMatch() {
        matcher.includeFiles("*");
        assertFalse(matcher.matchesFile(null));
        assertFalse(matcher.matchesDirectory(null));
    }

    @Test
    public void excludedDirectory_isExcludedInSubDirectories() {
        matcher.includeFiles("*");
        matcher.excludeDirectories("dir");
        assertIncludedFile("file.txt");
        assertExcludedDir("dir");
        assertIncludedFile("dir", "file.txt");
        assertIncludedDir("dir", "subdir");
        assertExcludedDir("dir", "dir");
        assertIncludedDir("otherDir");
        assertIncludedFile("otherDir", "file.txt");
        assertExcludedDir("otherDir", "dir");
        assertExcludedDir("otherDir", "subdir", "dir");
        assertIncludedFile("file1.txt");
        assertIncludedFile("file.txt.bak");
    }

    @Test
    public void includeDSStore() {
        matcher.includeFiles(".DS_Store");
        assertIncludedFile(".DS_Store");
        assertIncludedFile("subdir", ".DS_Store");
        assertIncludedFile("subdir", "subsubdir", ".DS_Store");
    }

    @Test
    public void excludeGit() {
        matcher.excludeDirectories(".git");
        assertExcludedDir(".git");
        assertExcludedDir("subdir", ".git");
        assertExcludedDir("subdir", "subsubdir", ".git");
    }

    @Test
    public void includeFilesWithSingleCharMatch() {
        matcher.includeFiles("?a.txt");
        assertIncludedFile("aa.txt");
        assertIncludedFile("ba.txt");
        assertExcludedFile("a.txt");
        assertExcludedFile("aaa.txt");
    }

    @Test
    public void excludeDirectoriesWithSingleCharMatch() {
        matcher.excludeDirectories("?a");
        assertExcludedDir("aa");
        assertExcludedDir("ba");
        assertIncludedDir("a");
        assertIncludedDir("aaa");
    }

    @Test
    public void includeFilesWithRange() {
        matcher.includeFiles("[a-c].txt");
        assertIncludedFile("a.txt");
        assertIncludedFile("b.txt");
        assertIncludedFile("c.txt");
        assertExcludedFile("d.txt");
        assertExcludedFile("aa.txt");
    }

    @Test
    public void excludeDirectoriesWithRange() {
        matcher.includeFiles("*");
        matcher.excludeDirectories("[a-c].tmp");
        assertExcludedDir("a.tmp");
        assertExcludedDir("b.tmp");
        assertExcludedDir("c.tmp");
        assertIncludedDir("aa.tmp");
        assertIncludedDir("d.tmp");
        assertIncludedDir("aa.tmp");
    }

    @Test(expected = IllegalArgumentException.class)
    public void slashInIncludeFilesPattern_throwsException() {
        matcher.includeFiles("**/.somedir");
    }

    @Test(expected = IllegalArgumentException.class)
    public void slashInExcludeDirectoriesPattern_throwsException() {
        matcher.excludeDirectories("**/.somedir");
    }

    @Test
    public void includeFiles() {
        matcher.includeFiles(Arrays.asList("a.txt", "*.css"));
        assertIncludedFile("a.txt");
        assertIncludedFile("b.css");
        assertExcludedFile("c");
    }

    @Test
    public void excludeDirectories() {
        matcher.excludeDirectories(Arrays.asList("a", "*~"));
        assertExcludedDir("a");
        assertExcludedDir("b~");
        assertIncludedDir("b");
    }

    @Test
    public void includeFiles_ignoresIllegalPatterns() throws IOException, InterruptedException {
        matcher.includeFiles(Arrays.asList("a.txt", "**/*~", "b.txt"));
        assertIncludedFile("a.txt");
        assertIncludedFile("b.txt");
        assertExcludedFile("c");
        logRecorder.assertLogged(Level.WARN, "Ignoring file name glob pattern '**/*~': cannot contain '/'");
    }

    @Test
    public void excludeDirectories_ignoresIllegalPatterns() throws IOException, InterruptedException {
        matcher.excludeDirectories(Arrays.asList("a", "**/*~", "b"));
        assertExcludedDir("a");
        assertExcludedDir("b");
        assertIncludedDir("c.txt");
        logRecorder.assertLogged(Level.WARN, "Ignoring file name glob pattern '**/*~': cannot contain '/'");
    }

    private void assertIncludedFile(final String firstPath, final String... morePath) {
        final Path path = createPath(firstPath, morePath);
        assertTrue("Expected file to be included: " + path, matcher.matchesFile(path));
    }

    private void assertExcludedFile(final String firstPath, final String... morePath) {
        final Path path = createPath(firstPath, morePath);
        assertFalse("Expected file to be excluded: " + path, matcher.matchesFile(path));
    }

    private void assertIncludedDir(final String firstPath, final String... morePath) {
        final Path path = createPath(firstPath, morePath);
        assertTrue("Expected directory to be included: " + path, matcher.matchesDirectory(path));
    }

    private void assertExcludedDir(final String firstPath, final String... morePath) {
        final Path path = createPath(firstPath, morePath);
        assertFalse("Expected directory to be excluded: " + path, matcher.matchesDirectory(path));
    }

    private Path createPath(final String first, final String... more) {
        return FileSystems.getDefault().getPath(first, more);
    }

}