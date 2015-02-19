/*
 *  Copyright 2015 Hippo B.V. (http://www.onehippo.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.frontend.editor.icon;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.util.io.IClusterable;
import org.hippoecm.frontend.plugins.standards.icon.HippoIcon;
import org.hippoecm.frontend.service.IconSize;
import org.hippoecm.frontend.skin.Icon;
import org.hippoecm.frontend.translation.ILocaleProvider;
import org.hippoecm.frontend.translation.ILocaleProvider.HippoLocale;
import org.hippoecm.frontend.translation.ILocaleProvider.LocaleState;
import org.hippoecm.repository.translation.HippoTranslationNodeType;
import org.hippoecm.repository.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EditorTabIconProvider implements IClusterable {

    public static final Logger log = LoggerFactory.getLogger(EditorTabIconProvider.class);

    private ILocaleProvider localeProvider;

    public EditorTabIconProvider(final ILocaleProvider localeProvider) {
        this.localeProvider = localeProvider;
    }

    public Component getIcon(final IModel<Node> model, final String id, final IconSize size) {
        if (model == null) {
            return null;
        }

        final Node node = model.getObject();
        if (node == null) {
            return null;
        }

        Component icon = null;
        try {
            if (node.isNodeType("hippo:document")) {
                // document, image or asset
                if (node.isNodeType("hippogallery:imageset")) {
                    icon = getImageIcon(node, id, size);
                } else if ("hippogallery:asset".equals(node.getPrimaryNodeType().getPrimaryItemName())) {
                    icon = getAssetIcon(node, id, size);
                } else {
                    icon = getDocumentIcon(node, id, size);
                }
            } else if (node.isNodeType("hipposysedit:templatetype")) {
                // document template
                icon = getTemplateIcon(node, id, size);
            }
        } catch (RepositoryException e) {
            log.error("Error retrieving icon for node {}, message is {}", JcrUtils.getNodePathQuietly(node),
                    e.getMessage());
        }
        return icon;
    }

    private Component getAssetIcon(final Node node, final String id, final IconSize size) {
        return HippoIcon.fromSprite(id, Icon.DOCUMENT_FILE_TINY);
    }

    private Component getImageIcon(final Node node, final String id, final IconSize size) {
        return HippoIcon.fromSprite(id, Icon.DOCUMENT_IMAGE_TINY);
    }

    private Component getTemplateIcon(final Node node, final String id, final IconSize size) throws RepositoryException {
        if (isCompoundTemplate(node)) {
            return HippoIcon.fromSprite(id, Icon.DOCUMENT_COMPOUND_TINY);
        } else {
            return HippoIcon.fromSprite(id, Icon.DOCUMENT_FILE_TINY);
        }
    }

    private Component getDocumentIcon(final Node node, final String id, final IconSize size) throws RepositoryException {
        if (localeProvider != null) {
            if (node.isNodeType(HippoTranslationNodeType.NT_TRANSLATED)) {
                String localeName = node.getProperty(HippoTranslationNodeType.LOCALE).getString();
                for (HippoLocale locale : localeProvider.getLocales()) {
                    if (localeName.equals(locale.getName())) {
                        ResourceReference reference = locale.getIcon(size, LocaleState.EXISTS);
                        return HippoIcon.fromResource(id, reference);
                    }
                }
                log.info("Locale '" + localeName + "' was not found in provider");
            }
        }
        return HippoIcon.fromSprite(id, Icon.DOCUMENT_TINY);
    }

    private boolean isCompoundTemplate(final Node templateType) throws RepositoryException {
        if (templateType.hasNode("hipposysedit:nodetype")) {
            Node nodeType = templateType.getNode("hipposysedit:nodetype");
            if (nodeType.hasNode("hipposysedit:nodetype")) {
                nodeType = nodeType.getNode("hipposysedit:nodetype");
                final Property superTypeProperty = nodeType.getProperty("hipposysedit:supertype");
                if (superTypeProperty.isMultiple()) {
                    for (Value value : superTypeProperty.getValues()) {
                        if ("hippo:compound".equals(value.getString())) {
                            return true;
                        }
                    }
                } else {
                    return "hippo:compound".equals(superTypeProperty.getString());
                }
            }
        }
        return false;
    }

}
