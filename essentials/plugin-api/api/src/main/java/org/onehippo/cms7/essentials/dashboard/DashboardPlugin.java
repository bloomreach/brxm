/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 */

package org.onehippo.cms7.essentials.dashboard;

import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;

/**
 * @version "$Id$"
 */
public abstract class DashboardPlugin extends Panel {

    private final PluginContext context;
    private final Plugin descriptor;

    public DashboardPlugin(final String id, final Plugin descriptor, final PluginContext context) {
        super(id);
        this.context = context;
        this.descriptor = descriptor;

    }


    public Plugin getDescriptor() {
        return descriptor;
    }

    public PluginContext getContext() {
        return context;
    }
}
