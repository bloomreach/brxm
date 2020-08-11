/*
 *  Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.onehippo.repository.concurrent.action;

import javax.jcr.Node;

public class AddNewsDocumentAction extends AbstractFolderWorkflowAction {
    
    public AddNewsDocumentAction(ActionContext context) {
        super(context);
    }

    @Override
    protected Node doExecute(Node node) throws Exception {
        String newName = "document";
        do {
            newName +=  random.nextInt(10);
        } while (node.hasNode(newName));
        String absPath = context.getFolderWorkflow(node).add("new-document", "testcontent:news", newName);
        node.getSession().refresh(false);
        return node.getSession().getNode(absPath);
    }

    @Override
    protected String getWorkflowMethodName() {
        return "add";
    }

    @Override
    public boolean canOperateOnNode(Node node) throws Exception {
        if (node.getPath().startsWith(context.getDocumentBasePath())) {
            return super.canOperateOnNode(node);
        }
        return false;
    }
    
}
