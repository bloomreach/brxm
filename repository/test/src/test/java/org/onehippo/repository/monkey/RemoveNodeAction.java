/*
 * Copyright 2014-2016 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.repository.monkey;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class RemoveNodeAction extends Action {

    static final Logger log = LoggerFactory.getLogger(RemoveNodeAction.class);

    private final String relPath;

    protected RemoveNodeAction(String relPath) {
        super("removeNode-" + relPath);
        this.relPath = relPath;
    }

    @Override
    boolean execute(final Session s) throws RepositoryException {
        return removeNode(s.getNode("/test"), relPath);
    }

    private boolean removeNode(final Node node, final String relPath) throws RepositoryException {
        if (node.hasNode(relPath)) {
            final Node target = node.getNode(relPath);
            final String id = target.getIdentifier();
            target.remove();
            log.info("removed node {}/{} with id={}", node.getPath(), relPath, id);
            return true;
        }
        return false;
    }

}
