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
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.Profile;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.model.EssentialsDependency;
import org.onehippo.cms7.essentials.dashboard.model.Repository;
import org.onehippo.cms7.essentials.dashboard.model.RepositoryPolicy;
import org.onehippo.cms7.essentials.dashboard.model.RepositoryPolicyRestful;
import org.onehippo.cms7.essentials.dashboard.model.RepositoryRestful;
import org.onehippo.cms7.essentials.dashboard.model.TargetPom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

import static org.onehippo.cms7.essentials.dashboard.utils.ProjectUtils.ADDON_EDITION_INDICATOR_ARTIFACT_ID;
import static org.onehippo.cms7.essentials.dashboard.utils.ProjectUtils.ENT_RELEASE_ID;
import static org.onehippo.cms7.essentials.dashboard.utils.ProjectUtils.ENT_GROUP_ID;
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
            final String pomPath = ProjectUtils.getPomPath(context, targetPom);
            return writePom(pomPath, model);
        }
        return true;
    }

    public static boolean writePom(final String path, final Model model) {
        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter(path);
            // fix profile names (intellij expects default profile id)
            // see: http://youtrack.jetbrains.com/issue/IDEA-126568
            final List<Profile> profiles = model.getProfiles();
            boolean needsRewrite = false;
            for (Profile profile : profiles) {
                if (Strings.isNullOrEmpty(profile.getId()) || profile.getId().equals("default")) {
                    profile.setId("{{ESSENTIALS_DEFAULT_PLACEHOLDER}}");
                    needsRewrite = true;
                }
            }
            final MavenXpp3Writer writer = new MavenXpp3Writer();
            writer.write(fileWriter, model);
            if (needsRewrite) {
                fileWriter.close();
                // replace default id:
                final String pomContent = GlobalUtils.readStreamAsText(new FileInputStream(path));
                final Map<String, String> data = new HashMap<>();
                data.put("ESSENTIALS_DEFAULT_PLACEHOLDER", DEFAULT_PROFILE_ID);
                final String newContent = TemplateUtils.replaceStringPlaceholders(pomContent, data);
                GlobalUtils.writeToFile(newContent, new File(path).toPath());
                log.debug("Fixed default profile id");
            }
            log.debug("Written pom to: {}", path);
            return true;
        } catch (IOException e) {
            log.error("Error adding maven dependency", e);
            return false;
        } finally {
            IOUtils.closeQuietly(fileWriter);
        }


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
        final String pomPath = ProjectUtils.getPomPath(context, type);
        return writePom(pomPath, model);


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
            final String pomPath = ProjectUtils.getPomPath(context, targetPom);
            return writePom(pomPath, model);
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

            // add indicator:
            final Model cmsModel = ProjectUtils.getPomModel(context, TargetPom.CMS);
            final Dependency indicator = new Dependency();
            indicator.setArtifactId(ADDON_EDITION_INDICATOR_ARTIFACT_ID);
            indicator.setGroupId(ENT_GROUP_ID);
            cmsModel.addDependency(indicator);
            writePom(ProjectUtils.getPomPath(context, TargetPom.CMS), cmsModel);
            return writePom(ProjectUtils.getPomPath(context, TargetPom.PROJECT), pomModel);
        }
        return false;
    }

    public static boolean isEnterpriseProject(final PluginContext context) {
        final Model pomModel = ProjectUtils.getPomModel(context, TargetPom.PROJECT);
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
