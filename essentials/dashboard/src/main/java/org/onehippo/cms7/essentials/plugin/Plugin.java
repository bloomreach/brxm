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

package org.onehippo.cms7.essentials.plugin;

import java.util.Map;
import java.util.Set;

import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.model.EssentialsDependency;
import org.onehippo.cms7.essentials.dashboard.model.PluginDescriptor;
import org.onehippo.cms7.essentials.dashboard.model.Repository;
import org.onehippo.cms7.essentials.dashboard.packaging.InstructionPackage;
import org.onehippo.cms7.essentials.dashboard.packaging.TemplateSupportInstructionPackage;
import org.onehippo.cms7.essentials.dashboard.utils.DependencyUtils;
import org.onehippo.cms7.essentials.dashboard.utils.GlobalUtils;
import org.onehippo.cms7.essentials.dashboard.utils.inject.ApplicationModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import com.google.common.base.Strings;

public class Plugin {
    private final static Logger log = LoggerFactory.getLogger(Plugin.class);

    private final PluginDescriptor descriptor;
    private final InstallStateMachine stateMachine;
    private final PluginContext context;

    public Plugin(final PluginContext context, final PluginDescriptor descriptor) {
        this.context = context;
        this.descriptor = descriptor;
        this.stateMachine = new InstallStateMachine(this, context);
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
        return StringUtils.hasText(getDescriptor().getPackageFile())
            || StringUtils.hasText(getDescriptor().getPackageClass());
    }

    public InstructionPackage makeInstructionPackageInstance() {
        InstructionPackage instructionPackage = null;

        // Prefers packageClass over packageFile.
        final String packageClass = descriptor.getPackageClass();
        if (!Strings.isNullOrEmpty(packageClass)) {
            instructionPackage = GlobalUtils.newInstance(packageClass);
            if (instructionPackage == null) {
                log.warn("Can't create instance for instruction package class {}", packageClass);
            }
        }

        if (instructionPackage == null) {
            final String packageFile = descriptor.getPackageFile();
            if (!Strings.isNullOrEmpty(packageFile)) {
                instructionPackage = GlobalUtils.newInstance(TemplateSupportInstructionPackage.class);
                instructionPackage.setInstructionPath(packageFile);
            }
        }

        if (instructionPackage != null) {
            ApplicationModule.getInjector().autowireBean(instructionPackage);
        }
        return instructionPackage;
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
                DependencyUtils.upgradeToEnterpriseProject(context);
            }
        }
    }

    private void installRepositories() throws PluginException {
        final StringBuilder builder = new StringBuilder();

        for (Repository repository : descriptor.getRepositories()) {
            if (!DependencyUtils.addRepository(context, repository)) {
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

        for (EssentialsDependency dependency : descriptor.getDependencies()) {
            if (!DependencyUtils.addDependency(context, dependency)) {
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
