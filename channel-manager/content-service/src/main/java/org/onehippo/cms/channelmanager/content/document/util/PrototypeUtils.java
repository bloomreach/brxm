/*
 * Copyright 2022 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms.channelmanager.content.document.util;

import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.repository.api.HippoNodeType;
import org.onehippo.repository.util.JcrConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.repository.api.HippoNodeType.HIPPO_PROTOTYPES;
import static org.hippoecm.repository.api.HippoNodeType.NAMESPACES_PATH;
import static org.onehippo.cms.channelmanager.content.document.NodeFieldServiceImpl.SYSTEM_PREFIX;

public class PrototypeUtils {

    private static final Logger log = LoggerFactory.getLogger(PrototypeUtils.class);

    public static Node findFirstPrototypeNode(final Session session, final String type) {
        try {
            final String prototypesPath = getPrototypesLocation(type, session);
            if (!session.itemExists(prototypesPath) || !session.getItem(prototypesPath).isNode()) {
                return null;
            }
            final NodeIterator iter = ((Node) session.getItem(prototypesPath)).getNodes(HippoNodeType.HIPPO_PROTOTYPE);

            while (iter.hasNext()) {
                final Node node = iter.nextNode();
                if (!node.isNodeType(JcrConstants.NT_UNSTRUCTURED)) {
                    return node;
                }
            }
        } catch (final RepositoryException e) {
            log.error("An error occurred while looking up the prototype node for type '{}'", type, e);
        }

        return null;
    }

    public static String getPrototypesLocation(final String type, final Session session) throws RepositoryException {
        String prefix = SYSTEM_PREFIX;
        String subType = type;

        final int separatorIndex = type.indexOf(':');
        if (separatorIndex > 0) {
            prefix = type.substring(0, separatorIndex);
            subType = type.substring(separatorIndex + 1);
        }

        final String uri = getNamespaceURI(prefix, session);
        final String nsVersion = "_" + uri.substring(uri.lastIndexOf('/') + 1);

        final int prefixLength = prefix.length();
        final int nsVersionLength = nsVersion.length();

        if (prefixLength > nsVersionLength && nsVersion.equals(prefix.substring(prefixLength - nsVersionLength))) {
            prefix = prefix.substring(0, prefixLength - nsVersionLength);
        }

        return String.format("/%s/%s/%s/%s", NAMESPACES_PATH, prefix, subType, HIPPO_PROTOTYPES);
    }

    public static String getNamespaceURI(final String prefix, final Session session) throws RepositoryException {
        if (SYSTEM_PREFIX.equals(prefix)) {
            return "internal";
        }

        final NamespaceRegistry nsReg = session.getWorkspace().getNamespaceRegistry();
        return nsReg.getURI(prefix);
    }

}
