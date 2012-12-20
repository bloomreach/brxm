package org.hippoecm.frontend.dialog;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;

/**
 * Loadable detachable model that wraps a delegate string model and removes all line breaks from the delegate model
 * string once it is loaded.
 */
class StringWithoutLineBreaksModel extends LoadableDetachableModel<String> {

    private static final String LINE_BREAKS_REGEX = "(\\r|\\n)";

    private IModel<String> delegate;

    StringWithoutLineBreaksModel(IModel<String> delegate) {
        this.delegate = delegate;
    }

    @Override
    protected String load() {
        if (delegate != null) {
            final String mayContainLineBreaks = delegate.getObject();
            if (mayContainLineBreaks != null) {
                return mayContainLineBreaks.replaceAll(LINE_BREAKS_REGEX, "");
            }
        }
        return null;
    }

}
