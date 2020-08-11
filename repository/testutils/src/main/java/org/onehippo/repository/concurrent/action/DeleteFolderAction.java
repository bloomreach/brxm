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

public class DeleteFolderAction extends AbstractFolderWorkflowAction {

    public DeleteFolderAction(ActionContext context) {
        super(context);
    }

    @Override
    public boolean canOperateOnNode(Node node) throws Exception {
        if (context.isBasePath(node.getPath())) {
            return false;
        }
        if (node.hasNodes()) {
            return false;
        }
        return super.canOperateOnNode(node);
    }

    @Override
    protected String getWorkflowMethodName() {
        return "delete";
    }

    @Override
    protected Node doExecute(Node node) throws Exception {
        Node parent = node.getParent();
        context.getFolderWorkflow(parent).delete(node.getName() + (node.getIndex() > 1 ? "[" + node.getIndex() + "]" : ""));
        parent.getSession().refresh(false);
        return parent;
    }

}
