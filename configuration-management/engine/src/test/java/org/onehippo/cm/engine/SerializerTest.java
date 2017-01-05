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
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.onehippo.cm.api.model.Configuration;
import org.onehippo.cm.impl.model.ConfigDefinitionImpl;
import org.onehippo.cm.impl.model.ConfigurationImpl;
import org.onehippo.cm.impl.model.DefinitionNodeImpl;
import org.onehippo.cm.impl.model.ModuleImpl;
import org.onehippo.cm.impl.model.ProjectImpl;
import org.onehippo.cm.impl.model.SourceImpl;
import org.onehippo.cm.impl.model.ValueImpl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class SerializerTest extends AbstractBaseTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void read_write_hierarchy_test() throws IOException {
        readAndWrite("/parser/hierarchy_test/repo-config.yaml");
    }

    @Test
    public void read_write_value_test() throws IOException {
        readAndWrite("/parser/value_test/repo-config.yaml");
    }

    @Test
    public void ad_hoc() throws IOException {
        // small test setup that can be used to quickly validate some serialization use case
        final ConfigurationImpl configuration = new ConfigurationImpl("configuration");
        final ProjectImpl project = configuration.addProject("project");
        final ModuleImpl module = project.addModule("module");
        final SourceImpl source = module.addSource("test.yaml");
        final ConfigDefinitionImpl config = source.addConfigDefinition();
        final DefinitionNodeImpl node = new DefinitionNodeImpl("/foo", "foo", config);
        config.setNode(node);

        final StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            if (i > 0) builder.append(' ');
            builder.append("0123456789");
        }
        node.addProperty("str", new ValueImpl(builder.toString()));

        final Map<String, Configuration> configurations = new LinkedHashMap<>();
        configurations.put(configuration.getName(), configuration);

        final ConfigurationSerializer serializer = new ConfigurationSerializer();
        serializer.serializeNode(folder.getRoot().toPath(), configurations);
        final Path path = folder.getRoot().toPath().resolve("repo-config").resolve("test.yaml");

        assertEquals(
                // the value for str should actually be broken over two lines, see
                // https://bitbucket.org/asomov/snakeyaml/issues/355/dumping-long-string-values-are-not-split
                "instructions:\n" +
                "  - config:\n" +
                "      - /foo:\n" +
                "          - str: 0123456789 0123456789 0123456789 0123456789 0123456789 0123456789 0123456789 0123456789 0123456789 0123456789\n",
                new String(Files.readAllBytes(path)));
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
