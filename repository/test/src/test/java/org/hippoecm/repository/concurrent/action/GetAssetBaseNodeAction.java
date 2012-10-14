package org.hippoecm.repository.concurrent.action;

import javax.jcr.Node;

public class GetAssetBaseNodeAction extends Action {

    public GetAssetBaseNodeAction(ActionContext context) {
        super(context);
    }

    @Override
    public boolean canOperateOnNode(Node node) throws Exception {
        return !node.getPath().startsWith(context.getAssetBasePath());
    }

    @Override
    protected Node doExecute(Node node) throws Exception {
        return node.getSession().getNode(context.getAssetBasePath());
    }

    @Override
    public double getWeight() {
        return 0.3;
    }

    @Override
    public boolean isWriteAction() {
        return false;
    }

}
