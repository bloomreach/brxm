package org.onehippo.cms7.essentials.dashboard.installer;

/**
 * @version "$Id: Installer.java 174159 2013-08-19 11:30:46Z jreijn $"
 */
public interface Installer {

    public void install();

    public InstallState getInstallState();

    public boolean isInstalled();

}
