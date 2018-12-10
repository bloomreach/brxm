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
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

import org.apache.commons.lang.Validate;
import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.repository.HippoStdNodeType.NT_DOCUMENT;
import static org.hippoecm.repository.HippoStdNodeType.UNPUBLISHED;

public class VariantFinder {

    private static final Logger log = LoggerFactory.getLogger(VariantFinder.class);
    private final Optional<Node> variant;


    public VariantFinder(final Property accessedProperty) throws RepositoryException {
        if (accessedProperty == null) {
            this.variant = Optional.empty();
        } else {
            log.debug("accessedProperty:{path:{}}", accessedProperty.getPath());
            this.variant = constructVariantPathByProperty(accessedProperty);
        }
    }

    public VariantFinder(final Node modified) throws RepositoryException {
        if (modified == null) {
            this.variant = Optional.empty();
        } else {
            this.variant = constructVariantPathByNode(modified);
        }
    }

    public static Optional<Node> find(final Node node) throws RepositoryException {
        VariantFinder finder = new VariantFinder(node);
        final Optional<Node> findVariant = finder.find();
        if (findVariant.isPresent()) {
            log.debug("Found variant:{path:{}} for node:{path:{}}", findVariant.get().getPath(), node.getPath());
        } else {
            log.debug("Could not find variant for node:{path:{}}", node.getPath());
        }
        return findVariant;
    }

    public static Optional<Node> find(final Property property) throws RepositoryException {
        VariantFinder finder = new VariantFinder(property);
        final Optional<Node> findVariant = finder.find();
        if (findVariant.isPresent()) {
            log.debug("Found variant:{path:{}} for property:{path:{}}", findVariant.get().getPath(), property.getPath());
        } else {
            if (property == null) {
                log.debug("Property is null, could not find variant");
            } else {
                log.debug("Could not find variant for property:{path:{}}", property.getPath());
            }
        }
        return findVariant;
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

    public Optional<Node> findUnpublished() throws RepositoryException {
        final Optional<Node> findVariant = find();
        if (findVariant.isPresent()) {
            return findUnpublished(findVariant.get());
        }
        return Optional.empty();
    }

    private Optional<Node> findUnpublished(final Node variant) throws RepositoryException {
        Validate.isTrue(variant.isNodeType(NT_DOCUMENT));
        log.debug("Find unpublished variant for variant:{path:{}}", JcrUtils.getNodePathQuietly(variant));
        final NodeIterator nodes = variant.getParent().getNodes(variant.getName());
        while (nodes.hasNext()) {
            final Node nextVariant = nodes.nextNode();
            if (isUnpublished(nextVariant)) {
                return Optional.of(nextVariant);
            }
        }
        return Optional.empty();

    }

    private boolean isUnpublished(Node variant) throws RepositoryException {
        Validate.isTrue(variant.isNodeType(NT_DOCUMENT)
                , String.format("Node:{path:%s} should be of type: %s", variant.getPath(), NT_DOCUMENT));
        Validate.isTrue(variant.hasProperty(HippoStdNodeType.HIPPOSTD_STATE)
                , String.format("Node:{path:%s} should have property: %s", variant.getPath(), HippoStdNodeType.HIPPOSTD_STATE));
        return UNPUBLISHED.equals(variant.getProperty(HippoStdNodeType.HIPPOSTD_STATE).getString());

    }
}
