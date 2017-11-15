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

package org.onehippo.cms7.essentials.dashboard.utils;

import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.Profile;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.model.EssentialsDependency;
import org.onehippo.cms7.essentials.dashboard.model.TargetPom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MavenCargoUtils {
    private static Logger log = LoggerFactory.getLogger(MavenCargoUtils.class);

    private static final String CARGO_PROFILE_ID = "cargo.run";
    private static final String CARGO_MAVEN2_PLUGIN = "cargo-maven2-plugin";
    private static final String MAVEN_GROUPID = "groupId";
    private static final String MAVEN_ARTIFACTID = "artifactId";

    private MavenCargoUtils() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Checks if the cargo plugin in the cargo.run profile already has a deployable defined with specific context path
     *
     * @param webContext name of the context to look for. For example "/cms"
     * @return true if a deployable is defined in the cargo.run profile with the webContext context path
     */
    public static boolean hasCargoRunnerWebappContext(final PluginContext context, String webContext) {
        final Model pomModel = ProjectUtils.getPomModel(context, TargetPom.PROJECT);

        Plugin cargoPlugin = getCargoPlugin(pomModel);
        if (cargoPlugin != null) {
            Xpp3Dom configuration = (Xpp3Dom) cargoPlugin.getConfiguration();
            Xpp3Dom deployables = configuration.getChild("deployables");
            for (Xpp3Dom deployable : deployables.getChildren()) {
                if (webContext.equals(deployable.getChild("properties").getChild("context").getValue())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Adds a deployable section to the cargo plugin that defines a webapp
     **
     * @param dependency The dependency to add. Dependency groupId, artifactId and type are used
     * @param webContext Name of the context to look for. For example "/cms"
     * @return true if webapp deployable is added
     */
    public static boolean addDeployableToCargoRunner(final PluginContext context, final EssentialsDependency dependency, String webContext) {
        final Model pomModel = ProjectUtils.getPomModel(context, TargetPom.PROJECT);

        Plugin cargoPlugin = getCargoPlugin(pomModel);
        if (cargoPlugin == null) {
            return false;
        }

        if (hasCargoRunnerWebappContext(context, webContext)) {
            log.info("Cargo deployable with web context {} already exists", webContext);
            return false;
        }
        // Create new deployable
        Xpp3Dom deployable = new Xpp3Dom("deployable");
        addTextElement(deployable, MAVEN_GROUPID, dependency.getGroupId());
        addTextElement(deployable, MAVEN_ARTIFACTID, dependency.getArtifactId());
        addTextElement(deployable, "type", dependency.getType());
        Xpp3Dom properties = new Xpp3Dom("properties");
        addTextElement(properties, "context", webContext);
        deployable.addChild(properties);
        // Now add the deployable to the cargo deployables
        Xpp3Dom configuration = (Xpp3Dom) cargoPlugin.getConfiguration();
        Xpp3Dom deployables = configuration.getChild("deployables");
        deployables.addChild(deployable);
        return DependencyUtils.writePom(ProjectUtils.getPomPath(context, TargetPom.PROJECT),pomModel);
    }

    public static boolean removeDeployableFromCargoRunner(final PluginContext context, String webContext) {
        final Model pomModel = ProjectUtils.getPomModel(context, TargetPom.PROJECT);

        Plugin cargoPlugin = getCargoPlugin(pomModel);

        if (cargoPlugin != null) {
            Xpp3Dom configuration = (Xpp3Dom) cargoPlugin.getConfiguration();
            Xpp3Dom deployables = configuration.getChild("deployables");
            int childCount = deployables.getChildCount();
            for (int i = 0; i < childCount; i++) {
                Xpp3Dom deployable = deployables.getChild(i);
                if (webContext.equals(deployable.getChild("properties").getChild("context").getValue())) {
                    deployables.removeChild(i);
                    return DependencyUtils.writePom(ProjectUtils.getPomPath(context, TargetPom.PROJECT), pomModel);
                }
            }
        }
        return false;
    }

    /**
     * Adds a deployable section to the cargo plugin that defines a webapp
     **
     * @param groupId The maven groupId to add to the classpath
     * @param artifactId The maven artifactId to add to the classpath
     * @return true if the dependency is added to the shared classpath
     */
    public static boolean addDependencyToCargoSharedClasspath(final PluginContext context, final String groupId, final String artifactId) {
        final Model pomModel = ProjectUtils.getPomModel(context, TargetPom.PROJECT);

        Plugin cargoPlugin = getCargoPlugin(pomModel);
        if (cargoPlugin == null) {
            return false;
        }

        Xpp3Dom configuration = (Xpp3Dom) cargoPlugin.getConfiguration();
        Xpp3Dom container = configuration.getChild("container");
        // Add dependencies element to the container if it doesn't already exist
        Xpp3Dom dependencies = container.getChild("dependencies");
        if (dependencies == null) {
            dependencies = new Xpp3Dom("dependencies");
            container.addChild(dependencies);
        }
        // First check that item with the same groupId/artifactId doesn't already exist
        for (Xpp3Dom dep : dependencies.getChildren()) {
            if (groupId.equals(dep.getChild(MAVEN_GROUPID).getValue()) &&
                    artifactId.equals(dep.getChild(MAVEN_ARTIFACTID).getValue())) {
                log.info("Dependency {}:{} already on the shared classpath", groupId, artifactId);
                return false;
            }
        }

        // Create new shared classpath dependency
        Xpp3Dom dependency = new Xpp3Dom("dependency");
        addTextElement(dependency, MAVEN_GROUPID, groupId);
        addTextElement(dependency, MAVEN_ARTIFACTID, artifactId);
        addTextElement(dependency, "classpath", "shared");

        dependencies.addChild(dependency);
        return DependencyUtils.writePom(ProjectUtils.getPomPath(context, TargetPom.PROJECT),pomModel);
    }

    /**
     * Remove a deployable section to the cargo plugin that defines a webapp
     **
     * @param groupId The maven groupId to add to the classpath
     * @param artifactId The maven artifactId to add to the classpath
     * @return true if the dependency is added to the shared classpath
     */
    public static boolean removeDependencyFromCargoSharedClasspath(final PluginContext context, final String groupId, final String artifactId) {
        final Model pomModel = ProjectUtils.getPomModel(context, TargetPom.PROJECT);

        Plugin cargoPlugin = getCargoPlugin(pomModel);
        if (cargoPlugin == null) {
            return false;
        }

        Xpp3Dom configuration = (Xpp3Dom) cargoPlugin.getConfiguration();
        Xpp3Dom container = configuration.getChild("container");
        // Add dependencies element to the container if it doesn't already exist
        Xpp3Dom dependencies = container.getChild("dependencies");
        if (dependencies == null) {
            return false;
        }
        int childCount = dependencies.getChildCount();
        for (int i = 0; i < childCount; i++) {
            Xpp3Dom dependency = dependencies.getChild(i);
            if (groupId.equals(dependency.getChild(MAVEN_GROUPID).getValue())
                    && artifactId.equals(dependency.getChild(MAVEN_ARTIFACTID).getValue())) {
                dependencies.removeChild(i);
                return DependencyUtils.writePom(ProjectUtils.getPomPath(context, TargetPom.PROJECT),pomModel);
            }
        }
        return false;
    }

    /**
     * Return the cargo plugin inside the cargo.run profile as Maven model object. Return null if the profile or plugin is not found
     */
    private static Plugin getCargoPlugin(final Model pomModel) {
        // First find the cargo.run profile
        Profile cargoProfile = null;

        for (Profile profile : pomModel.getProfiles()) {
            if (CARGO_PROFILE_ID.equals((profile.getId()))) {
                cargoProfile = profile;
                break;
            }
        }
        if (cargoProfile == null) {
            log.info("{} profile not found", CARGO_PROFILE_ID);
            return null;
        }
        // Now the cargo.run profile is found, look for the plugin
        for (Plugin plugin : cargoProfile.getBuild().getPlugins()) {
            if (plugin.getArtifactId().equals(CARGO_MAVEN2_PLUGIN)) {
                return plugin;
            }
        }
        return null;
    }

    /* Add element to the dom with text. For example <artifactId>my-module</artifactId>
     */
    private static void addTextElement(Xpp3Dom dom, String key, String value) {
        Xpp3Dom element = new Xpp3Dom(key);
        element.setValue(value);
        dom.addChild(element);
    }
}
