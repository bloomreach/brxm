/*
 *  Copyright 2016 Hippo B.V. (http://www.onehippo.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.onehippo.cms.l10n;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.repository.RepositorySystem;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilder;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilderException;
import org.apache.maven.shared.dependency.graph.DependencyNode;

import static java.lang.String.format;

@SuppressWarnings("unused")
@Mojo(name = "include-locales", defaultPhase = LifecyclePhase.PREPARE_PACKAGE)
public class IncludeLocalesMojo extends AbstractMojo {

    @SuppressWarnings("unused")
    @Parameter( defaultValue = "${project}", readonly = true )
    private MavenProject project;

    /**
     * The dependency tree builder to use for verbose output.
     */
    @SuppressWarnings("unused")
    @Component(hint = "default")
    private DependencyGraphBuilder dependencyGraphBuilder;

    @SuppressWarnings("unused")
    @Parameter(defaultValue = "${session}", readonly = true, required = true)
    private MavenSession session;

    /**
     * The local repository taken from Maven's runtime. Typically $HOME/.m2/repository.
     */
    @SuppressWarnings("unused")
    @Parameter(defaultValue = "${localRepository}", readonly = true, required = true)
    private ArtifactRepository localRepository;

    /**
     * List of Remote Repositories used by the resolver.
     */
    @SuppressWarnings("unused")
    @Parameter(defaultValue = "${project.remoteArtifactRepositories}", readonly = true, required = true)
    private List<ArtifactRepository> remoteRepositories;

    @SuppressWarnings("unused")
    @Parameter
    private String locales;

    @SuppressWarnings("unused")
    @Parameter
    private List<String> artifactPrefixes;

    private Map<String, String> artifactPrefixToLocaleArtifact;

    @SuppressWarnings("unused")
    @Component
    RepositorySystem repositorySystem;

    public void execute() throws MojoExecutionException {
        if (locales == null || artifactPrefixes == null || artifactPrefixes.size() == 0) {
            return;
        }

        String[] localeArray = StringUtils.split(locales, " ,\t\f\r\n");

        List<String> invalidPrefixes = new ArrayList<>();

        artifactPrefixToLocaleArtifact = map(artifactPrefixes);

        try {
            ProjectBuildingRequest buildingRequest =
                    new DefaultProjectBuildingRequest(session.getProjectBuildingRequest());
            buildingRequest.setProject(project);
            final DependencyNode dependencyNode = dependencyGraphBuilder.buildDependencyGraph(buildingRequest, null);
            recurse(dependencyNode, localeArray);
        } catch (DependencyGraphBuilderException e) {
            getLog().error(e);
        }


    }

    private void recurse(final DependencyNode dependencyNode, final String[] localeArray) {
        final Artifact artifact = dependencyNode.getArtifact();
        final String artifactName = artifact.getGroupId() + ":" + artifact.getArtifactId();
        if (isIncludedScope(artifact)) {
            String resolvedArtifactPrefix = null;
            for (String artifactPrefix : artifactPrefixToLocaleArtifact.keySet()) {

                if (artifactName.equals(artifactPrefix) || artifactName.startsWith(artifactPrefix + "-")) {
                    resolvedArtifactPrefix = artifactPrefix;
                    for (String locale : localeArray) {
                        final Artifact localeArtifact = repositorySystem.createArtifactWithClassifier(artifact.getGroupId(), artifactPrefixToLocaleArtifact.get(artifactPrefix), artifact.getBaseVersion(), "jar", locale);
                        try {
                            final Artifact resolvableLocaleArtifact = resolvableLocaleArtifact(localeArtifact, repositorySystem, remoteRepositories, localRepository);
                            if (resolvableLocaleArtifact == null) {
                                getLog().warn("Could not resolve localization module '" + localeArtifact + "' and also no fallback to lower micro version");
                            } else {
                                project.getDependencyArtifacts().add(resolvableLocaleArtifact);
                                getLog().info("Include localization module '" + resolvableLocaleArtifact + "'.");
                                if (getLog().isDebugEnabled() && !localeArtifact.getVersion().equals(resolvableLocaleArtifact.getVersion())) {
                                    getLog().debug("Fallback localization module '" + resolvableLocaleArtifact + "' is found for " +
                                            "'" + localeArtifact + "'");
                                }
                            }
                        } catch (MojoExecutionException e) {
                            if (getLog().isDebugEnabled()) {
                                getLog().warn(e.toString(), e);
                            } else {
                                getLog().warn(e.toString());
                            }
                        }
                    }
                    break;
                }
            }
            if (resolvedArtifactPrefix != null) {
                // avoid multiple inclusion of say 'org.onehippo.cms7:hippo-cms-l10n:jar:nl:4.0.0-SNAPSHOT:runtime' jar
                // once a artifactPrefixe has been resolved, it can be removed since needs to added just once
                artifactPrefixToLocaleArtifact.remove(resolvedArtifactPrefix);
            }
        }

        if (artifactPrefixes.isEmpty()) {
            // all locales artifacts have been added
            return;
        }

        for (DependencyNode child : dependencyNode.getChildren()) {
            recurse(child, localeArray);
        }
    }

    public Artifact resolvableLocaleArtifact(Artifact localeArtifact, RepositorySystem repositorySystem, List remoteRepositories,
                                             ArtifactRepository localRepository) throws MojoExecutionException {
        ArtifactResolutionRequest request = new ArtifactResolutionRequest()
                .setArtifact(localeArtifact)
                .setRemoteRepositories(remoteRepositories)
                .setLocalRepository(localRepository);

        ArtifactResolutionResult resolutionResult = repositorySystem.resolve(request);

        if (resolutionResult.hasExceptions()) {
            throw new MojoExecutionException("Could not resolve artifact " + localeArtifact, resolutionResult.getExceptions().get(0));
        }

        final Artifact result;
        if (resolutionResult.getArtifacts().isEmpty() || !(result = resolutionResult.getArtifacts().iterator().next()).isResolved()) {
            final Artifact fallbackLocaleArtifact = findFallbackLocaleArtifact(localeArtifact);
            if (fallbackLocaleArtifact == null) {
                return null;
            }
            return resolvableLocaleArtifact(fallbackLocaleArtifact, repositorySystem, remoteRepositories, localRepository);
        }
        return result;
    }

    /**
     * Returns the Artifact with version x.y.(z - 1) for {@code artifact} with version x.y.z. If 'z' is not a number but
     * for example '4-SNAPSHOT' or '4-cmng-psp1-SNAPSHOT', everything after the number gets removed and then number that is
     * left is returned as version. Thus for example '4-SNAPSHOT' returns '4' as micro version.
     * If no more fallback artifacts should be tried, {@code null} is returned
     * @return fallback {@link Artifact} or {@code null} when no more fallback artifacts can be tried
     */
    Artifact findFallbackLocaleArtifact(final Artifact artifact) {
        final String version = artifact.getBaseVersion();
        if (StringUtils.isBlank(version)) {
            getLog().error(format("Unexpected empty baseversion for '%'", artifact.toString()));
            return null;
        }
        String[] majorMinorMicro = version.split("\\.");
        if (majorMinorMicro.length != 3) {
            getLog().info(format("Unexpected version formatting '%s' because expected MAJOR.MINOR.MICRO format where " +
                    "MAJOR, MINOR are expected to be an integer and MICRO to start with an integer. Cannot try fallback locale artifacts.", version));
            return null;
        }
        try {
            final String previousMicroVersion = findPreviousMicroVersion(majorMinorMicro[2]);
            if (previousMicroVersion == null) {
                getLog().debug(String.format("no more fallback to lower micro version for artifact '%s'", artifact));
                return null;
            } else {
                final String fallbackVersion = majorMinorMicro[0] + "." + majorMinorMicro[1] + "." + previousMicroVersion;
                final Artifact fallback = repositorySystem.createArtifactWithClassifier(artifact.getGroupId(), artifact.getArtifactId(), fallbackVersion, "jar", artifact.getClassifier());
                getLog().debug(String.format("Fallback to artifact '%s'", fallback));
                return fallback;
            }
        } catch (IllegalArgumentException e) {
            getLog().info(format("Unexpected version formatting '%s' because expected MAJOR.MINOR.MICRO format where " +
                    "MAJOR, MINOR are expected to be an integer and MICRO to start with an integer. Cannot try fallback locale artifacts.", version));
        }
        return null;
    }

    /**
     * @return the previous micro microVersion or {@link null} when micro microVersion is already 0 (or 0-x-y-x)
     */
    static String findPreviousMicroVersion(final String microVersion) {
        if (microVersion == null || microVersion.length() == 0) {
            throw new IllegalArgumentException("Unexpected micro microVersion since does not start with a number");
        }
        try {
            final int currentVersion = Integer.parseInt(microVersion);
            if (currentVersion == 0) {
                return null;
            }
            return String.valueOf(currentVersion - 1);
        } catch (NumberFormatException e) {
            boolean isSnapshot = microVersion.endsWith("-SNAPSHOT");
            if (isSnapshot) {
                return findPreviousMicroNumericVersion(microVersion.substring(0, microVersion.length() - "-SNAPSHOT".length()), isSnapshot, false);
            } else {
                return findPreviousMicroNumericVersion(microVersion, isSnapshot, false);
            }
        }
    }

    static String findPreviousMicroNumericVersion(final String microVersion,
                                                  final boolean isSnapshot, final boolean containsAlphaCharsApartFromSnapshot) {
        if (microVersion == null || microVersion.length() == 0) {
            throw new IllegalArgumentException("Unexpected micro microVersion since does not start with a number");
        }

        try {
            final int currentVersion = Integer.parseInt(microVersion);
            if (isSnapshot && containsAlphaCharsApartFromSnapshot) {
                // micro version was something 1-xyz-psp1-SNAPSHOT : return 1-SNAPSHOT
                return currentVersion + "-SNAPSHOT";
            }

            // micro version was something 1-SNAPSHOT : return 1
            return String.valueOf(currentVersion);

        } catch (NumberFormatException e) {
            // we are either of the form '4-rc-1' or of the form '4-xyx-psp1-SNAPSHOT'
            return findPreviousMicroNumericVersion(microVersion.substring(0, microVersion.length() - 1), isSnapshot, true);
        }
    }

    private boolean isIncludedScope(final Artifact artifact) {
        return artifact.getScope() == null || artifact.getScope().equals("runtime") || artifact.getScope().equals("compile") || artifact.getScope().equals("sytem");
    }

    Map<String, String> map(final List<String> artifactPrefixes) {
        Map<String, String> mapping = new HashMap<>();

        for (String artifactPrefix : artifactPrefixes) {
            try {
                validate(artifactPrefix);
                final int index = artifactPrefix.indexOf(",");
                mapping.put(artifactPrefix.substring(0, index), artifactPrefix.substring(index + 1));
            } catch (IllegalArgumentException e) {
                getLog().warn(format("Skipping invalid artifact prefix '%s' : '%s'", artifactPrefix, e.getMessage()));
            }
        }
        return mapping;
    }

    /**
     * artifactPrefix must contain a : before the comma and after the comma not a : is allowed. There must be a comma
     * present
     */
    void validate(final String artifactPrefix) {
        if (StringUtils.isBlank(artifactPrefix)) {
            throw new IllegalArgumentException("Skipping artifactPrefix because empty");
        }
        if (!artifactPrefix.contains(":")) {
            throw new IllegalArgumentException("Skipping artifactPrefix because not of pattern '<groupId>:<artifactId>,<localeArtifactId>'");
        }
        if (!artifactPrefix.contains(",")) {
            throw new IllegalArgumentException("Skipping artifactPrefix because not of pattern '<groupId>:<artifactId>,<localeArtifactId>'");
        }
        final String substringAfterComma = artifactPrefix.substring(artifactPrefix.indexOf(",") + 1);
        if (substringAfterComma.contains(":") || substringAfterComma.contains(",")) {
            throw new IllegalArgumentException("Skipping artifactPrefix because not of pattern '<groupId>:<artifactId>,<localeArtifactId>'");
        }
    }
}
