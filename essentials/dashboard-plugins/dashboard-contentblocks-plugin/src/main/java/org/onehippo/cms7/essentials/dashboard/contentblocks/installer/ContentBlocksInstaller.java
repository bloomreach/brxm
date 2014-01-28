package org.onehippo.cms7.essentials.dashboard.contentblocks.installer;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.model.Dependency;
import org.onehippo.cms7.essentials.dashboard.AbstractDependencyInstaller;
import org.onehippo.cms7.essentials.dashboard.installer.InstallState;

/**
 * @version "$Id$"
 */
public class ContentBlocksInstaller extends AbstractDependencyInstaller {


    @Override
    public List<Dependency> getCmsDependencies() {
        final List<Dependency> contentblocksCmsDependencies = new ArrayList<>();
        Dependency plugin = new Dependency();
        plugin.setGroupId("org.onehippo.forge");
        plugin.setArtifactId("content-blocks");

        contentblocksCmsDependencies.add(plugin);
        return contentblocksCmsDependencies;
    }

    @Override
    public List<Dependency> getSiteDependencies() {
        return null;
    }

    @Override
    public InstallState getInstallState() {
        final InstallState currentState = super.getInstallState();
        if (InstallState.INSTALLED.equals(currentState)) {
            return InstallState.INSTALLED_AND_RESTARTED;
        }
        return currentState;
    }
}
