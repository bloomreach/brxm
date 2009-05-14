package org.hippoecm.hst.plugins.frontend.editor.dialogs;

import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.editor.plugins.linkpicker.LinkPickerDialog;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class HstPickerDialog extends LinkPickerDialog {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(HstPickerDialog.class);

    public HstPickerDialog(IPluginContext context, IPluginConfig config, IModel model, List<String> nodetypes) {
        super(context, config, model, nodetypes);
    }

    @Override
    protected boolean isValidSelection(IModel targetModel) {
        if (targetModel == null || targetModel.getObject() == null) {
            return false;
        }

        try {
            Node node = (Node) targetModel.getObject();
            if (node.isNodeType(HippoNodeType.NT_HANDLE) && node.hasNode(node.getName())) {
                node = node.getNode(node.getName());
            }
            return isValidSelection(node);

        } catch (RepositoryException e) {
            log.error("An error occured during validation of targetModel", e);
        }
        return false;
    }

    protected abstract boolean isValidSelection(Node node) throws RepositoryException;

    @Override
    protected IModel getInitialNode() {
        String path = getModelObjectAsString();
        if (path != null && !"".equals(path)) {
            return new JcrNodeModel(path);
        }
        return null;
    }
}
