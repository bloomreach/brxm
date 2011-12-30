package org.hippoecm.frontend.plugins.console.editor;

import org.apache.wicket.ResourceReference;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.panel.Panel;

/**
 * A clickable header that toggles the visbility of another element by calling the javascript function 'toggleBox'.
 */
class ToggleHeader extends Panel {

    private final String name;

    /**
     * Creates a clickable header.
     *
     * @param id the Wicket ID of this component
     * @param name the name of the header (used as a parameter in the toggleBox() javascript call)
     * @param text the text the display in the header
     */
    public ToggleHeader(String id, String name, String text) {
        super(id);
        this.name = name;

        final Label textLabel = new Label("text", text);
        add(textLabel);

        final Image toggleImage = new Image("toggle-icon", new ResourceReference(ToggleHeader.class, "group-expanded.png"));
        toggleImage.setMarkupId("toggle-" + name);
        toggleImage.setOutputMarkupId(true);
        add(toggleImage);
    }

    @Override
    protected void onComponentTag(final ComponentTag tag) {
        super.onComponentTag(tag);
        tag.put("class", "toggle-header");
        tag.put("onclick", "javascript:toggleBox('" + name + "');");
    }
}

