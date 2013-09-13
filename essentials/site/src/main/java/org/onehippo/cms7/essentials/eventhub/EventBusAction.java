package org.onehippo.cms7.essentials.eventhub;

public class EventBusAction {

    private String type;
    private String channel;

    public EventBusAction() {
    }

    public String getType() {
        return type;
    }

    public void setType(final String type) {
        this.type = type;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(final String channel) {
        this.channel = channel;
    }
}
