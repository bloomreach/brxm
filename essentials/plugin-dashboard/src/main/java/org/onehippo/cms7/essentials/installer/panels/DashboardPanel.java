package org.onehippo.cms7.essentials.installer.panels;

import java.util.List;

import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.onehippo.cms7.essentials.dashboard.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;

/**
 * @version "$Id$"
 */
public class DashboardPanel extends Panel {

    private static final long serialVersionUID = 1L;
    private static Logger log = LoggerFactory.getLogger(DashboardPanel.class);

    public DashboardPanel(final String id, final List<Plugin> allPlugins, final EventBus eventBus) {
        super(id);

    }

    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);

        /* TODO mm enable once we are finished*/
        /*response.render(JavaScriptHeaderItem.forReference(new JavaScriptResourceReference(
                DashboardPanel.class, "carousel.js")));*/
    }
}
