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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.onehippo.cm.api.ConfigurationModel;
import org.onehippo.cm.engine.parser.ParserException;
import org.onehippo.cm.impl.model.builder.ConfigurationModelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.onehippo.cm.engine.Constants.HCM_MODULE_YAML;
import static org.onehippo.cm.engine.Constants.MAVEN_MODULE_DESCRIPTOR;

public class ClasspathConfigurationModelReader {

    private static final Logger log = LoggerFactory.getLogger(ClasspathConfigurationModelReader.class);

    /**
     * Searches the classpath for module manifest files and uses these as entry points for loading HCM module
     * configuration and content into a ConfigurationModel.
     *
     * @param classLoader the ClassLoader which will be searched for HCM modules
     * @param verifyOnly TODO explain this
     * @return a ConfigurationModel of configuration and content definitions
     * @throws IOException
     * @throws ParserException
     */
    public ConfigurationModel read(final ClassLoader classLoader, final boolean verifyOnly)
            throws IOException, ParserException {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        final ConfigurationModelBuilder builder = new ConfigurationModelBuilder();

        // if repo.bootstrap.modules and project.basedir are defined, load modules from the filesystem before loading
        // additional modules from the classpath
        final String projectDir = System.getProperty("project.basedir");
        final String fileModules = System.getProperty("repo.bootstrap.modules");
        if (StringUtils.isNotBlank(projectDir) && StringUtils.isNotBlank(fileModules)) {

            // convert the project basedir to a Path, so we can resolve modules against it
            Path projectPath = Paths.get(projectDir);

            // for each module in repo.bootstrap.modules
            String[] moduleNames = fileModules.split(";");
            for (String moduleName : moduleNames) {
                // use maven conventions to find a module descriptor, then parse it
                final Path moduleDescriptorPath = projectPath.resolve(moduleName).resolve(MAVEN_MODULE_DESCRIPTOR);

                log.debug("Loading module descriptor from filesystem here: {}", moduleDescriptorPath.toString());

                final PathConfigurationReader.ReadResult result =
                        new PathConfigurationReader().read(moduleDescriptorPath, verifyOnly);
                builder.push(result.getGroups());
            }

        }

        final Enumeration<URL> resources = classLoader.getResources(HCM_MODULE_YAML);
        while (resources.hasMoreElements()) {
            final URL resource = resources.nextElement();
            // note: the below mapping of resource url to path assumes the jar physically exists on the filesystem,
            // using a non-exploded war based classloader might fail here, but that is (currently) not supported anyway
            Path jarPath = Paths.get(resource.getFile().substring("file:".length(), resource.getFile().lastIndexOf("!/")));

            // FileSystems must remain open for the life of a ConfigurationModel, and must be closed when processing is complete
            // via ConfigurationModel.close()!
            FileSystem fs = FileSystems.newFileSystem(jarPath, null);
            builder.addFileSystem(fs);

            final Path moduleDescriptorPath = fs.getPath(HCM_MODULE_YAML);
            final PathConfigurationReader.ReadResult result =
                    new PathConfigurationReader().read(moduleDescriptorPath, verifyOnly);

            builder.push(result.getGroups());
        }
        ConfigurationModel model = builder.build();

        stopWatch.stop();
        log.info("ConfigurationModel loaded in {}", stopWatch.toString());

        return model;
    }
}
