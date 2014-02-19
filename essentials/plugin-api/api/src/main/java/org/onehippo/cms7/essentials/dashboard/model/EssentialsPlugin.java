/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 */

package org.onehippo.cms7.essentials.dashboard.model;

import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.installer.InstallState;
import org.onehippo.cms7.essentials.dashboard.installer.Installer;

/**
 * @version "$Id$"
 */
public abstract class EssentialsPlugin implements Installer {

    private final PluginContext context;
    private final Plugin descriptor;

    public EssentialsPlugin(final Plugin descriptor, final PluginContext context) {
        this.context = context;
        this.descriptor = descriptor;
    }

    public Plugin getDescriptor() {
        return descriptor;
    }

    public PluginContext getContext() {
        return context;
    }

    @Override
    public void install() {
        // do nothing
    }

    @Override
    public InstallState getInstallState() {
        return InstallState.INSTALLED_AND_RESTARTED;
    }

    @Override
    public boolean isInstalled() {
        return true;
    }
}
