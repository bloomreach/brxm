package org.hippoecm.hst.core.template.node.el;

import java.util.Map;

import javax.jcr.Node;

public interface ELNode {
    public Map getProperty();
    public Node getJcrNode();
    public String getDecodedName();
    public String getName();
}
