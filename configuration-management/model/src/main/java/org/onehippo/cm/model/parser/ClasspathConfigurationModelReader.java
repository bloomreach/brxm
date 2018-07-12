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
package org.onehippo.cm.model.parser;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.onehippo.cm.model.Constants;
import org.onehippo.cm.model.impl.ConfigurationModelImpl;
import org.onehippo.cm.model.impl.GroupImpl;
import org.onehippo.cm.model.impl.ModuleImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.stream.Collectors.toList;

public class ClasspathConfigurationModelReader {

    private static final Logger log = LoggerFactory.getLogger(ClasspathConfigurationModelReader.class);


    /**
     * Searches the classpath for module manifest files and uses these as entry points for loading HCM module
     * configuration and content into a ConfigurationModel.
     *
     * @param classLoader the ClassLoader which will be searched for HCM modules
     * @return a ConfigurationModel of configuration and content definitions
     * @throws IOException
     * @throws ParserException
     */
    public ConfigurationModelImpl read(final ClassLoader classLoader)
            throws IOException, ParserException, URISyntaxException {
        return read(classLoader, false);
    }

    /**
     * Searches the classpath for module manifest files and uses these as entry points for loading HCM module
     * configuration and content into a ConfigurationModel. This method only loads "core" modules that are not part
     * of an extension.
     *
     * @param classLoader the ClassLoader which will be searched for HCM modules
     * @param verifyOnly when true use 'verify only' yaml parsing, allowing (but warning on) certain model errors
     * @return a ConfigurationModel of configuration and content definitions
     * @throws IOException
     * @throws ParserException
     */
    public ConfigurationModelImpl read(final ClassLoader classLoader, final boolean verifyOnly)
            throws IOException, ParserException, URISyntaxException {
        final StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        final ConfigurationModelImpl model = new ConfigurationModelImpl();

        // load modules that are packaged on the classpath
        final Pair<Set<FileSystem>, List<ModuleImpl>> coreModules =
                readModulesFromClasspath(classLoader, verifyOnly, false);

        // TODO: filter out isExtension() modules

        // add classpath modules to model
        coreModules.getRight().forEach(model::addModule);

        // add filesystems to the model
        model.setFileSystems(coreModules.getLeft());

        // build the merged model
        model.build();

        stopWatch.stop();
        log.info("ConfigurationModel core loaded in {}", stopWatch.toString());

        return model;
    }

    /**
     * Searches the classpath for module manifest files and uses these as entry points for loading HCM module
     * configuration and content into a ConfigurationModel. This method only loads modules that are part of an extension.
     *
     * @param classLoader the ClassLoader which will be searched for HCM modules
     * @return a ConfigurationModel of configuration and content definitions
     * @throws IOException
     * @throws ParserException
     */
    // TODO: Why do we need a separate code path for this? Why not just load everything available to the classloader?
    public ConfigurationModelImpl readExtension(final String extensionName, final String hstRoot, final ClassLoader classLoader, final ConfigurationModelImpl model)
            throws IOException, ParserException, URISyntaxException {
        final StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        final Pair<Set<FileSystem>, List<ModuleImpl>> extensionModules =
                readModulesFromClasspath(classLoader, false, true);

        // insert extension name and hstRoot into each module
        extensionModules.getRight().forEach(module -> {
            module.setExtensionName(extensionName);
            module.setHstRoot(hstRoot);
        });

        // TODO: why use the override addReplacementModule codepath here instead of the normal addModule?
        extensionModules.getRight().forEach(model::addReplacementModule);

        // add filesystems to the model
        model.setFileSystems(extensionModules.getLeft());

        final ConfigurationModelImpl build = model.build();
        stopWatch.stop();
        log.info("ConfigurationModel extension loaded in {}", stopWatch.toString());

        return build;
    }

    // TODO: this is scary -- almost guaranteed to be a bad resource leak of open FileSystems!
    public Collection<ModuleImpl> collectExtensionModules(final String extensionName, final String hstRoot, final ClassLoader classLoader)
            throws IOException, ParserException, URISyntaxException {
        final Pair<Set<FileSystem>, List<ModuleImpl>> extensionModules =
                readModulesFromClasspath(classLoader, false, true);

        // insert extension name into each module
        extensionModules.getRight().forEach(module -> {
            module.setExtensionName(extensionName);
            module.setHstRoot(hstRoot);
        });

        return extensionModules.getRight();
    }

    /**
     * Read modules that are packaged on the classpath for the given ClassLoader.
     * @param classLoader the classloader to search for packaged config modules
     * @param verifyOnly TODO describe
     * @param extensions if true, load extension modules; if false, load core modules
     * @return a Map of FileSystems that will need to be closed after processing the modules and the corresponding PathConfigurationReader result
     */
    protected Pair<Set<FileSystem>, List<ModuleImpl>> readModulesFromClasspath(final ClassLoader classLoader,
                                                                              final boolean verifyOnly,
                                                                              final boolean extensions)
            throws IOException, ParserException, URISyntaxException {
        final Pair<Set<FileSystem>, List<ModuleImpl>> modules = new MutablePair<>(new HashSet<>(), new ArrayList<>());

        // find all the classpath resources with a filename that matches the expected module descriptor filename
        // and also located at the root of a classpath entry
        final Enumeration<URL> resources = classLoader.getResources(Constants.HCM_MODULE_YAML);
        final Enumeration<URL> parentResourcesEn = classLoader.getParent() != null ?
                classLoader.getParent().getResources(Constants.HCM_MODULE_YAML) : Collections.emptyEnumeration();
        final HashSet<URL> parentResources = new HashSet<>(Collections.list(parentResourcesEn));
        while (resources.hasMoreElements()) {
            final URL resource = resources.nextElement();

            // Skip modules in the parent (shared in web container) classloader during extension loading.
            // These should be considered part of the core HCM model.
            if (extensions && parentResources.contains(resource)) {
                continue;
            }

            final String resourcePath = resource.getPath();

            // look for the marker that indicates this is a path within a jar file
            // this is the normal case when we load modules that were packaged and deployed in a Tomcat container
            final int jarContentMarkerIdx = resourcePath.lastIndexOf("!/");
            if (jarContentMarkerIdx > 0) {
                // note: the below mapping of resource url to path assumes the jar physically exists on the filesystem,
                // using a non-exploded war based classloader might fail here, but that is (currently) not supported anyway

                // First convert the resourcePath to a platform native one, e.g. on Windows this 'fixes' /C:/ to C:\
                // Furthermore, it also properly decodes encoded special characters like spaces (%20) as well as for example '+' characters.
                // without needing to use URLDecoder.decode() (as often times is suggested).
                // URLDecoder.decode() typically will incorrectly replace '+' with ' '!
                // (it also could throw UnsupportedException, but most/all implementations handle this example 'silently')
                final File archiveFile = new File(new URL(resourcePath.substring(0, jarContentMarkerIdx)).toURI());
                final String nativePath = archiveFile.getPath();
                final Path jarPath = Paths.get(nativePath);

                // Jar-based FileSystems must remain open for the life of a ConfigurationModel, and must be closed when
                //  processing is complete via ConfigurationModel.close()!
                final FileSystem fs = FileSystems.newFileSystem(jarPath, null);

                // since this FS represents a jar, we should look for the descriptor at the root of the FS
                final Path moduleDescriptorPath = fs.getPath(Constants.HCM_MODULE_YAML);
                final PathConfigurationReader.ReadResult result =
                        new PathConfigurationReader().read(moduleDescriptorPath, verifyOnly);
                final ModuleImpl moduleImpl = result.getModuleContext().getModule();
                moduleImpl.setArchiveFile(archiveFile);


                // Hang onto a reference to this FS, so we can close it later with ConfigurationModel.close()
                modules.getLeft().add(fs);

                modules.getRight().add(moduleImpl);
            }
            else {
                // if part of the classpath is a raw dir on the native filesystem, just use the default FileSystem
                // this is useful for loading a module for testing purposes without packaging it into a jar
                // since this FS is a normal native FS, we need to use the full resource path to load the descriptor
                final Path moduleDescriptorPath = Paths.get(resource.toURI());
                final PathConfigurationReader.ReadResult result =
                        new PathConfigurationReader().read(moduleDescriptorPath, verifyOnly);

                modules.getRight().add(result.getModuleContext().getModule());
            }
        }
        return modules;
    }
}
