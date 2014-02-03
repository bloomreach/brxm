package org.onehippo.cms7.essentials.dashboard;

import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.installer.InstallState;
import org.onehippo.cms7.essentials.dashboard.utils.CndUtils;

/**
 * Extends the AbstractDependencyInstaller, which implements the dependency installer API.
 * The AbstractCndInstaller checks if the namespace of the CMS dependencies has been installed in the repository and will return the appropriate InstallState.
 * Which can be either UNINSTALLED, INSTALLED_AND_RESTARTED (plugin is installed but the application needs to be rebuild for changes to take effect)
 * and INSTALLED (application has been rebuild and all the changes have taken effect). The AbstractCndInstaller checks the INSTALLED_AND_RESTARTED and INSTALLED state compared to the AbstractDependencyInstaller
 *
 * @version "$Id: AbstractCndInstaller.java 176263 2013-09-06 09:31:16Z dvandiepen $"
 */
public abstract class AbstractCndInstaller extends AbstractDependencyInstaller {

    private PluginContext context;
    private String uri;

    /**
     * The constructor needs the namespace URI to check if the appropriate CND has been installed with de dependencies.
     *
     * @param context the provided {@link org.onehippo.cms7.essentials.dashboard.ctx.PluginContext}
     * @param uri the uri of the namespace
     */
    public AbstractCndInstaller(PluginContext context, String uri) {
        this.context = context;
        this.uri = uri;
    }

    @Override
    public InstallState getInstallState() {
        InstallState installed = super.getInstallState();
        switch (installed) {
            case INSTALLED:
                if (CndUtils.existsNamespaceUri(context, uri)) {
                    installed = InstallState.INSTALLED_AND_RESTARTED;
                }
                break;
            case UNINSTALLED:
                break;

        }
        return installed;
    }

    /**
     * Gets the provided PluginContext
     * @return the corresponding {@link org.onehippo.cms7.essentials.dashboard.ctx.PluginContext}
     */
    public PluginContext getContext() {
        return context;
    }

    /**
     * Get the URI of the to installed namespace
     * @return the uri
     */
    public String getUri() {
        return uri;
    }
}
