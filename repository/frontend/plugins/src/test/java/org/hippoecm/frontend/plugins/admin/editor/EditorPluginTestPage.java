package org.hippoecm.frontend.plugins.admin.editor;

import org.apache.wicket.markup.html.WebPage;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.PluginDescriptor;

public class EditorPluginTestPage extends WebPage {
    private static final long serialVersionUID = 1L;

    public EditorPluginTestPage() {
        Application app = (Application) getSession().getApplication();
        PluginDescriptor editorDescriptor = new PluginDescriptor("editorPlugin", null);
        add(new EditorPlugin(editorDescriptor, new JcrNodeModel(null, app.node), null));
    }

}
