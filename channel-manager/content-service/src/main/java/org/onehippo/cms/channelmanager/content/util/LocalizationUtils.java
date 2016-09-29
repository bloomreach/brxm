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

package org.onehippo.cms.channelmanager.content.util;

import java.util.Locale;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.NodeNameCodec;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.repository.l10n.LocalizationService;
import org.onehippo.repository.l10n.ResourceBundle;

/**
 * LocalizationHelper provides functionality for determining correctly localized document type and field names and hints.
 */
public class LocalizationUtils {
    // TODO: these are defined in TranslatorType in the CMS API. Move to repository and avoid duplication?
    private static final String HIPPO_TYPES = "hippo:types";
    private static final String JCR_NAME = "jcr:name";

    private LocalizationUtils() { }

    /**
     * Retrieve the ResourceBundle applicable for a certain document type and locale.
     *
     * @param id     ID of the document type, e.g. "myhippoproject:newsdocument"
     * @param locale Locale of the related CMS session
     * @return       ResourceBundle or null
     */
    public static ResourceBundle getResourceBundleForDocument(final String id, final Locale locale) {
        final LocalizationService localizationService = HippoServiceRegistry.getService(LocalizationService.class);
        final String bundleName = HIPPO_TYPES + "." + id;
        return localizationService.getResourceBundle(bundleName, locale);
    }

    /**
     * Determine the localized display name of a document type.
     *
     * @param id             ID of the document type, e.g. "myhippoproject:newsdocument"
     * @param resourceBundle Document type's resource bundle. May be null
     * @return               Display name or null
     */
    public static String determineDocumentDisplayName(final String id, final ResourceBundle resourceBundle) {
        // Try to return a localised document name
        if (resourceBundle != null) {
            String displayName = resourceBundle.getString(JCR_NAME);
            if (displayName != null) {
                return displayName;
            }
        }
        // Fall-back to node name, use part after namespace prefix
        if (id.contains(":")) {
            String unNamespaced = id.substring(id.indexOf(":") + 1);
            return NodeNameCodec.decode(unNamespaced);
        }
        // No display name available
        return null;
    }

    /**
     * Determine the localized display name ("caption") of a document type field.
     *
     * @param fieldId        ID of the field, e.g. "myhippoproject:title"
     * @param resourceBundle Document type's resource bundle. May be null
     * @param namespaceNode  Document type's root node
     * @return               Display name or null
     */
    public static String determineFieldDisplayName(final String fieldId, final ResourceBundle resourceBundle,
                                            final Node namespaceNode) {
        // Try to return a localised field name
        if (resourceBundle != null) {
            String displayName = resourceBundle.getString(fieldId);
            if (displayName != null) {
                return displayName;
            }
        }
        // Try to read the caption property of the field's plugin config
        final Node fieldConfig = NamespaceUtils.getConfigForField(namespaceNode, fieldId);
        if (fieldConfig != null) {
            try {
                return fieldConfig.getProperty("caption").getString();
            } catch (RepositoryException e) {
                // failed to read caption
            }
        }
        // No display name available
        return null;
    }

    /**
     * Determine the localized hint of a document type field.
     *
     * @param fieldId        ID of the field, e.g. "myhippoproject:title"
     * @param resourceBundle Document type's resource bundle. May be null
     * @param namespaceNode  Document type's root node
     * @return               Hint or null
     */
    public static String determineFieldHint(final String fieldId, final ResourceBundle resourceBundle,
                                     final Node namespaceNode) {
        // Try to return a localised field name
        if (resourceBundle != null) {
            String hint = resourceBundle.getString(fieldId + "#hint");
            if (hint != null) {
                return hint;
            }
        }
        // Try to read the caption property of the field's plugin config
        final Node fieldConfig = NamespaceUtils.getConfigForField(namespaceNode, fieldId);
        if (fieldConfig != null) {
            try {
                return fieldConfig.getProperty("hint").getString();
            } catch (RepositoryException e) {
                // failed to read caption
            }
        }
        // No hint available
        return null;
    }
}
