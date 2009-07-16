package org.hippoecm.hst.plugins.frontend.editor.dialogs;

import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.hst.plugins.frontend.editor.context.HstContext;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HstContentPickerDialog extends HstPickerDialog {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(HstContentPickerDialog.class);

    public HstContentPickerDialog(final IPluginContext context, IPluginConfig config, IModel model,
            List<String> nodetypes) {
        super(context, new HstPickerConfig(config) {
            private static final long serialVersionUID = 1L;

            @Override
            protected String getPath() {
                HstContext hc = context.getService(HstContext.class.getName(), HstContext.class);
                return hc.content.getPath();
            }

        }, model, nodetypes);
    }

    @Override
    protected boolean isValidSelection(Node node) throws RepositoryException {
        //See if node is a hippo:softdocument that has a hst:site ancestor
        if (node.isNodeType(HippoNodeType.NT_SOFTDOCUMENT)) {
            Node rootNode = node.getSession().getRootNode();
            node = node.getParent();
            while (!node.equals(rootNode)) {
                if (node.isNodeType("hst:site")) {
                    return true;
                }
                node = node.getParent();
            }
        }
        return false;
    }
}
