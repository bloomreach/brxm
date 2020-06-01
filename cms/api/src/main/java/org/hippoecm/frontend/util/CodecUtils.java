/*
 *  Copyright 2015-2017 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.util;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.service.ISettingsService;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.StringCodec;
import org.hippoecm.repository.api.StringCodecFactory;
import org.hippoecm.repository.translation.HippoTranslationNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CodecUtils {
    public static final Logger log = LoggerFactory.getLogger(CodecUtils.class);

    public static final String ENCODING_NODE    = "encoding.node";
    public static final String ENCODING_DISPLAY = "encoding.display";

    /**
     * Return a {@link StringCodecFactory} from the {@link ISettingsService} or throw an {@link IllegalStateException} 
     * if not found.
     * 
     * @param context The {@link IPluginContext} should contain a reference to the {@link ISettingsService}, otherwise
     *                an {@link IllegalStateException} is thrown.
     * @return a {@link StringCodecFactory} if found, otherwise an {@link IllegalStateException} is thrown.
     */
    public static StringCodecFactory getStringCodecFactory(final IPluginContext context) {
        ISettingsService settingsService = context.getService(ISettingsService.SERVICE_ID, ISettingsService.class);
        if (settingsService == null) {
            throw new IllegalStateException("ISettingsService not found.");
        }

        StringCodecFactory factory = settingsService.getStringCodecFactory();
        if (factory == null) {
            throw new IllegalStateException("StringCodecFactory not found.");
        }

        return factory;
    }

    /**
     * Lookup a {@link StringCodec} for encoding/decoding a display name.
     * 
     * @param context Used for retrieving the {@link StringCodecFactory}, see {@link #getStringCodecFactory}
     * @return a {@link StringCodec} for encoding/decoding a display name.
     */
    public static StringCodec getDisplayNameCodec(final IPluginContext context) {
        return getStringCodecFactory(context).getStringCodec(ENCODING_DISPLAY);
    }

    /**
     * Lookup a {@link StringCodec} for encoding/decoding a node name for a specific locale (optional).
     *
     * @param context Used for retrieving the {@link StringCodecFactory}, see {@link #getStringCodecFactory}
     * @param locale Simplified string representation of a locale like <em>en_US</em> or <em>de</em>
     * @return a {@link StringCodec} for encoding/decoding a node name.
     */
    public static StringCodec getNodeNameCodec(final IPluginContext context, final String locale) {
        return getStringCodecFactory(context).getStringCodec(ENCODING_NODE, locale);
    }

    /**
     * Lookup a {@link StringCodec} for encoding/decoding a node name. The {@link Node} is used for retrieving an 
     * optional locale.
     * 
     * @param context Used for retrieving the {@link StringCodecFactory}, see {@link #getStringCodecFactory}
     * @param node If the node or any of its parents have property {@link HippoTranslationNodeType#LOCALE} set, the 
     *             value is used to localize the {@link StringCodec}. 
     * @return a {@link StringCodec} for encoding/decoding a node name. 
     */
    public static StringCodec getNodeNameCodec(final IPluginContext context, final Node node) {
        final String locale = getLocaleFromNodeAndAncestors(node);
        return getNodeNameCodec(context, locale);
    }

    /**
     * Convenience method for wrapping a {@link StringCodec} in a {@link LoadableDetachableModel}. See 
     * {@link #getNodeNameCodec(IPluginContext, String)}
     *
     * @param context Used for retrieving the {@link StringCodecFactory}, see {@link #getStringCodecFactory}
     * @param locale Simplified string representation of a locale like <em>en_US</em> or <em>de</em>
     * @return A {@link LoadableDetachableModel} wrapping a {@link StringCodec} for encoding/decoding a node name.
     */
    public static IModel<StringCodec> getNodeNameCodecModel(final IPluginContext context, final String locale) {
        return new LoadableDetachableModel<StringCodec>() {
            @Override
            protected StringCodec load() {
                return getNodeNameCodec(context, locale);
            }
        };
    }

    /**
     * Return the value of property {@link HippoTranslationNodeType#LOCALE} for the first translated node found. If
     * the provided node is a handle, start with the first child to check whether there is a {@code HippoTranslationNodeType.LOCALE}
     * property
     * Traverse up the ancestor tree until a locale is found.
     * 
     * @param node Node to start traversal with
     * @return String representation of locale associated with subtree
     */
    public static String getLocaleFromNodeAndAncestors(Node node) {
        try {
            if (node.isNodeType(HippoNodeType.NT_HANDLE) && node.hasNode(node.getName())) {
                node = node.getNode(node.getName());
            }

            while (node != null && node.getDepth() > 0) {
                if (node.isNodeType(HippoTranslationNodeType.NT_TRANSLATED)) {
                    return node.getProperty(HippoTranslationNodeType.LOCALE).getString();
                }
                node = node.getParent();
            }
        } catch (RepositoryException e) {
            log.error("Could not determine visibility of language field");
        }

        return null;
    }

    public static String getLocaleFromNode(Node node) {
        try {
            if (node.isNodeType(HippoNodeType.NT_HANDLE) && node.hasNode(node.getName())) {
                node = node.getNode(node.getName());
            }

            if (node.isNodeType(HippoTranslationNodeType.NT_TRANSLATED)) {
                return node.getProperty(HippoTranslationNodeType.LOCALE).getString();
            }
        } catch (RepositoryException e) {
            log.error("Could not determine visibility of language field");
        }

        return null;
    }

    /**
     * Helper method for fetching a locale for a document or a folder.
     */
    public static String getLocaleFromDocumentOrFolder(final Node document, final Node folder) {
        String locale = CodecUtils.getLocaleFromNode(document);
        if (locale == null) {
            // otherwise use locale of folder
            locale = CodecUtils.getLocaleFromNodeAndAncestors(folder);
        }
        return locale;
    }
}
