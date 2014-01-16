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

import java.util.Random;

import javax.jcr.Node;

import org.hippoecm.repository.util.NodeIterable;

public class JanitorAction extends Action {

    private Random random = new Random();

    public JanitorAction(final ActionContext context) {
        super(context);
    }

    @Override
    public boolean canOperateOnNode(final Node node) throws Exception {
        if (random.nextGaussian() < .1) {
            if (node.getNodes().getSize() > 100) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isWriteAction() {
        return true;
    }

    @Override
    protected Node doExecute(final Node node) throws Exception {
        int count = 0;
        for (Node child : new NodeIterable(node.getNodes())) {
            count++;
            if (count > 10) {
                child.remove();
            }
            try {
                node.getSession().save();
            } catch (AssertionError e) {
                context.getLog().error("TODO: " + e);
            }
        }
        return node;
    }
}
