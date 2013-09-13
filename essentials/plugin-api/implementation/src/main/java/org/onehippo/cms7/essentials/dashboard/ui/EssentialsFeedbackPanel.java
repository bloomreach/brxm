package org.onehippo.cms7.essentials.dashboard.ui;

import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.feedback.IFeedbackMessageFilter;
import org.apache.wicket.markup.html.panel.FeedbackPanel;

/**
 *
 * @version "$Id: EssentialsFeedbackPanel.java 171192 2013-07-22 10:03:46Z mmilicevic $"
 */
public class EssentialsFeedbackPanel extends FeedbackPanel {

    private static final long serialVersionUID = 1L;

    public EssentialsFeedbackPanel(String id) {
        super(id);
    }

    public EssentialsFeedbackPanel(String id, IFeedbackMessageFilter filter) {
        super(id, filter);
    }

    @Override
    protected String getCSSClass(FeedbackMessage message) {
        String css;
        switch (message.getLevel()) {
            case FeedbackMessage.SUCCESS:
                css = "alert alert-success";
                break;
            case FeedbackMessage.INFO:
                css = "alert alert-info";
                break;
            case FeedbackMessage.ERROR:
                css = "alert alert-danger";
                break;
            default:
                css = "alert";
        }

        return css;
    }
}
