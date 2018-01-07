/*
 * Copyright 2014-2018 Hippo B.V. (http://www.onehippo.com)
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

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableSet;

import org.onehippo.cms7.essentials.dashboard.service.ProjectService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id$"
 */
public final class BeanWriterUtils {

    public static final ImmutableSet<String> BUILT_IN_DOCUMENT_TYPES = new ImmutableSet.Builder<String>()
            .add("autoexport").add("brokenlinks").add("editor")
            .add("frontend").add("hippo").add("hippobroadcast")
            .add("hippofacnav").add("hippogallery").add("hippogallerypicker")
            .add("hippohtmlcleaner").add("hippolog").add("hipporeport")
            .add("hipposched").add("hipposcxml").add("hippostd")
            .add("hippostdpubwf").add("hipposys").add("hipposysedit")
            .add("hippotranslation").add("hst").add("hstconfigedit").add("mix")
            .add("nt").add("properties").add("rep").add("reporting")
            .add("resourcebundle").add("selection")
            .build();


    private static Logger log = LoggerFactory.getLogger(BeanWriterUtils.class);

    private BeanWriterUtils() { }

    /**
     * Builds an in-memory graph by parsing XML namespaces. This graph can be used to write HST beans
     *
     * @param projectService      project service
     * @param sourceFileExtension extension used for source files e.g. {@code "java"}
     * @return a list of MemoryBeans or empty list if nothing is found
     */


    public static Map<String, Path> mapExitingBeanNames(final ProjectService projectService, final String sourceFileExtension) {
        final Map<String, Path> retVal = new HashMap<>();
        final List<Path> exitingBeans = findExitingBeans(projectService, sourceFileExtension);
        for (Path exitingBean : exitingBeans) {
            retVal.put(exitingBean.toFile().getName(), exitingBean);
        }
        // TODO improve
        return retVal;
    }

    /**
     * Find all existing HST beans (which annotated with {@code @Node})
     *
     * @param projectService      project service
     * @param sourceFileExtension file extension, e.g. {@code "java"}
     * @return a list of beans or an empty list if nothing was found
     */
    private static List<Path> findExitingBeans(final ProjectService projectService, final String sourceFileExtension) {
        final Path startDir = projectService.getBeansPackagePath();
        final List<Path> existingBeans = new ArrayList<>();
        final List<Path> directories = new ArrayList<>();
        GlobalUtils.populateDirectories(startDir, directories);
        final String pattern = "*." + sourceFileExtension;
        for (Path directory : directories) {
            try (final DirectoryStream<Path> stream = Files.newDirectoryStream(directory, pattern)) {
                for (Path path : stream) {
                    final String nodeJcrType = JavaSourceUtils.getNodeJcrType(path);
                    if (nodeJcrType != null) {
                        existingBeans.add(path);
                    }
                }
            } catch (IOException e) {
                log.error("Error reading java files", e);
            }
        }

        return existingBeans;
    }
}
