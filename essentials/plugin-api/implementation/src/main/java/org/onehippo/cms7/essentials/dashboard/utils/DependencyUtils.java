/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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

import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.onehippo.cms7.essentials.dashboard.model.DependencyType;
import org.onehippo.cms7.essentials.dashboard.model.EssentialsDependency;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

/**
 * @version "$Id$"
 */
public final class DependencyUtils {

    private static Logger log = LoggerFactory.getLogger(DependencyUtils.class);

    private DependencyUtils() {
    }

    /**
     * Remove dependency from pom (if exists)
     *
     * @param dependency instance of EssentialsDependency dependency
     * @return true if removed or did not exist, false if dependency was invalid or on IO error
     */
    public static boolean removeDependency(final EssentialsDependency dependency) {
        final DependencyType type = dependency.getDependencyType();
        if (type == DependencyType.INVALID) {
            return false;
        }
        final Model model = ProjectUtils.getPomModel(type);
        if (model == null) {
            log.warn("Pom model was null for type: {}", type);
            return false;
        }
        if (!hasDependency(dependency)) {
            return true;
        }
        FileWriter fileWriter = null;
        try {
            final List<Dependency> dependencies = model.getDependencies();
            final Iterator<Dependency> iterator = dependencies.iterator();
            while (iterator.hasNext()) {
                final Dependency next = iterator.next();
                if (isSameDependency(dependency, next)) {
                    iterator.remove();
                    log.info("Removed dependency {}", dependency);
                    break;
                }
            }
            fileWriter = new FileWriter(ProjectUtils.getPomPath(type));
            final MavenXpp3Writer writer = new MavenXpp3Writer();
            writer.write(fileWriter, model);
        } catch (IOException e) {
            log.error("Error adding maven dependency", e);
            return false;
        } finally {
            IOUtils.closeQuietly(fileWriter);
        }

        return true;

    }


    /**
     * Adds dependency if one does not exists. If dependency exists, only version will be updated
     * (if version can be resolved and if provided dependency  is higher than existing one)
     *
     * @param dependency instance of EssentialsDependency dependency
     * @return true if dependency is added or already exists
     */

    public static boolean addDependency(final EssentialsDependency dependency) {
        final DependencyType type = dependency.getDependencyType();
        if (type == DependencyType.INVALID) {
            return false;
        }
        final Model model = ProjectUtils.getPomModel(type);
        if (model == null) {
            log.warn("Pom model was null for type: {}", type);
            return false;
        }
        if (!hasDependency(dependency)) {
            FileWriter fileWriter = null;
            try {
                final Dependency newDependency = dependency.createMavenDependency();
                model.addDependency(newDependency);
                fileWriter = new FileWriter(ProjectUtils.getPomPath(type));
                final MavenXpp3Writer writer = new MavenXpp3Writer();
                writer.write(fileWriter, model);
            } catch (IOException e) {
                log.error("Error adding maven dependency", e);
                return false;
            } finally {
                IOUtils.closeQuietly(fileWriter);
            }
        }
        return true;

    }

    /**
     * Checks if project has dependency
     *
     * @param dependency instance of EssentialsDependency dependency
     * @return
     */
    public static boolean hasDependency(final EssentialsDependency dependency) {
        final DependencyType type = dependency.getDependencyType();
        if (type == DependencyType.INVALID) {
            return false;
        }
        if (type == DependencyType.CMS) {
            final Model model = ProjectUtils.getPomModel(type);
            final List<Dependency> dependencies = model.getDependencies();
            for (Dependency projectDependency : dependencies) {
                final boolean isSameDependency = isSameDependency(dependency, projectDependency);
                if (isSameDependency) {
                    final String ourVersion = dependency.getVersion();
                    // we don't   care about the version:
                    if (Strings.isNullOrEmpty(ourVersion)) {
                        return true;
                    }
                    //check if versions match:    (TODO fix placeholder versions)
                    final String currentVersion = projectDependency.getVersion();
                    if (Strings.isNullOrEmpty(currentVersion) || currentVersion.indexOf('$') != -1) {
                        log.warn("Current version couldn't be resolved {}", currentVersion);
                        return true;
                    }
                    return VersionUtils.compareVersionNumbers(currentVersion, ourVersion) >= 0;
                }
            }
            return false;
        }
        return false;
    }


    private static boolean isSameDependency(final EssentialsDependency dependency, final Dependency projectDependency) {
        return projectDependency.getArtifactId().equals(dependency.getArtifactId())
                && projectDependency.getGroupId().equals(dependency.getGroupId());
    }


}
