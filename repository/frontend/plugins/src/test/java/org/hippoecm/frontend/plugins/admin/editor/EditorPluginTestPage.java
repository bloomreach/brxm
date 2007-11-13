package org.hippoecm.frontend.plugins.admin.editor;

import org.apache.wicket.markup.html.WebPage;
import org.hippoecm.frontend.model.JcrTreeNode;
import org.hippoecm.frontend.plugin.PluginDescriptor;

public class EditorPluginTestPage extends WebPage {
    private static final long serialVersionUID = 1L;
    
    private MockJcr mockJcr;

    public EditorPluginTestPage() {
        mockJcr = new MockJcr();
        mockJcr.setUp();
        PluginDescriptor editorDescriptor = new PluginDescriptor("editorPlugin", null);
        add(new EditorPlugin(editorDescriptor, new JcrTreeNode(null, mockJcr.node), null));
    }

    public void tearDown() {
        mockJcr.tearDown();
    }

}
