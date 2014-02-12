/*
 * Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *  http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.repository.concurrent.action;

import java.util.NoSuchElementException;
import java.util.Random;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.Query;

import org.hippoecm.repository.api.Document;

public class MoveDocumentAction extends AbstractDocumentWorkflowAction {

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
            Node handle = node.getParent();
            getDocumentWorkflow(handle).move(new Document(targetFolder), newName);
            node.getSession().refresh(false);
        }
        return targetFolder;
    }
    
    private Node selectRandomDocumentFolderNode(Node node) throws RepositoryException {
        String stmt = "/jcr:root" + context.getDocumentBasePath() + "//element(*,hippostd:folder) order by @jcr:score descending";
        Query query = node.getSession().getWorkspace().getQueryManager().createQuery(stmt, Query.XPATH);
        NodeIterator folders = query.execute().getNodes();
        Node result = null;
        try {
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
        }
        catch (NoSuchElementException e) {
            // guard against and ignore possibly no longer existing folders
            result = null;
        }
        return result;
    }

}
