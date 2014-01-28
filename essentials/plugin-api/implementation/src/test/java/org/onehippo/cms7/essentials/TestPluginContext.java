package org.onehippo.cms7.essentials;

import javax.jcr.Session;

import org.onehippo.cms7.essentials.dashboard.Plugin;
import org.onehippo.cms7.essentials.dashboard.ctx.DashboardPluginContext;

/**
 * @version "$Id: TestPluginContext.java 174579 2013-08-21 16:43:11Z mmilicevic $"
 */
public class TestPluginContext extends DashboardPluginContext {



    private static final long serialVersionUID = 1L;

    private Session session;

    public TestPluginContext(final Session session, final Plugin plugin) {
        super(session, plugin);
    }





    @Override
    public Session getSession() {
        return session;
    }

    public void setSession(final Session session) {
        this.session = session;
    }
}
