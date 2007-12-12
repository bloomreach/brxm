package org.hippoecm.frontend.plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.wicket.IClusterable;
import org.hippoecm.frontend.model.JcrNodeModel;

public class PluginEvent implements IClusterable {
    private static final long serialVersionUID = 1L;

    private Set<EventChannel> channels;
    private Map<String, JcrNodeModel> events;
    
    public PluginEvent(Plugin plugin, String type, JcrNodeModel nodeModel) {
        this.channels = plugin.getDescriptor().getOutgoing();
        this.events = new HashMap<String, JcrNodeModel>();
        this.events.put(type, nodeModel.findValidParentModel());
    }
    
    public PluginEvent(Plugin plugin) {
        this.channels = plugin.getDescriptor().getOutgoing();
        this.events = new HashMap<String, JcrNodeModel>();
    }

    public void chainEvent(String type, JcrNodeModel nodeModel) {
        events.put(type, nodeModel.findValidParentModel());
    }
            
    public JcrNodeModel getNodeModel(String type) {
        return events.get(type);
    }

    public Set<EventChannel> getChannels() {
        return channels;
    }
    
    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("channel", channels)
            .append("events", events)
            .toString();
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof PluginEvent == false) {
            return false;
        }
        if (this == object) {
            return true;
        }
        PluginEvent event = (PluginEvent) object;
        return new EqualsBuilder()
            .append(channels, event.channels)
            .append(events, event.events)
            .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(11, 99)
            .append(channels)
            .append(events)
            .toHashCode();
    }

}