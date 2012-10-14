package org.hippoecm.repository.concurrent.action;

import java.util.Random;

import javax.jcr.Node;

public class RenameAssetAction extends AbstractAssetActionsWorkflowAction {

    private final Random random = new Random(System.currentTimeMillis());
    
    @Override
    protected String getWorkflowMethodName() {
        return "move";
    }

    @Override
    protected Node doExecute(Node node) throws Exception {
        Node parent = node.getParent().getParent();
        String newName = node.getName();
        do {
            newName += "." + random.nextInt(10);
        } while (parent.hasNode(newName));
        getWorkflow(node).rename(newName);
        return parent.getNode(newName).getNode(newName);
    }
}
