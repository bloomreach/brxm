package org.onehippo.cms7.essentials.installer;

import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.feedback.IFeedbackMessageFilter;
import org.apache.wicket.markup.html.panel.FeedbackPanel;

/**
 * @version "$Id$"
 */
public class EssentialsGlobalFeedBackPanel extends FeedbackPanel {
    private static final long serialVersionUID = 1L;

    public EssentialsGlobalFeedBackPanel(String id) {
        super(id);
    }

    public EssentialsGlobalFeedBackPanel(String id, IFeedbackMessageFilter filter) {
        super(id, filter);
    }

    @Override
    protected String getCSSClass(FeedbackMessage message) {
        String css;
        switch (message.getLevel()) {
            case FeedbackMessage.SUCCESS:
                css = "alert success";
                break;
            case FeedbackMessage.INFO:
                css = "alert info";
                break;
            case FeedbackMessage.ERROR:
                css = "alert error";
                break;
            default:
                css = "alert";
        }

        return css;
    }
}
