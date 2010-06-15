package org.hippoecm.frontend.widgets;

import org.apache.wicket.model.IModel;
import org.apache.wicket.util.time.Duration;

public class ThrottledTextFieldWidget extends TextFieldWidget {
    final static String SVN_ID = "$Id$";

    public static final Duration THROTTLE_DELAY = Duration.milliseconds(750);

    public ThrottledTextFieldWidget(String id, IModel<String> model) {
        super(id, model, null, THROTTLE_DELAY);
    }

    public ThrottledTextFieldWidget(String id, IModel<String> model, Duration throttleDelay) {
        super(id, model, null, throttleDelay);
    }

    public ThrottledTextFieldWidget(String id, IModel<String> model, IModel<String> labelModel) {
        super(id, model, labelModel, THROTTLE_DELAY);
    }

    public ThrottledTextFieldWidget(String id, IModel<String> model, IModel<String> labelModel, Duration throttleDelay) {
        super(id, model, labelModel, throttleDelay);
    }
}
