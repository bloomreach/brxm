package org.hippoecm.frontend.plugins.admin.editor;

import org.apache.wicket.markup.html.WebPage;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugins.admin.editor.EditorPlugin;

public class EditorPluginTestPage extends WebPage {
    private static final long serialVersionUID = 1L;
    
    private MockJcr mockJcr;

    public EditorPluginTestPage() {
        mockJcr = new MockJcr();
        mockJcr.setUp();
        add(new EditorPlugin("editorPlugin", new JcrNodeModel(mockJcr.node)));
    }

    public void tearDown() {
        mockJcr.tearDown();
    }

}
