package org.onehippo.cms7.essentials.dashboard;

import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.utils.CndUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id: AbstractCndInstaller.java 176263 2013-09-06 09:31:16Z dvandiepen $"
 */
public abstract class AbstractCndInstaller extends AbstractDependencyInstaller {

    private static Logger log = LoggerFactory.getLogger(AbstractCndInstaller.class);
    private PluginContext context;
    private String uri;

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

    public PluginContext getContext() {
        return context;
    }

    public String getUri() {
        return uri;
    }
}
