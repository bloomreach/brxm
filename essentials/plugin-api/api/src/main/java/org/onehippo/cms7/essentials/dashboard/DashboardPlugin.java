/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 */

package org.onehippo.cms7.essentials.dashboard;

import javax.jcr.Session;

import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;

/**
 * @version "$Id: DashboardPlugin.java 167907 2013-06-17 08:34:55Z mmilicevic $"
 */
public abstract class DashboardPlugin extends Panel {


    private static final long serialVersionUID = 1L;
    private final PluginContext context;
    private final Plugin descriptor;
    private boolean outputMarkupId;
    private boolean outputMarkupPlaceholderTag;

    public DashboardPlugin(final String id, final Plugin descriptor, final PluginContext context) {
        super(id);
        this.context = context;
        this.descriptor = descriptor;
        setOutputMarkupId(true);
        setOutputMarkupPlaceholderTag(true);

    }

    /**
     * Logout  all JCR sessions
     * <p> <strong>NOTE:</strong> no save or session refresh is called, only {@code session.logout()} is callled</p>
     */

    protected void onRemove() {

        // cleanup connections:
        final Session session = context.getSession();
        if (session != null) {
            session.logout();
        }


    }

    public Plugin getDescriptor() {
        return descriptor;
    }

    public PluginContext getContext() {
        return context;
    }

    public void setOutputMarkupId(final boolean outputMarkupId) {
        this.outputMarkupId = outputMarkupId;
    }

    public void setOutputMarkupPlaceholderTag(final boolean outputMarkupPlaceholderTag) {
        this.outputMarkupPlaceholderTag = outputMarkupPlaceholderTag;
    }
}
