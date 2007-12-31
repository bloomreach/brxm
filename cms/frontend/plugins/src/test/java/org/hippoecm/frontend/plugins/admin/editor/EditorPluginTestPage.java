package org.hippoecm.frontend.plugins.admin.editor;

import org.apache.wicket.markup.html.WebPage;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.PluginDescriptor;
import org.hippoecm.frontend.plugin.channel.Channel;

public class EditorPluginTestPage extends WebPage {
    private static final long serialVersionUID = 1L;

    public EditorPluginTestPage() {
        Application app = (Application) getSession().getApplication();
        Channel outgoing = null;
        PluginDescriptor editorDescriptor = new PluginDescriptor("editorPlugin", null, outgoing);
        add(new EditorPlugin(editorDescriptor, new JcrNodeModel(app.node), null));
    }

}
