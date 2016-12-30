/*
 *  Copyright 2016-2017 Hippo B.V. (http://www.onehippo.com)
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

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.onehippo.cm.api.model.Configuration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class SerializerTest extends AbstractBaseTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void read_write_hierarchy_test() throws IOException {
        readAndWrite("/parser/hierarchy_test/repo-config.yaml");
    }

    @Ignore
    @Test
    public void read_write_value_test() throws IOException {
        readAndWrite("/parser/value_test/repo-config.yaml");
    }

    private void readAndWrite(final String repoConfig) throws IOException {
        final TestFiles files = collectFilesFromResource(repoConfig);
        final ConfigurationParser parser = new ConfigurationParser();
        final Map<String, Configuration> configurations = parser.parse(files.repoConfig, files.sources);

        final ConfigurationSerializer serializer = new ConfigurationSerializer();
        serializer.serializeNode(folder.getRoot().toPath(), configurations);

        final TestFiles serializedFiles = collectFiles(folder.getRoot().toPath());

        assertURLContentIdentical(files.repoConfig, serializedFiles.repoConfig);
        assertEquals(files.sources.size(), serializedFiles.sources.size());
        for (URL url : files.sources) {
            final URL match = findMatch(url, serializedFiles.sources);
            assertURLContentIdentical(url, match);
        }
    }

    private void assertURLContentIdentical(final URL expected, final URL actual) throws IOException {
        assertEquals(new String(Files.readAllBytes(urlToPath(expected))), new String(Files.readAllBytes(urlToPath(actual))));
    }

    private URL findMatch(final URL url, final List<URL> sources) {
        final String suffix = getUrlSuffix(url);
        for (URL source : sources) {
            if (suffix.equals(getUrlSuffix(source))) {
                return source;
            }
        }
        fail("Cannot find matching file for " + url.toString());
        return null;
    }

    private String getUrlSuffix(final URL url) {
        final String str = url.toString();
        final int position = str.lastIndexOf("repo-config");
        if (position == -1) {
            fail("Cannot find string 'repo-config' in " + str);
        }
        return str.substring(position);
    }

}
