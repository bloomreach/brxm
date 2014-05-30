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
package org.hippoecm.frontend.editor.plugins.linkpicker;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.frontend.model.JcrItemModel;
import org.hippoecm.frontend.model.properties.JcrPropertyValueModel;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JavaPluginConfig;
import org.hippoecm.frontend.plugins.standards.picker.NodePickerControllerSettings;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.translation.HippoTranslationNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates the default link picker dialog configuration for a hippo:mirror field with the given value model.
 *
 * The configuration property 'base.uuid' is set to the UUID of the translated ancestor folder closest to the root
 * that has the same locale as the document the compound is a part of, unless the configuration property
 * 'language.context.aware' is set to 'false' or no translated ancestor folder with the same locale exists.
 */
public class LinkPickerDialogConfig {

    public static final String CONFIG_LANGUAGE_CONTEXT_AWARE = "language.context.aware";
    public static final boolean DEFAULT_LANGUAGE_CONTEXT_AWARE = true;

    private static final Logger log = LoggerFactory.getLogger(LinkPickerDialogConfig.class);

    public static IPluginConfig fromPluginConfig(IPluginConfig config, JcrPropertyValueModel model) {
        String baseUuid = getPickerBaseUuid(config, model);
        if (baseUuid != null) {
            config = new JavaPluginConfig(config);
            config.put(NodePickerControllerSettings.BASE_UUID, baseUuid);
        }

        return config;
    }

    private static String getPickerBaseUuid(IPluginConfig config, final JcrPropertyValueModel model) {
        if (config.containsKey(NodePickerControllerSettings.BASE_UUID)) {
            return config.getString(NodePickerControllerSettings.BASE_UUID);
        } else if (isLanguageContextAware(config)) {
            try {
                return getTranslatedBaseUuid(model);
            } catch (RepositoryException e) {
                log.warn("Failed to get UUID of translated base folder, using default instead", e);
            }
        }
        return null;
    }

    private static boolean isLanguageContextAware(IPluginConfig config) {
        return config.getAsBoolean(CONFIG_LANGUAGE_CONTEXT_AWARE, DEFAULT_LANGUAGE_CONTEXT_AWARE);
    }

    private static String getTranslatedBaseUuid(final JcrPropertyValueModel model) throws RepositoryException {
        final Node compound = getCompoundNode(model);
        final Node document = getDocumentNode(compound);
        final String documentLocaleOrNull = getLocaleOrNull(document);
        if (documentLocaleOrNull != null) {
            final Node localizedRoot = getLocalizedAncestorClosestToRoot(document, documentLocaleOrNull);
            if (localizedRoot != null) {
                return localizedRoot.getIdentifier();
            }
        }
        return null;
    }

    private static Node getCompoundNode(final JcrPropertyValueModel model) {
        JcrItemModel itemModel = model.getJcrPropertymodel().getItemModel();
        final JcrItemModel nodeModel = itemModel.getParentModel();
        return (Node) nodeModel.getObject();
    }

    private static Node getDocumentNode(final Node compound) throws RepositoryException {
        Node cursor = compound;
        while (!isDocument(cursor)) {
            cursor = cursor.getParent();
        }
        return cursor;
    }

    private static boolean isDocument(final Node node) throws RepositoryException {
        return node.isNodeType(HippoNodeType.NT_DOCUMENT);
    }

    private static Node getLocalizedAncestorClosestToRoot(final Node node, final String documentLocale) throws RepositoryException {
        Node result = null;
        Node cursor = node;

        while (cursor != null && cursor.getDepth() > 0) {
            cursor = cursor.getParent();
            final String locale = getLocaleOrNull(cursor);
            if (documentLocale.equals(locale)) {
                result = cursor;
            }
        }

        return result;
    }

    private static String getLocaleOrNull(final Node node) throws RepositoryException {
        if (node.isNodeType(HippoTranslationNodeType.NT_TRANSLATED)
                && node.hasProperty(HippoTranslationNodeType.LOCALE)) {
               return node.getProperty(HippoTranslationNodeType.LOCALE).getString();
        }
        return null;
    }

}
