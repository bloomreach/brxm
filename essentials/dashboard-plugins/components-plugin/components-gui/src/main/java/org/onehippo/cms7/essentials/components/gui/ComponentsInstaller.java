/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 */

package org.onehippo.cms7.essentials.components.gui;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.model.Dependency;
import org.onehippo.cms7.essentials.dashboard.AbstractDependencyInstaller;
import org.onehippo.cms7.essentials.dashboard.DependencyType;
import org.onehippo.cms7.essentials.dashboard.InstallState;
import org.onehippo.cms7.essentials.dashboard.utils.ProjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Installs maven dependencies  for components
 *
 * @version "$Id$"
 */
public class ComponentsInstaller extends AbstractDependencyInstaller {

    private static Logger log = LoggerFactory.getLogger(ComponentsInstaller.class);
    private Dependency dependency;

    public ComponentsInstaller() {
        dependency = new Dependency();
        dependency.setGroupId("org.onehippo.cms7.essentials");
        dependency.setArtifactId("hippo-components-plugin-library");
        dependency.setVersion("1.01.00-SNAPSHOT");
    }

    @Override
    public List<Dependency> getCmsDependencies() {
        return null;
    }

    @Override
    public List<Dependency> getSiteDependencies() {
        final ArrayList<Dependency> dependencies = new ArrayList<>();
        dependencies.add(dependency);
        return dependencies;
    }


    @Override
    public InstallState getInstallState() {
        if (ProjectUtils.isInstalled(DependencyType.SITE, dependency)) {
            return InstallState.INSTALLED_AND_RESTARTED;
        }
        return InstallState.UNINSTALLED;
    }
}
