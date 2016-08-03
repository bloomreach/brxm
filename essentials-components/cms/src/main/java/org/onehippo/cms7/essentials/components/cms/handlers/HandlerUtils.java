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

package org.onehippo.cms7.essentials.components.cms.handlers;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * @version "$Id$"
 */
public final class HandlerUtils {



    private HandlerUtils() {
    }

    /**
     * @param handle JCR node representing a handle
     * @param state  desired state of the variant
     * @return JCR node representing that variant, or null.
     * @throws javax.jcr.RepositoryException
     */
    public static Node getVariant(final Node handle, final String state) throws RepositoryException {
        final NodeIterator variants = handle.getNodes(handle.getName());
        while (variants.hasNext()) {
            final Node variant = variants.nextNode();
            if (variant.hasProperty("hippostd:state") && variant.getProperty("hippostd:state").getString().equals(state)) {
                return variant;
            }
        }
        return null;
    }

    /**
     * Helper function to derive a certain document variant, given a hippo:mirror node.
     * Optimally, the CMS or repository would provide this functionality.
     *
     * @param mirror repository node of type hippo:mirror
     * @param state  desired state of the variant
     * @return JCR node representing that variant, or null.
     * @throws javax.jcr.RepositoryException
     */
    public static Node getReferencedVariant(final Node mirror, final String state) throws RepositoryException {
        final Session session = mirror.getSession();
        final String rootUuid = session.getRootNode().getIdentifier();
        final String uuid = mirror.getProperty("hippo:docbase").getString();
        Node variant = null;
        if (!rootUuid.equals(uuid)) {
            final Node authorHandle = session.getNodeByIdentifier(uuid);
            variant = getVariant(authorHandle, state);
        }
        return variant;
    }
}
