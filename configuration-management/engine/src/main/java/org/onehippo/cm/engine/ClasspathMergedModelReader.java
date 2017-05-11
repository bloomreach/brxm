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

import org.apache.commons.lang3.time.StopWatch;
import org.onehippo.cm.api.MergedModel;
import org.onehippo.cm.api.ResourceInputProvider;
import org.onehippo.cm.api.model.Module;
import org.onehippo.cm.engine.parser.ParserException;
import org.onehippo.cm.impl.model.builder.MergedModelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.Map;

import static java.util.stream.Collectors.toMap;
import static org.onehippo.cm.engine.Constants.REPO_CONFIG_YAML;

public class ClasspathMergedModelReader {

    private static final Logger log = LoggerFactory.getLogger(ClasspathMergedModelReader.class);

    /**
     * Searches the classpath for module manifest files and uses these as entry points for loading HCM module
     * configuration and content into a MergedModel.
     *
     * @param classLoader the ClassLoader which will be searched for HCM modules
     * @param verifyOnly TODO explain this
     * @return a MergedModel of configuration and content definitions
     * @throws IOException
     * @throws ParserException
     */
    public MergedModel read(final ClassLoader classLoader, final boolean verifyOnly)
            throws IOException, ParserException {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        final Enumeration<URL> resources = classLoader.getResources(REPO_CONFIG_YAML);
        final MergedModelBuilder builder = new MergedModelBuilder();
        while (resources.hasMoreElements()) {
            final URL resource = resources.nextElement();
            // note: the below mapping of resource url to path assumes the jar physically exists on the filesystem,
            // using a non-exploded war based classloader might fail here, but that is (currently) not supported anyway
            Path jarPath = Paths.get(resource.getFile().substring("file:".length(), resource.getFile().lastIndexOf("!/")));

            // FileSystems must remain open for the life of a MergedModel, and must be closed when processing is complete
            // via MergedModel.close()!
            FileSystem fs = FileSystems.newFileSystem(jarPath, null);
            builder.addFileSystem(fs);

            final Path repoConfig = fs.getPath(REPO_CONFIG_YAML);
            final PathConfigurationReader.ReadResult result =
                    new PathConfigurationReader().read(repoConfig, verifyOnly);
            Map<Module, ModuleContext> moduleContexts = result.getModuleContexts();
            Map<Module, ResourceInputProvider> configInputProviders = moduleContexts.entrySet().stream() //TODO SS: review this transformation
                    .collect(toMap(Map.Entry::getKey, v -> v.getValue().getConfigInputProvider()));

            //TODO SS: decide whether it is worth of passing module contexts to merged model
            Map<Module, ResourceInputProvider> contentInputProviders = moduleContexts.entrySet().stream()
                    .collect(toMap(Map.Entry::getKey, v -> v.getValue().getContentInputProvider()));

            builder.push(result.getConfigurations(), configInputProviders);
        }
        MergedModel model = builder.build();

        stopWatch.stop();
        log.info("MergedModel loaded in {}", stopWatch.toString());

        return model;
    }
}
