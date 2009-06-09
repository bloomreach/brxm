package org.hippoecm.frontend.util;

import org.hippoecm.frontend.i18n.model.NodeTranslator;
import org.hippoecm.frontend.model.JcrNodeModel;

public class MaxLengthNodeNameFormatter extends MaxLengthStringFormatter {
    private static final long serialVersionUID = 1L;

    public MaxLengthNodeNameFormatter() {
        super();
    }

    public MaxLengthNodeNameFormatter(int maxLength, String split, int indentLength) {
        super(maxLength, split, indentLength);
    }

    public boolean isTooLong(JcrNodeModel nodeModel) {
        return isTooLong(nodeModel, 0);
    }

    public boolean isTooLong(JcrNodeModel nodeModel, int indent) {
        return super.isTooLong(getName(nodeModel), indent);
    }

    public String parse(JcrNodeModel nodeModel, int indent) {
        return super.parse(getName(nodeModel), indent);
    }

    public String parse(JcrNodeModel nodeModel) {
        return parse(nodeModel, 0);
    }

    protected String getName(JcrNodeModel nodeModel) {
        return (String) new NodeTranslator(nodeModel).getNodeName().getObject();
    }
}
