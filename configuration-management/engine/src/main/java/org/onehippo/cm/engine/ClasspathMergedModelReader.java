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
package org.onehippo.cm.engine;

import java.io.IOException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;

import org.onehippo.cm.api.MergedModel;
import org.onehippo.cm.impl.model.builder.MergedModelBuilder;

import static org.onehippo.cm.engine.Constants.REPO_CONFIG_YAML;

public class ClasspathMergedModelReader {

    public MergedModel read(final ClassLoader classLoader, final boolean verifyOnly)
            throws IOException, ParserException {
        final Enumeration<URL> resources = classLoader.getResources(REPO_CONFIG_YAML);
        final MergedModelBuilder builder = new MergedModelBuilder();
        while (resources.hasMoreElements()) {
            final URL resource = resources.nextElement();
            // note: the below mapping of resource url to path assumes the jar physically exists on the filesystem,
            // using a non-exploded war based classloader might fail here, but that is (currently) not supported anyway
            Path jarPath = Paths.get(resource.getFile().substring("file:".length(), resource.getFile().lastIndexOf("!/")));
            try (FileSystem fs = FileSystems.newFileSystem(jarPath, null)) {
                final Path repoConfig = fs.getPath(REPO_CONFIG_YAML);
                final PathConfigurationReader.ReadResult result =
                        new PathConfigurationReader().read(repoConfig, verifyOnly);
                builder.push(result.getConfigurations(), result.getResourceInputProviders());
            }
        }
        return builder.build();
    }
}
