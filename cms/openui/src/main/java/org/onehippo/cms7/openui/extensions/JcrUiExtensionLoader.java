/*
 * Copyright 2018-2021 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.openui.extensions;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.frontend.util.InterpolationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.frontend.FrontendNodeType.FRONTEND_CONFIG;
import static org.hippoecm.frontend.FrontendNodeType.FRONTEND_DISPLAY_NAME;
import static org.hippoecm.frontend.FrontendNodeType.FRONTEND_EXTENSION_POINT;
import static org.hippoecm.frontend.FrontendNodeType.FRONTEND_INITIAL_HEIGHT_IN_PIXELS;
import static org.hippoecm.frontend.FrontendNodeType.FRONTEND_URL;
import static org.hippoecm.frontend.FrontendNodeType.UI_EXTENSIONS_CONFIG_PATH;

public class JcrUiExtensionLoader implements UiExtensionLoader {

    private static final Logger log = LoggerFactory.getLogger(JcrUiExtensionLoader.class);

    private final Session session;

    public JcrUiExtensionLoader(final Session session) {
        this.session = session;
    }

    @Override
    public Set<UiExtension> loadUiExtensions() {
        try {
            return readExtensions();
        } catch (RepositoryException e) {
            log.warn("Could not load UI extensions", e);
            return Collections.emptySet();
        }
    }

    @Override
    public Optional<UiExtension> loadUiExtension(final String extensionName, final UiExtensionPoint extensionPoint) {
        try {
            return readExtension(session.getNode(UI_EXTENSIONS_CONFIG_PATH + "/" + extensionName))
                    .filter(e -> e.getExtensionPoint().equals(extensionPoint));
        } catch (RepositoryException e) {
            log.warn("Could not load UI extension '" + extensionName + "'.");
        }
        return Optional.empty();
    }

    private Set<UiExtension> readExtensions() throws RepositoryException {
        if (!session.nodeExists(UI_EXTENSIONS_CONFIG_PATH)) {
            return Collections.emptySet();
        }

        final NodeIterator extensionNodes = session.getNode(UI_EXTENSIONS_CONFIG_PATH).getNodes();
        final Set<UiExtension> extensions = new LinkedHashSet<>();

        while (extensionNodes.hasNext()) {
            final Node extensionNode = extensionNodes.nextNode();
            readExtension(extensionNode).ifPresent(node -> {
                if (!extensions.add(node)) {
                    log.warn("Duplicate extensions found. Only the first extension with ID '{}' is loaded.",
                            node.getId());
                }
            });
        }

        return extensions;
    }

    private Optional<UiExtension> readExtension(final Node extensionNode) throws RepositoryException {
        final UiExtensionBean extension = new UiExtensionBean();

        final String extensionId = extensionNode.getName();
        extension.setId(extensionId);

        extension.setExtensionPoint(readExtensionPoint(extensionNode));

        final String displayName = readStringProperty(extensionNode, FRONTEND_DISPLAY_NAME).orElse(extensionId);
        extension.setDisplayName(displayName);

        readStringProperty(extensionNode, FRONTEND_URL).ifPresent(extension::setUrl);
        readStringProperty(extensionNode, FRONTEND_CONFIG).ifPresent((value) -> {
            extension.setConfig(InterpolationUtils.interpolate(value));
        });
        readIntProperty(extensionNode, FRONTEND_INITIAL_HEIGHT_IN_PIXELS).ifPresent(extension::setInitialHeightInPixels);

        final UiExtensionValidator validator = new UiExtensionValidator();
        if (validator.validate(extension)) {
            return Optional.of(extension);
        }
        return Optional.empty();
    }

    private UiExtensionPoint readExtensionPoint(final Node extensionNode) throws RepositoryException {
        return readStringProperty(extensionNode, FRONTEND_EXTENSION_POINT)
                .map(UiExtensionPoint::getByConfigValue)
                .orElse(UiExtensionPoint.UNKNOWN);
    }

    private Optional<String> readStringProperty(final Node extensionNode, final String propertyName) throws RepositoryException {
        if (extensionNode.hasProperty(propertyName)) {
            return Optional.of(extensionNode.getProperty(propertyName).getString());
        }
        return Optional.empty();
    }

    private Optional<Integer> readIntProperty(final Node extensionNode, final String propertyName) throws RepositoryException {
        if (extensionNode.hasProperty(propertyName)) {
            final long value = extensionNode.getProperty(propertyName).getLong();
            return Optional.of((int) value);
        }
        return Optional.empty();
    }
}
