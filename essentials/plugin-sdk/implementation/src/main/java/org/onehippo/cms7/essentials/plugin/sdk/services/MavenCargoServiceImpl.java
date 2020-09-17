/*
 * Copyright 2017-2020 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.plugin.sdk.services;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.inject.Inject;

import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.Profile;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.onehippo.cms7.essentials.sdk.api.model.rest.MavenDependency;
import org.onehippo.cms7.essentials.sdk.api.model.Module;
import org.onehippo.cms7.essentials.sdk.api.service.MavenCargoService;
import org.onehippo.cms7.essentials.sdk.api.service.ProjectService;
import org.onehippo.cms7.essentials.plugin.sdk.utils.MavenModelUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class MavenCargoServiceImpl implements MavenCargoService {
    private static final Logger LOG = LoggerFactory.getLogger(MavenCargoServiceImpl.class);

    private static final String CARGO_PROFILE_ID = "cargo.run";
    private static final String CARGO_MAVEN2_PLUGIN = "cargo-maven2-plugin";

    private final ProjectService projectService;

    @Inject
    public MavenCargoServiceImpl(final ProjectService projectService) {
        this.projectService = projectService;
    }

    @Override
    public boolean addDependencyToCargoSharedClasspath(final MavenDependency dependency) {
        return updatePluginConfig(cargoPlugin -> {
            Xpp3Dom configuration = (Xpp3Dom) cargoPlugin.getConfiguration();
            Xpp3Dom container = configuration.getChild("container");
            Xpp3Dom dependencies = ensureChildElement(container, "dependencies");

            // Avoid duplicate dependencies
            for (Xpp3Dom dep : dependencies.getChildren()) {
                if (dependency.getGroupId().equals(dep.getChild(MavenDependency.GROUP_ID).getValue())
                        && dependency.getArtifactId().equals(dep.getChild(MavenDependency.ARTIFACT_ID).getValue())) {
                    return;
                }
            }

            // Create new shared classpath dependency
            Xpp3Dom dependencyElement = new Xpp3Dom("dependency");
            addTextElement(dependencyElement, MavenDependency.GROUP_ID, dependency.getGroupId());
            addTextElement(dependencyElement, MavenDependency.ARTIFACT_ID, dependency.getArtifactId());
            addTextElement(dependencyElement, "classpath", "shared");
            dependencies.addChild(dependencyElement);
        });
    }

    @Override
    public boolean addDeployableToCargoRunner(final MavenDependency dependency, final String webappContext) {
        return updatePluginConfig(cargoPlugin -> {
            Xpp3Dom configuration = (Xpp3Dom) cargoPlugin.getConfiguration();
            Xpp3Dom deployables = configuration.getChild("deployables");
            Xpp3Dom deployable = getDeployableForContext(deployables, webappContext);
            if (deployable == null) {
                deployable = new Xpp3Dom("deployable");
                deployables.addChild(deployable);
                Xpp3Dom properties = new Xpp3Dom("properties");
                addTextElement(properties, "context", webappContext);
                deployable.addChild(properties);
            }

            // make sure the deployable coordinates are right
            addUniqueTextElement(deployable, MavenDependency.GROUP_ID, dependency.getGroupId());
            addUniqueTextElement(deployable, MavenDependency.ARTIFACT_ID, dependency.getArtifactId());
            addUniqueTextElement(deployable, MavenDependency.TYPE, dependency.getType());
        });
    }

    private boolean updatePluginConfig(final Consumer<Plugin> modifier) {
        return updateProjectPom(pomModel -> {
            final Plugin cargoPlugin = getCargoPlugin(pomModel);
            if (cargoPlugin == null) {
                LOG.error("Failed to locate profile '{}' in project root pom.xml.", CARGO_PROFILE_ID);
                return false;
            }

            modifier.accept(cargoPlugin);
            return true;
        });
    }

    /**
     * Merge plugins and properties from an external model's cargo.run profile.
     * DefaultPomManager.mergePoms could not be used as it fails to copy properties,
     * https://issues.apache.org/jira/browse/ARCHETYPE-535
     */
    @Override
    public boolean mergeCargoProfile(final URL incomingDefinitions) {
        return updateProjectPom(pomModel -> {
            final Profile modelProfile = getCargoProfile(pomModel);

            final Model incomingModel = MavenModelUtils.readPom(incomingDefinitions);
            final Profile incomingProfile = getCargoProfile(incomingModel);
            if (incomingProfile == null) {
                LOG.error("Failed to merge cargo profiles, incoming model contains no relevant data.");
                return false;
            }

            if (modelProfile != null) {
                // Copy all properties. Existing properties will be overwritten
                MavenModelUtils.mergeProperties(modelProfile, incomingProfile);
                if (incomingProfile.getBuild() != null) {
                    MavenModelUtils.mergeBuildPlugins(modelProfile.getBuild(), incomingProfile.getBuild());
                }
            } else {
                // Model doesn't have a cargo.run profile yet, copy it
                pomModel.addProfile(incomingProfile);
            }
            return true;
        });
    }

    public boolean addSystemProperty(final String propertyName, final String propertyValue) {
        return updatePluginConfig(cargoPlugin -> {
            Xpp3Dom configuration = (Xpp3Dom) cargoPlugin.getConfiguration();
            Xpp3Dom container = configuration.getChild("container");
            Xpp3Dom systemProperties = ensureChildElement(container, "systemProperties");

            // Add new property with specified value
            Xpp3Dom property = new Xpp3Dom(propertyName);
            property.setValue(propertyValue);
            systemProperties.addChild(property);
        });
    }

    private Xpp3Dom ensureChildElement(final Xpp3Dom parent, final String childName) {
        Xpp3Dom child = parent.getChild(childName);
        if (child == null) {
            child = new Xpp3Dom(childName);
            parent.addChild(child);
        }

        return child;
    }

    private boolean updateProjectPom(final Function<Model, Boolean> modifier) {
        final File pom = projectService.getPomPathForModule(Module.PROJECT).toFile();
        final Model pomModel = MavenModelUtils.readPom(pom);
        if (pomModel == null) {
            return false;
        }

        return modifier.apply(pomModel) && MavenModelUtils.writePom(pomModel, pom);
    }

    private Xpp3Dom getDeployableForContext(final Xpp3Dom deployables, final String webappContext) {
        for (Xpp3Dom deployable : deployables.getChildren()) {
            if (webappContext.equals(deployable.getChild("properties").getChild("context").getValue())) {
                return deployable;
            }
        }

        return null;
    }

    private Plugin getCargoPlugin(final Model pomModel) {
        final Profile cargoProfile = getCargoProfile(pomModel);
        if (cargoProfile != null && cargoProfile.getBuild() != null) {
            for (Plugin plugin : cargoProfile.getBuild().getPlugins()) {
                if (plugin.getArtifactId().equals(CARGO_MAVEN2_PLUGIN)) {
                    return plugin;
                }
            }
        }
        return null;
    }

    private Profile getCargoProfile(final Model pomModel) {
        return pomModel.getProfiles()
                .stream()
                .filter(p -> CARGO_PROFILE_ID.equals(p.getId()))
                .findFirst()
                .orElse(null);
    }

    private void addUniqueTextElement(final Xpp3Dom parent, final String key, final String value) {
        Xpp3Dom child = parent.getChild(key);
        if (child == null) {
            child = new Xpp3Dom(key);
            parent.addChild(child);
        }
        child.setValue(value);
    }

    private void addTextElement(final Xpp3Dom parent, final String key, final String value) {
        Xpp3Dom child = new Xpp3Dom(key);
        child.setValue(value);
        parent.addChild(child);
    }
}
