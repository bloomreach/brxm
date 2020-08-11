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
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class RemovePropertyAction extends Action {

    static final Logger log = LoggerFactory.getLogger(RemovePropertyAction.class);

    private final String relPath;

    protected RemovePropertyAction(final String relPath) {
        super("removeProperty-" + relPath);
        this.relPath = relPath;
    }

    @Override
    boolean execute(final Session s) throws RepositoryException {
        return removeProperty(s.getNode("/test"), relPath);
    }

    private boolean removeProperty(final Node node, final String relPath) throws RepositoryException {
        if (node.hasProperty(relPath)) {
            final Property property = node.getProperty(relPath);
            final Node parent = property.getNode();
            final String name = property.getName();
            property.remove();
            log.info("removed property {} from node {} with id={}", name, parent.getPath(), parent.getIdentifier());
            return true;
        }
        return false;
    }
}
