/*
 * Copyright 2016 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms.channelmanager.content.documenttype.util;

import java.util.Locale;
import java.util.Optional;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.NodeNameCodec;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.repository.l10n.LocalizationService;
import org.onehippo.repository.l10n.ResourceBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * LocalizationHelper provides functionality for determining correctly localized document type and field names and hints.
 */
public class LocalizationUtils {
    // These are also defined in org.hippoecm.frontend.i18n.types.TypeTranslator (CMS API)
    private static final String HIPPO_TYPES = "hippo:types";
    private static final String JCR_NAME = "jcr:name";

    private static final Logger log = LoggerFactory.getLogger(LocalizationUtils.class);

    private LocalizationUtils() { }

    /**
     * Retrieve the ResourceBundle applicable for a certain document type and locale.
     *
     * @param id     ID of the document type, e.g. "myhippoproject:newsdocument"
     * @param locale Locale of the related CMS session
     * @return       ResourceBundle or null
     */
    public static Optional<ResourceBundle> getResourceBundleForDocument(final String id, final Locale locale) {
        final LocalizationService localizationService = HippoServiceRegistry.getService(LocalizationService.class);
        final String bundleName = HIPPO_TYPES + "." + id;
        return Optional.ofNullable(localizationService.getResourceBundle(bundleName, locale));
    }

    /**
     * Determine the localized display name of a document type.
     *
     * @param id             ID of the document type, e.g. "myhippoproject:newsdocument"
     * @param resourceBundle Document type's resource bundle. May be null
     * @return               Display name or nothing, wrapped in an Optional
     */
    public static Optional<String> determineDocumentDisplayName(final String id,
                                                                final Optional<ResourceBundle> resourceBundle) {
        // Try to return a localised document name
        Optional<String> displayName = resourceBundle.map(rb -> rb.getString(JCR_NAME));
        if (displayName.isPresent()) {
            return displayName;
        }

        // Fall-back to node name, use part after namespace prefix
        if (id.contains(":")) {
            String unNamespaced = id.substring(id.indexOf(":") + 1);
            return Optional.of(NodeNameCodec.decode(unNamespaced));
        }
        // No display name available
        return Optional.empty();
    }

    /**
     * Determine the localized display name ("caption") of a document type field.
     *
     * @param fieldId         ID of the field, e.g. "myhippoproject:title"
     * @param resourceBundle  Document type's optional resource bundle
     * @param editorFieldNode Optional JCR node representing the field in the content editor
     * @return                Display name or nothing, wrapped in an Optional
     */
    public static Optional<String> determineFieldDisplayName(final String fieldId,
                                                             final Optional<ResourceBundle> resourceBundle,
                                                             final Optional<Node> editorFieldNode) {
        return determineFieldLabel(resourceBundle, fieldId, editorFieldNode, "caption");
    }

    /**
     * Determine the localized hint of a document type field.
     *
     * @param fieldId         ID of the field, e.g. "myhippoproject:title"
     * @param resourceBundle  Document type's optional resource bundle
     * @param editorFieldNode Optional JCR node representing the field in the content editor
     * @return                Hint or nothing, wrapped in an Optional
     */
    public static Optional<String> determineFieldHint(final String fieldId,
                                                      final Optional<ResourceBundle> resourceBundle,
                                                      final Optional<Node> editorFieldNode) {
        return determineFieldLabel(resourceBundle, fieldId + "#hint", editorFieldNode, "hint");
    }

    private static Optional<String> determineFieldLabel(final Optional<ResourceBundle> resourceBundle,
                                                        final String resourceKey,
                                                        final Optional<Node> editorFieldNode,
                                                        final String configProperty) {
        // Try to return a localized label
        Optional<String> label = resourceBundle.map(rb -> rb.getString(resourceKey));
        if (label.isPresent()) {
            return label;
        }

        // Try to read a property off the field's plugin config
        return editorFieldNode.map(node -> {
            try {
                if (node.hasProperty(configProperty)) {
                    return node.getProperty(configProperty).getString();
                }
            } catch (RepositoryException e) {
                log.warn("Failed to read property '{}'", configProperty, e);
            }
            return null;
        });
    }
}
