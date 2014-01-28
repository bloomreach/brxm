package org.onehippo.cms7.essentials.dashboard.installer;

/**
 * @version "$Id$"
 */
public interface Installer {

    public void install();

    public InstallState getInstallState();

    public boolean isInstalled();

}
