package org.hippoecm.hst.plugins.frontend.editor.dialogs;

import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HstComponentPickerDialog extends HstPickerDialog {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(HstComponentPickerDialog.class);

    public HstComponentPickerDialog(IPluginContext context, IPluginConfig config, IModel model, List<String> nodetypes) {
        super(context, config, model, nodetypes);
    }

    @Override
    protected boolean isValidSelection(Node node) throws RepositoryException {
        return false;
    }
}
