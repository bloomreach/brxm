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
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.onehippo.cms7.essentials.components.cms.modules.EventBusListenerModule;


/**
 * The BlogUpdater derives the bloggers' names from the linked Author document and stores them in the blog's
 * AuthorNames meta data field. That field is/will be used for faceted navigation.
 */
public final class BlogUpdater {

    private static final String TYPE_BLOGPOST = "connect:blogpost";
    private static final String PROP_BLOGPOST_AUTHOR_NAMES = "connect:authornames";
    private static final String NODE_BLOGPOST_AUTHORS = "connect:authors";
    private static final String PROP_AUTHOR_NAME = "connect:title";

    private BlogUpdater() {
    }

    /**
     * Indicate if the document variant represented by node should be handled.
     * @param node JCR node to consider
     * @return     true if the node is interesting, false otherwise.
     * @throws javax.jcr.RepositoryException
     */
    public static boolean wants(final Node node) throws RepositoryException {
        return node.getPrimaryNodeType().isNodeType(TYPE_BLOGPOST);
    }

    /**
     * Handle the Save event for a blogpost document.
     * @param blogpost JCR node representing the blogpost
     * @return         indication whether or not changes need to be saved.
     * @throws javax.jcr.RepositoryException
     */
    public static boolean handleSaved(final Node blogpost) throws RepositoryException {
        // Delete the old property
        if (blogpost.hasProperty(PROP_BLOGPOST_AUTHOR_NAMES)) {
            blogpost.getProperty(PROP_BLOGPOST_AUTHOR_NAMES).remove();
        }


        // Construct the new property
        NodeIterator authorMirrors = blogpost.getNodes(NODE_BLOGPOST_AUTHORS);
        List<String> authorNames = new ArrayList<String>();
        while (authorMirrors.hasNext()) {
            final Node mirror = authorMirrors.nextNode();
            final Node author = EventBusListenerModule.getReferencedVariant(mirror, "published");
            if (author != null) {
                authorNames.add(author.getProperty(PROP_AUTHOR_NAME).getString());
            }
        }

        if (authorNames.size() > 0) {
            blogpost.setProperty(PROP_BLOGPOST_AUTHOR_NAMES, authorNames.toArray(new String[authorNames.size()]));
        }
        return true;
    }
}
