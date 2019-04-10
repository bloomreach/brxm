package org.hippoecm.frontend.editor.plugins.openui;

import java.io.Serializable;

import org.apache.wicket.Component;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Wicket component that loads an Open UI extension.
 */
public interface OpenUiPlugin extends Serializable {

    /**
     * Mimics {@link Component#setOutputMarkupId(boolean)}.
     */
    Component setOutputMarkupId(boolean output);

    /**
     * Mimics {@link Component#getMarkupId()}.
     */
    String getMarkupId();

    /**
     * @return the parameters specific to the JavaScript class of the Open UI extension.
     * Will be provided to the constructor of that class.
     */
    ObjectNode getJavaScriptParameters();

}
