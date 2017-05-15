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
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.onehippo.cm.api.model.ConfigurationModel;
import org.onehippo.cm.api.model.Module;
import org.onehippo.cm.engine.parser.ParserException;
import org.onehippo.cm.impl.model.GroupImpl;
import org.onehippo.cm.impl.model.ModuleImpl;
import org.onehippo.cm.impl.model.builder.ConfigurationModelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.onehippo.cm.engine.Constants.BOOTSTRAP_MODULES_PROPERTY;
import static org.onehippo.cm.engine.Constants.HCM_MODULE_YAML;
import static org.onehippo.cm.engine.Constants.MAVEN_MODULE_DESCRIPTOR;
import static org.onehippo.cm.engine.Constants.PROJECT_BASEDIR_PROPERTY;

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
            throws IOException, ParserException, URISyntaxException {
        final StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        // load modules that are specified via repo.bootstrap.modules system property
        final List<Map<String, GroupImpl>> filesystemGroups = readModulesFromFilesystem(verifyOnly);

        // we expect each group above to hold exactly one module, and for modules to be uniquely named
        final Set<Module> filesystemModules = filesystemGroups.stream()
                .flatMap(gm -> gm.values().stream())
                .flatMap(g -> g.getProjects().stream())
                .flatMap(p -> p.getModules().stream())
                .collect(Collectors.toSet());

        // add all of the filesystem modules to the builder
        final ConfigurationModelBuilder builder = new ConfigurationModelBuilder();
        for (Map<String, GroupImpl> groups : filesystemGroups) {
            builder.push(groups);
        }

        // load modules that are packaged on the classpath
        final Pair<List<FileSystem>, List<Map<String, GroupImpl>>> classpathGroups =
                readModulesFromClasspath(classLoader, verifyOnly);

        // we need to filter out modules that were already loaded via the filesystem
        classpathGroups.getRight().stream()
                .flatMap(gm -> gm.values().stream())
                .flatMap(g -> g.getModifiableProjects().stream())
                .forEach(p -> {
            List<ModuleImpl> toRemove = new ArrayList<>();
            for (ModuleImpl m : p.getModifiableModules()) {
                if (filesystemModules.contains(m)) {
                    toRemove.add(m);
                }
            }
            p.getModifiableModules().removeAll(toRemove);
        });

        // add filesystems to the builder
        classpathGroups.getLeft().forEach(fileSystem -> {
            builder.addFileSystem(fileSystem);
        });

        // add filtered groups to the builder
        classpathGroups.getRight().forEach(groups -> {
            builder.push(groups);
        });

        // build the merged ConfigurationModel
        final ConfigurationModel model = builder.build();

        stopWatch.stop();
        log.info("ConfigurationModel loaded in {}", stopWatch.toString());

        return model;
    }

    /**
     * Read modules that were specified using the repo.bootstrap.modules system property.
     * @param verifyOnly TODO
     * @return a List of results from PathConfigurationReader
     */
    protected List<Map<String, GroupImpl>> readModulesFromFilesystem(final boolean verifyOnly) throws IOException, ParserException {
        // if repo.bootstrap.modules and project.basedir are defined, load modules from the filesystem before loading
        // additional modules from the classpath
        final String projectDir = System.getProperty(PROJECT_BASEDIR_PROPERTY);
        final String fileModules = System.getProperty(BOOTSTRAP_MODULES_PROPERTY);
        final List<Map<String, GroupImpl>> filesystemGroups = new ArrayList<>();
        if (StringUtils.isNotBlank(projectDir) && StringUtils.isNotBlank(fileModules)) {

            // convert the project basedir to a Path, so we can resolve modules against it
            final Path projectPath = Paths.get(projectDir);

            // for each module in repo.bootstrap.modules
            final String[] moduleNames = fileModules.split(";");
            for (String moduleName : moduleNames) {
                // use maven conventions to find a module descriptor, then parse it
                final Path moduleDescriptorPath = projectPath.resolve(moduleName).resolve(MAVEN_MODULE_DESCRIPTOR);

                if (!moduleDescriptorPath.toFile().exists()) {
                    throw new IllegalStateException("Cannot find module descriptor for module in "
                            + BOOTSTRAP_MODULES_PROPERTY + ", expected: " + moduleDescriptorPath);
                }

                log.debug("Loading module descriptor from filesystem here: {}", moduleDescriptorPath);

                final PathConfigurationReader.ReadResult result =
                        new PathConfigurationReader().read(moduleDescriptorPath, verifyOnly);
                filesystemGroups.add(result.getGroups());
            }

        }
        return filesystemGroups;
    }

    /**
     * Read modules that are packaged on the classpath for the given ClassLoader.
     * @param classLoader the classloader to search for packaged config modules
     * @param verifyOnly TODO
     * @return a Map of FileSystems that will need to be closed after processing the modules and the corresponding PathConfigurationReader result
     */
    protected Pair<List<FileSystem>, List<Map<String, GroupImpl>>> readModulesFromClasspath(final ClassLoader classLoader, final boolean verifyOnly)
            throws IOException, ParserException, URISyntaxException {
        final Pair<List<FileSystem>, List<Map<String, GroupImpl>>> groups = new MutablePair<>(new ArrayList<>(), new ArrayList<>());

        // find all the classpath resources with a filename that matches the expected module descriptor filename
        // and also located at the root of a classpath entry
        final Enumeration<URL> resources = classLoader.getResources(HCM_MODULE_YAML);
        while (resources.hasMoreElements()) {
            final URL resource = resources.nextElement();

            // look for the marker that indicates this is a path within a jar file
            // this is the normal case when we load modules that were packaged and deployed in a Tomcat container
            final int jarContentMarkerIdx = resource.getFile().lastIndexOf("!/");
            if (jarContentMarkerIdx > 0) {
                // note: the below mapping of resource url to path assumes the jar physically exists on the filesystem,
                // using a non-exploded war based classloader might fail here, but that is (currently) not supported anyway
                final Path jarPath = Paths.get(resource.getFile().substring("file:".length(), jarContentMarkerIdx));

                // Jar-based FileSystems must remain open for the life of a ConfigurationModel, and must be closed when
                //  processing is complete via ConfigurationModel.close()!
                final FileSystem fs = FileSystems.newFileSystem(jarPath, null);

                // since this FS represents a jar, we should look for the descriptor at the root of the FS
                final Path moduleDescriptorPath = fs.getPath(HCM_MODULE_YAML);
                final PathConfigurationReader.ReadResult result =
                        new PathConfigurationReader().read(moduleDescriptorPath, verifyOnly);

                // Hang onto a reference to this FS, so we can close it later with ConfigurationModel.close()
                groups.getLeft().add(fs);

                groups.getRight().add(result.getGroups());
            }
            else {
                // if part of the classpath is a raw dir on the native filesystem, just use the default FileSystem
                // this is useful for loading a module for testing purposes without packaging it into a jar
                // since this FS is a normal native FS, we need to use the full resource path to load the descriptor
                final Path moduleDescriptorPath = Paths.get(resource.toURI());
                final PathConfigurationReader.ReadResult result =
                        new PathConfigurationReader().read(moduleDescriptorPath, verifyOnly);

                groups.getRight().add(result.getGroups());
            }
        }
        return groups;
    }
}
