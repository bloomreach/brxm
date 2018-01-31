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

package org.onehippo.cms7.essentials.plugin;

import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import com.google.common.base.Strings;

import org.onehippo.cms7.essentials.plugin.sdk.packaging.DefaultInstructionPackage;
import org.onehippo.cms7.essentials.plugin.sdk.utils.EnterpriseUtils;
import org.onehippo.cms7.essentials.plugin.sdk.utils.GlobalUtils;
import org.onehippo.cms7.essentials.sdk.api.model.rest.MavenDependency;
import org.onehippo.cms7.essentials.sdk.api.model.rest.MavenRepository;
import org.onehippo.cms7.essentials.sdk.api.model.rest.PluginDescriptor;
import org.onehippo.cms7.essentials.sdk.api.service.MavenDependencyService;
import org.onehippo.cms7.essentials.sdk.api.service.MavenRepositoryService;
import org.onehippo.cms7.essentials.sdk.api.service.ProjectService;
import org.onehippo.cms7.essentials.sdk.api.model.Module;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.util.StringUtils;

public class Plugin {
    private final static Logger log = LoggerFactory.getLogger(Plugin.class);

    private final PluginDescriptor descriptor;
    private final ProjectService projectService;
    private final InstallStateMachine stateMachine;

    @Inject private AutowireCapableBeanFactory injector;
    @Inject private MavenDependencyService dependencyService;
    @Inject private MavenRepositoryService repositoryService;

    public Plugin(final PluginDescriptor descriptor, final ProjectService projectService) {
        this.descriptor = descriptor;
        this.projectService = projectService;
        this.stateMachine = new InstallStateMachine(this, projectService);
    }

    public PluginDescriptor getDescriptor() {
        return descriptor;
    }

    public String getId() {
        return descriptor.getId();
    }

    public InstallState getInstallState() {
        return stateMachine.getState();
    }

    public void install() throws PluginException {
        upgradeIfNecessary();
        installRepositories();
        installDependencies();

        stateMachine.install();
    }

    public void promote() {
        stateMachine.promote();
    }

    public void setup() throws PluginException {
        stateMachine.setup();
    }

    public boolean hasGeneralizedSetUp() {
        return StringUtils.hasText(getDescriptor().getPackageFile());
    }

    public DefaultInstructionPackage makeInstructionPackageInstance() {
        final String packageFile = descriptor.getPackageFile();
        if (!Strings.isNullOrEmpty(packageFile)) {
            final DefaultInstructionPackage instructionPackage = GlobalUtils.newInstance(DefaultInstructionPackage.class);
            if (instructionPackage != null) {
                instructionPackage.setInstructionPath(packageFile);
                injector.autowireBean(instructionPackage);
                return instructionPackage;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return descriptor != null ? descriptor.getName() : "unknown";
    }

    private void upgradeIfNecessary() {
        final Map<String, Set<String>> categories = descriptor.getCategories();
        if (categories != null) {
            final Set<String> licenses = categories.get("license");
            if (licenses != null && licenses.contains("enterprise")) {
                EnterpriseUtils.upgradeToEnterpriseProject(projectService, dependencyService, repositoryService);
            }
        }
    }

    private void installRepositories() throws PluginException {
        final StringBuilder builder = new StringBuilder();

        for (MavenRepository.WithModule repository : descriptor.getRepositories()) {
            if (!repositoryService.addRepository(Module.pomForName(repository.getTargetPom()), repository)) {
                if (builder.length() == 0) {
                    builder.append("Not all repositories were installed: ");
                } else {
                    builder.append(", ");
                }
                builder.append(repository.getUrl());
            }
        }

        if (builder.length() > 0) {
            throw new PluginException(builder.toString());
        }
    }

    private void installDependencies() throws PluginException {
        final StringBuilder builder = new StringBuilder();

        for (MavenDependency.WithModule dependency : descriptor.getDependencies()) {
            if (!dependencyService.addDependency(Module.pomForName(dependency.getTargetPom()), dependency)) {
                if (builder.length() == 0) {
                    builder.append("Not all dependencies were installed: ");
                } else {
                    builder.append(", ");
                }
                builder.append(dependency.getGroupId()).append(':').append(dependency.getArtifactId());
            }
        }

        if (builder.length() > 0) {
            throw new PluginException(builder.toString());
        }
    }
}
