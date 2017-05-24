/*
 * Copyright 2014-2017 Hippo B.V. (http://www.onehippo.com)
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

import java.util.function.Supplier;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.frontend.model.JcrItemModel;
import org.hippoecm.frontend.model.properties.JcrPropertyValueModel;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JavaPluginConfig;
import org.hippoecm.frontend.plugins.standards.picker.NodePickerControllerSettings;
import org.hippoecm.frontend.session.UserSession;
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

    public static IPluginConfig fromPluginConfig(final IPluginConfig config, final JcrPropertyValueModel model) {
        return fromPluginConfig(config, () -> getCompoundNode(model));
    }

    public static IPluginConfig fromPluginConfig(final IPluginConfig config, final Supplier<Node> fieldNodeProvider) {
        String baseUuid = getPickerBaseUuid(config, fieldNodeProvider);
        if (baseUuid != null) {
            final JavaPluginConfig result = new JavaPluginConfig(config);
            result.put(NodePickerControllerSettings.BASE_UUID, baseUuid);
            return result;
        }
        return config;
    }

    private static String getPickerBaseUuid(IPluginConfig config, final Supplier<Node> fieldNodeProvider) {
        String baseUuid = null;

        if (config.containsKey(NodePickerControllerSettings.BASE_UUID)) {
            baseUuid = config.getString(NodePickerControllerSettings.BASE_UUID);
        }

        // if base.uuid is blank but base.path is set, try to find the base.uuid from the base.path config.
        if (StringUtils.isBlank(baseUuid) && config.containsKey(NodePickerControllerSettings.BASE_PATH)) {
            final String basePath = config.getString(NodePickerControllerSettings.BASE_PATH);

            if (StringUtils.isNotBlank(basePath)) {
                try {
                    final Session session = UserSession.get().getJcrSession();
                    if (session.nodeExists(basePath)) {
                        final Node baseNode = session.getNode(basePath);
                        baseUuid = baseNode.getIdentifier();
                    }
                } catch (RepositoryException e) {
                    log.warn("Failed to retrieve node identifier from the configured base.path, '{}'.", basePath, e);
                }
            }
        }

        if (StringUtils.isNotBlank(baseUuid)) {
            return baseUuid;
        } else if (isLanguageContextAware(config)) {
            try {
                return getTranslatedBaseUuid(fieldNodeProvider);
            } catch (RepositoryException e) {
                log.warn("Failed to get UUID of translated base folder, using default instead", e);
            }
        }
        return null;
    }

    private static boolean isLanguageContextAware(IPluginConfig config) {
        return config.getAsBoolean(CONFIG_LANGUAGE_CONTEXT_AWARE, DEFAULT_LANGUAGE_CONTEXT_AWARE);
    }

    private static String getTranslatedBaseUuid(final Supplier<Node> fieldNodeProvider) throws RepositoryException {
        final Node field = fieldNodeProvider.get();
        final Node document = getDocumentNodeOrNull(field);
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

    private static Node getDocumentNodeOrNull(final Node compound) throws RepositoryException {
        if (compound != null) {
            for (Node cursor = compound; cursor.getDepth() > 0; cursor = cursor.getParent()) {
                if (isDocument(cursor)) {
                    return cursor;
                }
            }
        }
        return null;
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
        if (node != null
                && node.isNodeType(HippoTranslationNodeType.NT_TRANSLATED)
                && node.hasProperty(HippoTranslationNodeType.LOCALE)) {
               return node.getProperty(HippoTranslationNodeType.LOCALE).getString();
        }
        return null;
    }

}
