package org.onehippo.cms7.essentials.installer.panels;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;

/**
 * @author Jeroen Reijn
 */
public class BodyPanel extends Panel {
    private static final long serialVersionUID = 1L;

    public BodyPanel(final String id) {
        super(id);
        setOutputMarkupId(true);
        setOutputMarkupPlaceholderTag(true);
        add(new Label("plugin"));

    }
}
