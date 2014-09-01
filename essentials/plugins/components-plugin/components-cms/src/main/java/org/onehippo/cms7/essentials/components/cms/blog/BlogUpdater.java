/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.components.cms.blog;

import java.util.ArrayList;
import java.util.Collection;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

import org.onehippo.cms7.essentials.components.cms.handlers.HandlerUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The BlogUpdater derives the bloggers' names from the linked Author document and stores them in the blog's
 * AuthorNames meta data field. That field is/will be used for faceted navigation.
 */
public final class BlogUpdater {

    private static final Logger log = LoggerFactory.getLogger(BlogUpdater.class);

    private BlogUpdater() {
    }

    /**
     * Indicate if the document variant represented by node should be handled.
     *
     * @param node JCR node to consider
     * @return true if the node is interesting, false otherwise.
     * @throws javax.jcr.RepositoryException
     */
    public static boolean wants(final Node node, final String documentType) throws RepositoryException {
        return node.getPrimaryNodeType().isNodeType(documentType);
    }

    /**
     * Handle the Save event for a blogpost document.
     *
     * @param blogpost JCR node representing the blogpost
     * @return indication whether or not changes need to be saved.
     * @throws javax.jcr.RepositoryException
     */
    public static boolean handleSaved(final Node blogpost, final String projectNamespace) throws RepositoryException {
        // Delete the old property
        final String authorNamesProp = projectNamespace + ":authornames";
        if (blogpost.hasProperty(authorNamesProp)) {
            blogpost.getProperty(authorNamesProp).remove();
        }
        // Construct the new property
        final NodeIterator authorMirrors = blogpost.getNodes(projectNamespace + ":authors");
        final Collection<String> authorNames = new ArrayList<String>();
        // TODO mm check this (was :title???)
        final String authorProperty = projectNamespace + ":fullname";
        while (authorMirrors.hasNext()) {
            final Node mirror = authorMirrors.nextNode();
            final Node author = HandlerUtils.getReferencedVariant(mirror, "published");

            if (author != null) {

                if (author.hasProperty(authorProperty)) {
                    final Property property = author.getProperty(authorProperty);
                    authorNames.add(property.getString());
                } else {
                    log.warn("Author node:[{}] has no property:[{}]", author.getPath(), authorProperty);
                }
            } else {
                log.warn("Author property couldn't be updated because referenced author node is not published yet: {}", mirror.getPath());
            }
        }

        if (authorNames.size() > 0) {
            blogpost.setProperty(authorNamesProp, authorNames.toArray(new String[authorNames.size()]));
        }
        return true;
    }
}
