/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.repository.bootstrap.instructions;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.onehippo.repository.bootstrap.InitializeInstruction;
import org.onehippo.repository.bootstrap.InitializeItem;
import org.onehippo.repository.bootstrap.PostStartupTask;

import static org.hippoecm.repository.api.HippoNodeType.HIPPO_CONTENTDELETE;
import static org.onehippo.repository.bootstrap.util.BootstrapConstants.log;

public class ContentDeleteInstruction extends InitializeInstruction {

    public ContentDeleteInstruction(final InitializeItem item, final Session session) {
        super(item, session);
    }

    @Override
    protected String getName() {
        return HIPPO_CONTENTDELETE;
    }

    @Override
    protected boolean canCombine(final InitializeInstruction instruction) {
        return instruction instanceof ContentResourceInstruction;
    }

    @Override
    public PostStartupTask execute() throws RepositoryException {
        final String path = item.getContentDeletePath();
        if (!path.startsWith("/")) {
            throw new RepositoryException(String.format("Invalid content delete item %s: %s is not an absolute path", item.getName(), path));
        }
        if ("/".equals(path)) {
            throw new RepositoryException(String.format("Invalid content delete item %s: can't delete root node", item.getName()));
        }
        if (session.nodeExists(path)) {
            final Node node = session.getNode(path);
            final Node parent = node.getParent();
            if (parent.getNodes(node.getName()).getSize() > 1) {
                log.warn("Removing same name sibling is not supported: not removing {} on behalf of item {}",
                        path, item.getName());
            } else {
                node.remove();
            }
        } else {
            log.info("Content delete {} node {} not found", item.getName(), path);
        }

        return null;
    }


}
