package org.onehippo.cms7.essentials;

import javax.jcr.Session;

import org.onehippo.cms7.essentials.dashboard.Plugin;
import org.onehippo.cms7.essentials.dashboard.ctx.DashboardPluginContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;

/**
 * @version "$Id: TestPluginContext.java 174579 2013-08-21 16:43:11Z mmilicevic $"
 */
public class TestPluginContext extends DashboardPluginContext {



    private static final long serialVersionUID = 1L;
    private static Logger log = LoggerFactory.getLogger(TestPluginContext.class);
    private Session session;

    public TestPluginContext(final Session session, final Plugin plugin, final EventBus eventBus) {
        super(session, plugin, eventBus);
    }


    @Override
    public Session getSession() {
        return session;
    }

    public void setSession(final Session session) {
        this.session = session;
    }
}
