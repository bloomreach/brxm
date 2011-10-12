package org.onehippo.cms7.channelmanager;


import org.apache.wicket.Application;
import org.apache.wicket.Component;
import org.apache.wicket.behavior.AbstractBehavior;
import org.apache.wicket.markup.html.JavascriptPackageResource;
import org.onehippo.cms7.channelmanager.channels.ChannelGridPanel;
import org.onehippo.cms7.channelmanager.channels.ChannelPropertiesPanel;

public class ChannelManagerResourceBehaviour extends AbstractBehavior {

    public static final String ROOT_PANEL = "RootPanel.js";

    public static final String BLUEPRINT_LIST_PANEL = "BlueprintListPanel.js";

    public static final String CHANNEL_FORM_PANEL = "ChannelFormPanel.js";

    public static final String ALL = "channel-manager-all.js";

    public void bind(Component component) {
        if (Application.get().getDebugSettings().isAjaxDebugModeEnabled()) {
            component.add(JavascriptPackageResource.getHeaderContribution(ChannelManagerResourceBehaviour.class, ROOT_PANEL));
            component.add(JavascriptPackageResource.getHeaderContribution(ChannelManagerResourceBehaviour.class, BLUEPRINT_LIST_PANEL));
            component.add(JavascriptPackageResource.getHeaderContribution(ChannelManagerResourceBehaviour.class, CHANNEL_FORM_PANEL));
            component.add(JavascriptPackageResource.getHeaderContribution(ChannelPropertiesPanel.class, ChannelPropertiesPanel.CHANNEL_PROPERTIES_PANEL_JS));
            component.add(JavascriptPackageResource.getHeaderContribution(ChannelGridPanel.class, ChannelGridPanel.CHANNEL_GRID_PANEL_JS));
        } else {
            component.add(JavascriptPackageResource.getHeaderContribution(ChannelManagerResourceBehaviour.class, ALL));
        }
    }

}
