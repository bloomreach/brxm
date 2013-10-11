package org.onehippo.cms7.essentials.installer.panels;

import java.io.Serializable;

import javax.jcr.Node;
import javax.jcr.Session;

import org.apache.wicket.ajax.AbstractAjaxTimerBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.time.Duration;
import org.onehippo.cms7.essentials.dashboard.PanelPlugin;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO: extract to NotificationPanel (needs to be created)
 *
 * @version "$Id$"
 */
public class GlobalToolbarPanel extends PanelPlugin {

    public static final String AUTOEXPORT_ENABLED = "autoexport:enabled";
    public static final String AUTO_EXPORT_PATH = "/hippo:configuration/hippo:modules/autoexport/hippo:moduleconfig";
    public static final String AUTOEXPORT_NOT_ENABLED = "Autoexport not enabled";
    public static final String AUTOEXPORT_IS_ENABLED = "Autoexport is enabled";
    private static final long serialVersionUID = 1L;
    private static Logger log = LoggerFactory.getLogger(GlobalToolbarPanel.class);
    private final WebMarkupContainer container;
    private final Label autoExportLabel;
    private final Model<String> autoExportModel;
    private final Model<String> totalNotificationsModel;
    private final Label totalNotificationsLabel;
    private NotificationsWrapper notifications;
    private final transient Session session;

    public GlobalToolbarPanel(final String id, final PluginContext ctx) {
        super(id, ctx);
        session = ctx.getSession();
        final RepeatingView view = new RepeatingView("repeatableList");

        notifications = new NotificationsWrapper(0);
        container = new WebMarkupContainer(view.newChildId());
        if (!autoExportEnabled()) {
            notifications.increment();
            autoExportModel = Model.of(AUTOEXPORT_NOT_ENABLED);
            autoExportLabel = new Label("label", autoExportModel);
            container.add(autoExportLabel);
        } else {
            autoExportModel = Model.of(AUTOEXPORT_IS_ENABLED);
            autoExportLabel = new Label("label", autoExportModel);
            container.add(autoExportLabel);
        }


        totalNotificationsModel = Model.of(String.valueOf(notifications.getTotalNotifications()));

        totalNotificationsLabel = new Label("totalNotifications", totalNotificationsModel);
        add(totalNotificationsLabel);
        add(view);
        view.add(container);
        add(new AbstractAjaxTimerBehavior(Duration.seconds(10)) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onTimer(AjaxRequestTarget target) {

                if (autoExportEnabled()) {
                    autoExportModel.setObject(AUTOEXPORT_IS_ENABLED);
                    notifications.decrement();
                } else {
                    autoExportModel.setObject(AUTOEXPORT_NOT_ENABLED);
                    notifications.setTotalNotifications(1);
                }

                totalNotificationsModel.setObject(String.valueOf(notifications.getTotalNotifications()));

                target.add(autoExportLabel);
                target.add(totalNotificationsLabel);

            }
        });
        container.setOutputMarkupId(true);
        autoExportLabel.setOutputMarkupId(true);
        totalNotificationsLabel.setOutputMarkupId(true);
    }

    private boolean autoExportEnabled() {
        try {

            if (session !=null && session.nodeExists(AUTO_EXPORT_PATH)) {
                final Node autoExportNode = session.getNode(AUTO_EXPORT_PATH);
                if (autoExportNode.hasProperty(AUTOEXPORT_ENABLED)) {
                    return autoExportNode.getProperty(AUTOEXPORT_ENABLED).getBoolean();
                }
            }

        } catch (Exception e) {
            log.error("Error checking panel", e);
        }
        return false;
    }

    private static class NotificationsWrapper implements Serializable {
        private static final long serialVersionUID = 1L;
        private int totalNotifications;

        private NotificationsWrapper(final int totalNotifications) {
            this.totalNotifications = totalNotifications;
        }

        public int getTotalNotifications() {
            return totalNotifications;
        }

        public void setTotalNotifications(final int totalNotifications) {
            this.totalNotifications = totalNotifications;
        }

        public void increment() {
            ++totalNotifications;
        }

        public void decrement() {
            --totalNotifications;
            if (totalNotifications < 0) {
                totalNotifications = 0;
            }
        }
    }


}
