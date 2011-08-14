package org.hippoecm.frontend.editor;

import javax.jcr.Node;

import org.apache.wicket.IClusterable;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.EditorException;
import org.hippoecm.frontend.service.IEditor;

public interface IEditorFactory extends IClusterable {

    String SERVICE_ID = "editor.factory.id";

    /**
     * Create an editor for a node model.  Returns null if no editor can be created.
     * 
     * @param manager
     * @param nodeModel
     * @param mode
     * @return
     * @throws EditorException
     */
    IEditor<Node> newEditor(IEditorContext manager, IModel<Node> nodeModel, IEditor.Mode mode, IPluginConfig parameters)
            throws EditorException;

}
