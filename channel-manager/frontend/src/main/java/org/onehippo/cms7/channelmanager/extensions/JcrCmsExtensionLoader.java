/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.channelmanager.extensions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JcrCmsExtensionLoader implements CmsExtensionLoader {

    private static final String CMS_EXTENSION_CONFIG_PATH = "/hippo:configuration/hippo:frontend/extensions";
    private static final String CMS_EXTENSION_CONTEXT = "context";
    private static final String CMS_EXTENSION_DISPLAY_NAME = "displayName";
    private static final String CMS_EXTENSION_URL_PATH = "urlPath";

    private static final Logger log = LoggerFactory.getLogger(JcrCmsExtensionLoader.class);

    private final Session session;

    public JcrCmsExtensionLoader(final Session session) {
        this.session = session;
    }

    @Override
    public List<CmsExtension> loadCmsExtensions() {
        try {
            return readExtensions();
        } catch (RepositoryException e) {
            log.warn("Could not load CMS extensions", e);
            return Collections.emptyList();
        }
    }

    private List<CmsExtension> readExtensions() throws RepositoryException {
        if (!session.nodeExists(CMS_EXTENSION_CONFIG_PATH)) {
            return Collections.emptyList();
        }

        final NodeIterator extensionNodes = session.getNode(CMS_EXTENSION_CONFIG_PATH).getNodes();
        final List<CmsExtension> extensions = new ArrayList<>();

        while (extensionNodes.hasNext()) {
            final Node extensionNode = extensionNodes.nextNode();
            final CmsExtension extension = readExtension(extensionNode);
            extensions.add(extension);
        }

        return extensions;
    }

    private CmsExtension readExtension(final Node extensionNode) throws RepositoryException {
        final CmsExtensionBean extension = new CmsExtensionBean();

        final String extensionId = extensionNode.getName();
        extension.setId(extensionId);

        readContext(extensionNode).ifPresent(extension::setContext);

        final String displayName = readProperty(extensionNode, CMS_EXTENSION_DISPLAY_NAME).orElse(extensionId);
        extension.setDisplayName(displayName);

        readProperty(extensionNode, CMS_EXTENSION_URL_PATH).ifPresent(extension::setUrlPath);

        return extension;
    }

    private Optional<CmsExtensionContext> readContext(final Node extensionNode) throws RepositoryException {
        if (extensionNode.hasProperty(CMS_EXTENSION_CONTEXT)) {
            final String contextProp = extensionNode.getProperty("context").getString();
            try {
                return Optional.of(CmsExtensionContext.valueOf(contextProp.toUpperCase()));
            } catch (IllegalArgumentException e) {
                log.info("Cannot convert '{}' to a CMS extension context", contextProp, e);
            }
        }
        return Optional.empty();
    }

    private Optional<String> readProperty(final Node extensionNode, final String propertyName) throws RepositoryException {
        if (extensionNode.hasProperty(propertyName)) {
            return Optional.of(extensionNode.getProperty(propertyName).getString());
        }
        return Optional.empty();
    }
}
