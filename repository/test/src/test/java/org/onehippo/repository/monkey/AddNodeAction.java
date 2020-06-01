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

class AddNodeAction extends Action {

    static final Logger log = LoggerFactory.getLogger(AddNodeAction.class);

    private final String relPath;

    protected AddNodeAction(String relPath) {
        super("addNode-" + relPath);
        this.relPath = relPath;
    }

    @Override
    boolean execute(final Session s) throws RepositoryException {
        return addNode(s.getNode("/test"), relPath);
    }

    private boolean addNode(final Node node, final String relPath) throws RepositoryException {
        int offset = relPath.indexOf('/');
        if (offset != -1) {
            final String childPath = relPath.substring(0, offset);
            final String restPath = relPath.substring(offset+1);
            if (node.hasNode(childPath)) {
                final Node child = node.getNode(childPath);
                return addNode(child, restPath);
            }
            else {
                return false;
            }
        } else {
            if (node.hasNode(relPath)) {
                return false;
            }
            final Node newNode = node.addNode(relPath);
            log.info("added node {}/{}, new id={}", node.getPath(), relPath, newNode.getIdentifier());
        }
        return true;
    }

}
