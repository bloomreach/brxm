/*
 *  Copyright 2016-2016 Hippo B.V. (http://www.onehippo.com)
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
import java.util.List;

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

@SuppressWarnings("unused")
@Mojo(name = "includeLocales", defaultPhase = LifecyclePhase.PREPARE_PACKAGE, threadSafe = false)
public class IncludeLocalesMojo extends AbstractMojo {

    @SuppressWarnings("unused")
    @Component
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

    @SuppressWarnings("unused")
    @Component
    RepositorySystem repositorySystem;

    public void execute() throws MojoExecutionException {
        if (locales == null || artifactPrefixes == null || artifactPrefixes.size() == 0) {
            return;
        }

        String[] localeArray = StringUtils.split(locales, " ,\t\f\r\n");

        List<String> invalidPrefixes = new ArrayList<>();
        for (String artifactPrefix : artifactPrefixes) {
            try {
                validate(artifactPrefix);
            } catch (IllegalArgumentException e) {
                invalidPrefixes.add(artifactPrefix);
                getLog().warn(String.format("Skipping invalid artifact prefix '%s' : '%s'", artifactPrefix, e.getMessage()));
            }
        }
        artifactPrefixes.removeAll(invalidPrefixes);

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
            for (String artifactPrefix : artifactPrefixes) {
                if (artifactName.equals(artifactPrefix) || artifactName.startsWith(artifactPrefix+"-")) {
                    resolvedArtifactPrefix = artifactPrefix;
                    for (String locale : localeArray) {
                        final String localeArtifactId = artifactPrefix.substring(artifactPrefix.indexOf(":") + 1) + "-l10n";
                        final Artifact localeArtifact = repositorySystem.createArtifactWithClassifier(artifact.getGroupId(), localeArtifactId, artifact.getBaseVersion(), "jar", locale);
                        try {
                            if (canResolveArtifact(localeArtifact, repositorySystem, remoteRepositories, localRepository)) {
                                project.getDependencyArtifacts().add(localeArtifact);
                                getLog().info("Include localization module  " + localeArtifact);
                            } else {
                                getLog().warn("Could not resolve localization module " + localeArtifact);
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
                artifactPrefixes.remove(resolvedArtifactPrefix);
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

    public static boolean canResolveArtifact(Artifact artifact, RepositorySystem repositorySystem, List remoteRepositories,
                                       ArtifactRepository localRepository) throws MojoExecutionException {
        ArtifactResolutionRequest request = new ArtifactResolutionRequest()
                .setArtifact(artifact)
                .setRemoteRepositories(remoteRepositories)
                .setLocalRepository(localRepository);

        ArtifactResolutionResult resolutionResult = repositorySystem.resolve(request);

        if (resolutionResult.hasExceptions()) {
            throw new MojoExecutionException("Could not resolve artifact " + artifact, resolutionResult.getExceptions().get(0));
        }

        if (resolutionResult.getArtifacts().size() != 1 || !resolutionResult.getArtifacts().iterator().next().isResolved()) {
            return false;
        }
        return true;
    }

    private boolean isIncludedScope(final Artifact artifact) {
        return artifact.getScope() == null || artifact.getScope().equals("runtime") || artifact.getScope().equals("compile") || artifact.getScope().equals("sytem");
    }

    private void validate(final String artifactPrefix) {
        if (StringUtils.isBlank(artifactPrefix)) {
            throw new IllegalArgumentException("Skipping artifactPrefix because empty");
        }
        if (!artifactPrefix.contains(":")) {
            throw new IllegalArgumentException("Skipping artifactPrefix because not of pattern '<groupId>:<artifactId>'");
        }
    }

}
