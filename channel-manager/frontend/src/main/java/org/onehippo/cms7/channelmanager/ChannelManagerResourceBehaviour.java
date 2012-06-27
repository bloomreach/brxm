package org.onehippo.cms7.channelmanager;


import org.apache.wicket.Application;
import org.apache.wicket.Component;
import org.apache.wicket.behavior.AbstractBehavior;
import org.apache.wicket.markup.html.JavascriptPackageResource;
import org.onehippo.cms7.channelmanager.channels.ChannelGridPanel;
import org.onehippo.cms7.channelmanager.channels.ChannelIconPanel;
import org.onehippo.cms7.channelmanager.channels.ChannelOverview;
import org.onehippo.cms7.channelmanager.channels.ChannelPropertiesWindow;

public class ChannelManagerResourceBehaviour extends AbstractBehavior {

    private static final long serialVersionUID = 1L;

    public static final String ROOT_PANEL = "RootPanel.js";

    public static final String BLUEPRINT_LIST_PANEL = "BlueprintListPanel.js";

    public static final String CHANNEL_FORM_PANEL = "ChannelFormPanel.js";

    public static final String BREADCRUMB_TOOLBAR = "BreadcrumbToolbar.js";

    public static final String ALL = "channel-manager-all.js";

    public void bind(Component component) {
        if (Application.get().getDebugSettings().isAjaxDebugModeEnabled()) {
            component.add(JavascriptPackageResource.getHeaderContribution(ExtStoreFuture.class, ExtStoreFuture.EXT_STORE_FUTURE));
            component.add(JavascriptPackageResource.getHeaderContribution(ChannelManagerResourceBehaviour.class, BREADCRUMB_TOOLBAR));
            component.add(JavascriptPackageResource.getHeaderContribution(ChannelManagerResourceBehaviour.class, ROOT_PANEL));
            component.add(JavascriptPackageResource.getHeaderContribution(ChannelManagerResourceBehaviour.class, BLUEPRINT_LIST_PANEL));
            component.add(JavascriptPackageResource.getHeaderContribution(ChannelManagerResourceBehaviour.class, CHANNEL_FORM_PANEL));
            component.add(JavascriptPackageResource.getHeaderContribution(ChannelOverview.class, ChannelOverview.CHANNEL_OVERVIEW_PANEL_JS));
            component.add(JavascriptPackageResource.getHeaderContribution(ChannelPropertiesWindow.class, ChannelPropertiesWindow.CHANNEL_PROPERTIES_WINDOW_JS));
            component.add(JavascriptPackageResource.getHeaderContribution(ChannelGridPanel.class, ChannelGridPanel.CHANNEL_GRID_PANEL_JS));
            component.add(JavascriptPackageResource.getHeaderContribution(ChannelIconPanel.class, ChannelIconPanel.CHANNEL_ICON_PANEL_JS));
        } else {
            component.add(JavascriptPackageResource.getHeaderContribution(ChannelManagerResourceBehaviour.class, ALL));
        }
    }

}
