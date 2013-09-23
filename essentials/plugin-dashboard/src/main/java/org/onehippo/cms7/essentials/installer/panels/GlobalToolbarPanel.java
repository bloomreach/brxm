package org.onehippo.cms7.essentials.installer.panels;

import javax.jcr.Node;
import javax.jcr.Session;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.onehippo.cms7.essentials.dashboard.PanelPlugin;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO: extract to NotificationPanel (needs to be created)
 * @version "$Id$"
 */
public class GlobalToolbarPanel extends PanelPlugin {

    public static final String AUTOEXPORT_ENABLED = "autoexport:enabled";
    public static final String AUTO_EXPORT_PATH = "/hippo:configuration/hippo:modules/autoexport/hippo:moduleconfig";
    private static final long serialVersionUID = 1L;
    private static Logger log = LoggerFactory.getLogger(GlobalToolbarPanel.class);
    private int totalNotifications;


    public GlobalToolbarPanel(final String id, final PluginContext ctx) {
        super(id, ctx);

        final RepeatingView view = new RepeatingView("repeatableList");
        final WebMarkupContainer list = new WebMarkupContainer(view.newChildId());
        try {
            final Session session = getContext().getSession();
            if (session.nodeExists(AUTO_EXPORT_PATH)) {
                final Node autoExportNode = session.getNode(AUTO_EXPORT_PATH);
                if (autoExportNode.hasProperty(AUTOEXPORT_ENABLED)){
                    final boolean enabled = autoExportNode.getProperty(AUTOEXPORT_ENABLED).getBoolean();
                    if(!enabled){
                       ++totalNotifications;
                        list.add(new Label("label", "Autoexport not enabled"));
                    }
                }
            }

        } catch (Exception e) {
            log.error("Error checking panel", e);
        }
        if(totalNotifications==0){
            list.add(new Label("label", "No notifications"));
        }


        add(new Label("totalNotifications", String.valueOf(totalNotifications)));
        add(view);
        view.add(list);
    }




    public int getTotalNotifications() {
        return totalNotifications;
    }

    public void setTotalNotifications(final int totalNotifications) {
        this.totalNotifications = totalNotifications;
    }
}
