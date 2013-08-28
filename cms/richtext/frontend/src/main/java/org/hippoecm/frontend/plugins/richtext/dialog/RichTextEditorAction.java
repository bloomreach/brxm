package org.hippoecm.frontend.plugins.richtext.dialog;

import org.apache.wicket.util.io.IClusterable;

/**
 * Represents an action on a rich text editor instance.
 */
public interface RichTextEditorAction<ModelType> extends IClusterable {

    /**
     * Produces the JavaScript to execute this action on the client-side.
     * @param model the model that this action operates on.
     * @return JavaScript code that executes this action.
     */
    String getJavaScript(ModelType model);

}
