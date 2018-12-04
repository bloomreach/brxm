/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.hippoecm.repository.deriveddata;

import java.util.Optional;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.repository.HippoStdNodeType.NT_DOCUMENT;

public class VariantFinder {

    private final Optional<Node> variant;
    private final Logger log = LoggerFactory.getLogger(VariantFinder.class);


    public VariantFinder(final Property accessedProperty) throws RepositoryException {
        log.debug("accessedProperty:{path:{}}", accessedProperty.getPath());
        if (accessedProperty == null) {
            this.variant = Optional.empty();
        } else {
            this.variant = constructVariantPathByProperty(accessedProperty);
        }
    }

    public VariantFinder(final Node modified) throws RepositoryException {
        this.variant = constructVariantPathByNode(modified);
    }

    public static Optional<Node> find(final Node node) throws RepositoryException {
        VariantFinder finder = new VariantFinder(node);
        return finder.find();
    }

    public static Optional<Node> find(final Property property) throws RepositoryException {
        VariantFinder finder = new VariantFinder(property);
        return finder.find();
    }

    public Optional<Node> find() {
        return this.variant;
    }


    private Optional<Node> constructVariantPathByProperty(Property property) throws RepositoryException {
        return constructVariantPathByNode(property.getParent());
    }

    private Optional<Node> constructVariantPathByNode(final Node descendantOfVariant) throws RepositoryException {
        Node traverse = descendantOfVariant;
        final Node rootNode = traverse.getSession().getRootNode();
        while (!traverse.isNodeType(NT_DOCUMENT)) {
            if (traverse.isSame(rootNode)) {
                return Optional.empty();
            }
            traverse = traverse.getParent();
        }
        return Optional.of(traverse);
    }

}
