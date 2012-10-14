package org.hippoecm.repository.concurrent.action;

import java.util.Random;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.Query;

import org.hippoecm.repository.api.Document;

public class MoveDocumentAction extends AbstractFullReviewedActionsWorkflowAction {

    private final Random random = new Random(System.currentTimeMillis());
    
    public MoveDocumentAction(ActionContext context) {
        super(context);
    }
    
    @Override
    public boolean canOperateOnNode(Node node) throws Exception {
        if (node.isCheckedOut()) {
            return super.canOperateOnNode(node);
        }
        return false;
    }

    @Override
    protected String getWorkflowMethodName() {
        return "move";
    }

    @Override
    protected Node doExecute(Node node) throws Exception {
        Node targetFolder = selectRandomDocumentFolderNode(node);
        if (targetFolder != null) {
            String newName = "document";
            do {
                newName += random.nextInt(10);
            } while (targetFolder.hasNode(newName));
            getFullReviewedActionsWorkflow(node).move(new Document(targetFolder.getIdentifier()), newName);
            node.getSession().refresh(false);
        }
        return targetFolder;
    }
    
    private Node selectRandomDocumentFolderNode(Node node) throws RepositoryException {
        String stmt = "/jcr:root" + context.getDocumentBasePath() + "//element(*,hippostd:folder) order by @jcr:score descending";
        Query query = node.getSession().getWorkspace().getQueryManager().createQuery(stmt, Query.XPATH);
        NodeIterator folders = query.execute().getNodes();
        Node result = null;
        if (folders.getSize() > 1) {
            int index = random.nextInt((int)folders.getSize());
            if (index > 0) {
                folders.skip(index);
            }
            result = folders.nextNode();
            if (result.isSame(node.getParent().getParent())) {
                if (folders.hasNext()) {
                    result = folders.nextNode();
                }
                else {
                    result = null;
                }
            }
        }

        return result;
    }

}
