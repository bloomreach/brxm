package org.onehippo.cms7.essentials.plugin;

import com.google.common.base.Strings;
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

public class Plugin {
    private final static Logger log = LoggerFactory.getLogger(Plugin.class);

    private final PluginDescriptor descriptor;
    private final InstallStateMachine stateMachine;

    public Plugin(final PluginDescriptor descriptor) {
        this.descriptor = descriptor;
        this.stateMachine = new InstallStateMachine(this);
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
        final PluginDescriptor descriptor = getDescriptor();
        return descriptor != null ? descriptor.getName() : "unknown";
    }

    private void installRepositories() throws PluginException {
        final StringBuilder builder = new StringBuilder();

        for (Repository repository : descriptor.getRepositories()) {
            if (!DependencyUtils.addRepository(repository)) {
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
            if (!DependencyUtils.addDependency(dependency)) {
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
