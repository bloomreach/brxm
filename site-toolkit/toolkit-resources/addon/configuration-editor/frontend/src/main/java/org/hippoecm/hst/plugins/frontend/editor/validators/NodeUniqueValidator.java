package org.hippoecm.hst.plugins.frontend.editor.validators;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.validation.IValidatable;
import org.hippoecm.hst.plugins.frontend.editor.domain.BeanProvider;
import org.hippoecm.hst.plugins.frontend.editor.domain.EditorBean;

public class NodeUniqueValidator<K extends EditorBean> extends EditorValidator<K> {
    private static final long serialVersionUID = 1L;

    public NodeUniqueValidator(BeanProvider<K> provider) {
        super(provider);
    }

    @Override
    protected void onValidate(IValidatable validatable, K bean) {
        String newNodeName = getNewNodeName(validatable, bean);
        Node node = bean.getModel().getNode();
        try {
            if (newNodeName.equals(node.getName())) {
                return;
            }
            if (node.getParent().hasNode(newNodeName)) {
                error("node-unique-validator.error");
            }
        } catch (RepositoryException e) {
            log.error("Error validating node name", e);
        }
    }

    protected String getNewNodeName(IValidatable validatable, K bean) {
        return (String) validatable.getValue();
    }

}
