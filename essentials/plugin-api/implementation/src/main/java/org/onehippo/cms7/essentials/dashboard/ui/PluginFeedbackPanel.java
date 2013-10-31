package org.onehippo.cms7.essentials.dashboard.ui;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.feedback.IFeedbackMessageFilter;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.Model;

/**
 * @version "$Id: PluginFeedbackPanel.java 171536 2013-07-24 12:41:00Z mmilicevic $"
 */
public class PluginFeedbackPanel extends FeedbackPanel {

    private static final long serialVersionUID = 1L;


    public PluginFeedbackPanel(final String id, final IFeedbackMessageFilter filter) {
        super(id, filter);
    }

    public PluginFeedbackPanel(final String id) {
        super(id);
    }

    @Override
    protected Component newMessageDisplayComponent(final String id, final FeedbackMessage message) {
        final Component newMessageDisplayComponent = super.newMessageDisplayComponent(id, message);

        String clazz = "alert-info";
        switch (message.getLevel()) {
            case FeedbackMessage.DEBUG:
                clazz = "alert-info";
                break;
            case FeedbackMessage.ERROR:
                clazz = "alert-error";
                break;
            case FeedbackMessage.FATAL:
                clazz = "alert-error";
                break;
            case FeedbackMessage.SUCCESS:
                clazz = "alert-success";
                break;
            case FeedbackMessage.INFO:
                clazz = "alert-success";
                break;
            case FeedbackMessage.WARNING:
                clazz = "alert-block";
        }

        newMessageDisplayComponent.add(new AttributeAppender("class", new Model<>(clazz), " "));
        return newMessageDisplayComponent;
    }
}
