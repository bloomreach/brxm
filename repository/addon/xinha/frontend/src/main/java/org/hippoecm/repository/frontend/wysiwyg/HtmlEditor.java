package org.hippoecm.repository.frontend.wysiwyg;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.PropertyModel;

public abstract class HtmlEditor extends Panel {
    private static final long serialVersionUID = 1L;
    private String content;

    public HtmlEditor(final String id) {
        super(id);
        setModel(new PropertyModel(this, "content"));
    }

    public void setContent(String value) {
        content = value;
    }

    public String getContent() {
        return content;
    }

    public abstract void init();
}