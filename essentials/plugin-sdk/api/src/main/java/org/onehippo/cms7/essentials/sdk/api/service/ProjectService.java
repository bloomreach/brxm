/*
 * Copyright 2017-2018 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.sdk.api.service;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.onehippo.cms7.essentials.sdk.api.model.Module;

/**
 * ProjectService provides access to the project, and in particular to the file resources of a project.
 *
 * It can be @Inject-ed into an Essentials plugin's REST resource or custom {@code Instruction}.
 */
public interface ProjectService {
    String GROUP_ID_COMMUNITY = "org.onehippo.cms7";
    String GROUP_ID_ENTERPRISE = "com.onehippo.cms7";

    /**
     * Get the Path to the base directory of the specified project module.
     *
     * @param module desired project module
     * @return Path to module base directory
     */
    Path getBasePathForModule(Module module);

    /**
     * Get the Path to the pom.xml file of the specified project module.
     *
     * @param module desired project module
     * @return Path to pom.xml file
     */
    Path getPomPathForModule(Module module);

    /**
     * Get the Path to the Java root directory (src/main/java) of the specified project module.
     *
     * @param module desired project module
     * @return Path to Java root directory
     */
    Path getJavaRootPathForModule(Module module);

    /**
     * Get the Path to the resources root directory (src/main/resources) of the specified project module.
     *
     * @param module desired project module
     * @return Path to resources root directory
     */
    Path getResourcesRootPathForModule(Module module);

    /**
     * Get the Path to the web application root directory (src/main/webapp) of the specified project module.
     *
     * @param module desired project module
     * @return Path to web application root directory
     */
    Path getWebApplicationRootPathForModule(Module module);

    /**
     * Get the Path to the WEB-INF directory of the specified project module.
     *
     * @param module desired project module
     * @return Path to WEB-INF directory
     */
    Path getWebInfPathForModule(Module module);

    /**
     * Get the Path to the Java root directory of the module containing the HST bean classes.
     *
     * By default, this returns the Java root directory of the project's 'site' module, but if the
     * {@code ProjectSettings} specify a beansFolder, the returned Path is adjusted to that directory instead.
     *
     * @return Path to the Java root directory of the module containing the HST bean classes
     */
    Path getBeansRootPath();

    /**
     * Get the Path to the directory containing the HST bean classes.
     *
     * Combined the Path returned by #getBeansRootPath with {@code ProjectSettings}'s 'selectedBeansPackage'.
     *
     * @return Path to the directory containing the HST bean classes
     */
    Path getBeansPackagePath();

    /**
     * Get the Path to the directory containing the (HST) REST resource classes.
     *
     * HST REST resource classes are expected to reside inside the 'site' module.
     *
     * @return Path to the directory containing the (HST) REST resource classes
     */
    Path getRestPackagePath();

    /**
     * Get the Path to the directory containing the HST component classes.
     *
     * HST component classes are expected to reside in the 'site' module.
     *
     * @return Path to the directory containing the HST component classes
     */
    Path getComponentsPackagePath();

    /**
     * Get the Path to the project's tomcat context.xml configuration file.
     *
     * The context.xml file is expected to be found at 'conf/context.xml'.
     *
     * @return Path to the project's tomcat context.xml configuration file
     */
    Path getContextXmlPath();

    /**
     * Get the Path to the project's directory where the configuration files for the Maven Assembly plugin are located.
     *
     * These files are expected to be found in 'src/main/assembly' of the project.
     *
     * @return Path to the project's assembly directory
     */
    Path getAssemblyFolderPath();

    /**
     * Retrieve a list of the log4j2 files of the project.
     */
    List<File> getLog4j2Files();

    /**
     * Copy a resource to a file inside the project.
     *
     * The targetLocation is interpolated with the context's placeholder data to produce the full filesystem path
     * for the destination file. It therefore typically starts with a placeholder such as {{siteResources}}.
     *
     * @param resourcePath    absolute classloader path to the resource to copy
     * @param targetLocation  of the copied file
     * @param placeholderData to interpolate placeholders in resource and targetLocation
     * @param canOverwrite    flag indicating that if the file already exists, it may or may not be overwritten
     * @param isBinary        flag indicating that the resource has binary content and is therefore not subject to interpolation
     * @return true if the file was copied successfully, false otherwise
     */
    boolean copyResource(String resourcePath, String targetLocation, Map<String, Object> placeholderData, boolean canOverwrite, boolean isBinary);

    /**
     * Delete a file from the project sources.
     *
     * The targetLocation is interpolated with the context's placeholder data to produce the full filesystem path
     * for the destination file. It therefore typically starts with a placeholder such as {{siteResources}}.
     *
     * @param targetLocation  of the to-be-deleted file
     * @param placeholderData to interpolate placeholders in targetLocation
     * @return true if the file doesn't exist after the invocation, false otherwise
     */
    boolean deleteFile(String targetLocation, Map<String, Object> placeholderData);
}
