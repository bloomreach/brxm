/*
 * Copyright 2014-2017 Hippo B.V. (http://www.onehippo.com)
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

import java.io.File;
import java.util.Iterator;
import java.util.List;

import com.google.common.base.Strings;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.model.EssentialsDependency;
import org.onehippo.cms7.essentials.dashboard.model.Repository;
import org.onehippo.cms7.essentials.dashboard.model.RepositoryPolicy;
import org.onehippo.cms7.essentials.dashboard.model.RepositoryPolicyRestful;
import org.onehippo.cms7.essentials.dashboard.model.RepositoryRestful;
import org.onehippo.cms7.essentials.dashboard.model.TargetPom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.onehippo.cms7.essentials.dashboard.utils.ProjectUtils.ENT_GROUP_ID;
import static org.onehippo.cms7.essentials.dashboard.utils.ProjectUtils.ENT_RELEASE_ID;
import static org.onehippo.cms7.essentials.dashboard.utils.ProjectUtils.ENT_REPO_ID;
import static org.onehippo.cms7.essentials.dashboard.utils.ProjectUtils.ENT_REPO_NAME;
import static org.onehippo.cms7.essentials.dashboard.utils.ProjectUtils.ENT_REPO_URL;

/**
 * @version "$Id$"
 */
public final class DependencyUtils {

    private static final String DEFAULT_PROFILE_ID = "default";
    private static Logger log = LoggerFactory.getLogger(DependencyUtils.class);


    private DependencyUtils() {
    }

    /**
     * Add maven repository node to pom model
     *
     * @param repository Repository instance
     * @return true if tag is added or already exists
     */
    public static boolean addRepository(final PluginContext context, final Repository repository) {
        final TargetPom targetPom = repository.getDependencyTargetPom();
        if (targetPom == TargetPom.INVALID) {
            return false;
        }
        final Model model = ProjectUtils.getPomModel(context, targetPom);
        if (model == null) {
            log.warn("Pom model was null for type: {}", targetPom);
            return false;
        }
        if (!hasRepository(context, repository)) {
            final org.apache.maven.model.Repository mavenRepository = repository.createMavenRepository();
            model.addRepository(mavenRepository);
            log.debug("Added new maven repository {}", repository);
            final File pomFile = ProjectUtils.getPomFile(context, targetPom);
            return MavenModelUtils.writePom(model, pomFile);
        }
        return true;
    }

    /**
     * @deprecated Use {@link MavenModelUtils#writePom(Model, File)}
     */
    @Deprecated
    public static boolean writePom(final String path, final Model model) {
        return MavenModelUtils.writePom(model, new File(path));
    }

    /**
     * Remove dependency from pom (if exists)
     *
     * @param dependency instance of EssentialsDependency dependency
     * @return true if removed or did not exist, false if dependency was invalid or on IO error
     */
    public static boolean removeDependency(final PluginContext context, final EssentialsDependency dependency) {
        final TargetPom type = dependency.getDependencyTargetPom();
        if (type == TargetPom.INVALID) {
            return false;
        }
        final Model model = ProjectUtils.getPomModel(context, type);
        if (model == null) {
            log.warn("Pom model was null for type: {}", type);
            return false;
        }
        if (!hasDependency(context, dependency)) {
            return true;
        }
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
        final File pomFile = ProjectUtils.getPomFile(context, type);
        return MavenModelUtils.writePom(model, pomFile);
    }

    /**
     * Adds dependency if one does not exists. If dependency exists, only version will be updated
     * (if version can be resolved and if provided dependency  is higher than existing one)
     *
     * @param dependency instance of EssentialsDependency dependency
     * @return true if dependency is added or already exists
     */

    public static boolean addDependency(final PluginContext context, final EssentialsDependency dependency) {
        final TargetPom targetPom = dependency.getDependencyTargetPom();
        if (targetPom == TargetPom.INVALID) {
            return false;
        }
        final Model model = ProjectUtils.getPomModel(context, targetPom);
        if (model == null) {
            log.warn("Pom model was null for targetPom: {}", targetPom);
            return false;
        }
        if (!hasDependency(context, dependency)) {
            final Dependency newDependency = dependency.createMavenDependency();
            model.addDependency(newDependency);
            final File pomFile = ProjectUtils.getPomFile(context, targetPom);
            return MavenModelUtils.writePom(model, pomFile);
        }
        return true;

    }

    /**
     * Checks if repository entry already exists within pom model
     *
     * @param repository repository instance
     * @return true if provided repository is invalid or when it exists within pom model with same url
     */
    public static boolean hasRepository(final PluginContext context, final Repository repository) {
        final TargetPom type = repository.getDependencyTargetPom();
        if (type == TargetPom.INVALID) {
            return false;
        }
        final Model model = ProjectUtils.getPomModel(context, type);
        if (model == null) {
            return false;
        }
        final List<org.apache.maven.model.Repository> repositories = model.getRepositories();
        for (org.apache.maven.model.Repository rep : repositories) {
            final String url = repository.getUrl();
            if (Strings.isNullOrEmpty(url)) {
                log.warn("Invalid Repository defined {}", rep);
                return false;
            }
            final String existingUrl = rep.getUrl();
            final boolean hasRepo = url.equals(existingUrl);
            if (hasRepo) {
                log.debug("Found repository for url: {}", url);
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if project has dependency
     *
     * @param dependency instance of EssentialsDependency dependency
     * @return true when dependency already exists in the target pom
     */
    public static boolean hasDependency(final PluginContext context, final EssentialsDependency dependency) {
        final TargetPom targetPom = dependency.getDependencyTargetPom();
        if (targetPom == TargetPom.INVALID) {
            return false;
        }
        final Model model = ProjectUtils.getPomModel(context, targetPom);
        if (model == null) {
            return false;
        }
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
                    if (!ourVersion.equals(currentVersion)) {
                        log.warn("Current version couldn't be resolved {}", currentVersion);
                    }
                    return true;
                }
                return VersionUtils.compareVersionNumbers(currentVersion, ourVersion) >= 0;
            }
        }
        return false;
    }

    public static boolean upgradeToEnterpriseProject(final PluginContext context) {
        if (isEnterpriseProject(context)) {
            return true;
        }

        final Repository repository = new RepositoryRestful();
        repository.setId(ENT_REPO_ID);
        repository.setName(ENT_REPO_NAME);
        repository.setUrl(ENT_REPO_URL);
        final RepositoryPolicy releases = new RepositoryPolicyRestful();
        releases.setUpdatePolicy("never");
        releases.setChecksumPolicy("fail");
        repository.setReleases(releases);
        repository.setTargetPom(TargetPom.PROJECT.getName());

        addRepository(context, repository);

        final Model pomModel = ProjectUtils.getPomModel(context, TargetPom.PROJECT);
        if (pomModel != null) {
            final Parent parent = new Parent();
            parent.setArtifactId(ENT_RELEASE_ID);
            parent.setGroupId(ENT_GROUP_ID);
            parent.setVersion(pomModel.getParent().getVersion());
            pomModel.setParent(parent);

            // add edition indicator:
            final Model cmsModel = ProjectUtils.getPomModel(context, TargetPom.CMS);
            if (cmsModel != null) {
                final Dependency indicator = new Dependency();
                indicator.setArtifactId("hippo-addon-edition-indicator");
                indicator.setGroupId(ENT_GROUP_ID);
                cmsModel.addDependency(indicator);

                // add enterprise package of app dependencies
                final Dependency enterpriseApp = new Dependency();
                enterpriseApp.setArtifactId("hippo-enterprise-package-app-dependencies");
                enterpriseApp.setGroupId(ENT_GROUP_ID);
                enterpriseApp.setType("pom");
                cmsModel.addDependency(enterpriseApp);

                MavenModelUtils.writePom(cmsModel, ProjectUtils.getPomFile(context, TargetPom.CMS));
            }
            return MavenModelUtils.writePom(pomModel, ProjectUtils.getPomFile(context, TargetPom.PROJECT));
        }
        return false;
    }

    public static boolean isEnterpriseProject(final PluginContext context) {
        final Model pomModel = ProjectUtils.getPomModel(context, TargetPom.PROJECT);
        if (pomModel == null) {
            return false;
        }
        final Parent parent = pomModel.getParent();
        if (parent == null) {
            return false;
        }
        final String groupId = parent.getGroupId();
        final String artifactId = parent.getArtifactId();
        return groupId.equals(ENT_GROUP_ID) && artifactId.equals(ENT_RELEASE_ID);

    }

    private static boolean isSameDependency(final EssentialsDependency dependency, final Dependency projectDependency) {
        return projectDependency.getArtifactId().equals(dependency.getArtifactId())
                && projectDependency.getGroupId().equals(dependency.getGroupId());
    }
}
